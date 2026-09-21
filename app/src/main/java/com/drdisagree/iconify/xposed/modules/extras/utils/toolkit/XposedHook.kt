package com.drdisagree.iconify.xposed.modules.extras.utils.toolkit

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import com.drdisagree.iconify.xposed.modules.extras.utils.ViewHelper.toPx
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.WeakHashMap
import java.util.regex.Pattern

/**
 * Hooking toolkit built on LSPosed API 102 ([XposedModule.hook] interceptor model).
 */
class XposedHook {
    companion object {
        private var moduleRef: WeakReference<XposedModule>? = null
        private val classLoaders = mutableMapOf<String, ClassLoader>()

        val module: XposedModule
            get() = moduleRef?.get()
                ?: throw IllegalStateException("XposedHook.init() must be called first")

        fun init(module: XposedModule, packageName: String, classLoader: ClassLoader) {
            if (moduleRef?.get() !== module) {
                moduleRef = WeakReference(module)
            }
            classLoaders[packageName] = classLoader
        }

        fun init(module: XposedModule, classLoader: ClassLoader) {
            if (moduleRef?.get() !== module) {
                moduleRef = WeakReference(module)
            }
            classLoaders["*"] = classLoader
        }

        private fun classLoaderFor(): ClassLoader {
            classLoaders.values.lastOrNull()?.let { return it }
            throw IllegalStateException("XposedHook.init() must be called before findClass()")
        }

        fun findClass(
            vararg classNames: String,
            classLoader: ClassLoader? = null,
            suppressError: Boolean = false,
            throwException: Boolean = false
        ): Class<*>? {
            val loader = classLoader ?: try {
                classLoaderFor()
            } catch (t: Throwable) {
                if (throwException) throw t
                if (!suppressError) log(XposedHook, t)
                return null
            }

            for (className in classNames) {
                try {
                    return Class.forName(className, false, loader)
                } catch (_: Throwable) {
                }
            }

            if (throwException) {
                throw Throwable(
                    if (classNames.size == 1) "Class not found: ${classNames[0]}"
                    else "None of the classes were found: ${classNames.joinToString()}"
                )
            } else if (!suppressError) {
                if (classNames.size == 1) {
                    log(XposedHook, "Class not found: ${classNames[0]}")
                } else {
                    log(XposedHook, "None of the classes were found: ${classNames.joinToString()}")
                }
            }

            return null
        }
    }
}

/** Parameters for before/after/replace callbacks. */
class HookParam(
    val method: Executable?,
    val thisObject: Any?,
    args: Array<Any?>,
    initialResult: Any? = null
) {
    var args: Array<Any?> = args
        set(value) {
            field = value
            argsModified = true
        }
    var argsModified = false
        private set

    var result: Any? = initialResult
        set(value) {
            field = value
            resultSet = true
        }
    var resultSet = false
        private set

    /** Skip the original method and return [result] instead. */
    fun returnEarly(result: Any? = this.result) {
        this.result = result
    }
}

/** Combined before/after hook. */
abstract class MethodHook {
    open fun beforeHookedMethod(param: HookParam) {}
    open fun afterHookedMethod(param: HookParam) {}
}

fun Class<*>?.hookMethod(vararg methodNames: String): MethodHookHelper {
    return MethodHookHelper(this, methodNames)
}

fun Class<*>?.hookConstructor(): MethodHookHelper {
    return MethodHookHelper(this)
}

fun Class<*>?.hookMethodMatchPattern(methodNamePattern: String): MethodHookHelper {
    return MethodHookHelper(this, arrayOf(methodNamePattern), true)
}

class MethodHookHelper(
    private val clazz: Class<*>?,
    private val methodNames: Array<out String>? = null,
    private val isPattern: Boolean = false,
    private val method: Method? = null
) {
    constructor(
        clazz: Class<*>?,
        methodNames: Array<out String>? = null,
        isPattern: Boolean = false
    ) : this(clazz, methodNames, isPattern, null)

    constructor(method: Method) : this(null, null, false, method)

    private var parameterTypes: Array<Any?>? = null
    private var printError: Boolean = true
    private var throwError: Boolean = false

    @Suppress("UNCHECKED_CAST")
    fun parameters(vararg parameterTypes: Any?): MethodHookHelper {
        this.parameterTypes = parameterTypes as Array<Any?>?
        return this
    }

    fun run(hook: MethodHook): MethodHookHelper {
        eachTarget { executable ->
            try {
                XposedHook.module.hook(executable).intercept(object : XposedInterface.Hooker {
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        val beforeParam = HookParam(executable, chain.thisObject, chain.args.toTypedArray())
                        try {
                            hook.beforeHookedMethod(beforeParam)
                        } catch (t: Throwable) {
                            log(XposedHook, t)
                        }
                        val original: Any? = if (beforeParam.resultSet) {
                            beforeParam.result
                        } else {
                            try {
                                if (beforeParam.argsModified) chain.proceed(beforeParam.args) else chain.proceed()
                            } catch (t: Throwable) {
                                throw t
                            }
                        }
                        val afterParam = HookParam(executable, chain.thisObject, beforeParam.args, original)
                        if (beforeParam.resultSet) afterParam.result = beforeParam.result
                        try {
                            hook.afterHookedMethod(afterParam)
                        } catch (t: Throwable) {
                            log(XposedHook, t)
                        }
                        return afterParam.result
                    }
                })
            } catch (t: Throwable) {
                handleHookError(t, executable.toString())
            }
        }
        return this
    }

    fun runBefore(callback: (HookParam) -> Unit): MethodHookHelper {
        eachTarget { executable ->
            try {
                XposedHook.module.hook(executable).intercept(object : XposedInterface.Hooker {
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        val param = HookParam(executable, chain.thisObject, chain.args.toTypedArray())
                        try {
                            callback(param)
                        } catch (t: Throwable) {
                            log(XposedHook, t)
                        }
                        return if (param.resultSet) {
                            param.result
                        } else {
                            if (param.argsModified) chain.proceed(param.args) else chain.proceed()
                        }
                    }
                })
            } catch (t: Throwable) {
                handleHookError(t, executable.toString())
            }
        }
        return this
    }

    fun runAfter(callback: (HookParam) -> Unit): MethodHookHelper {
        eachTarget { executable ->
            try {
                XposedHook.module.hook(executable).intercept(object : XposedInterface.Hooker {
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        val original = chain.proceed()
                        val param = HookParam(executable, chain.thisObject, chain.args.toTypedArray(), original)
                        param.resultSetReset()
                        param.result = original
                        param.resultSetReset()
                        try {
                            callback(param)
                        } catch (t: Throwable) {
                            log(XposedHook, t)
                        }
                        return if (param.resultSet) param.result else original
                    }
                })
            } catch (t: Throwable) {
                handleHookError(t, executable.toString())
            }
        }
        return this
    }

    fun replace(callback: (HookParam) -> Unit): MethodHookHelper {
        eachTarget { executable ->
            try {
                XposedHook.module.hook(executable).intercept(object : XposedInterface.Hooker {
                    override fun intercept(chain: XposedInterface.Chain): Any? {
                        val param = HookParam(executable, chain.thisObject, chain.args.toTypedArray())
                        try {
                            callback(param)
                        } catch (t: Throwable) {
                            log(XposedHook, t)
                        }
                        return if (param.resultSet) param.result else null
                    }
                })
            } catch (t: Throwable) {
                handleHookError(t, executable.toString())
            }
        }
        return this
    }

    private fun handleHookError(t: Throwable, target: String) {
        if (throwError) throw t
        if (printError) log(XposedHook, "Hook failed ($target): $t")
    }

    private fun eachTarget(action: (Executable) -> Unit) {
        if (method != null) {
            action(method)
            return
        }
        if (clazz == null) return

        if (methodNames.isNullOrEmpty()) {
            val ctors = clazz.declaredConstructors.toList()
            if (parameterTypes.isNullOrEmpty()) {
                ctors.forEach(action)
            } else {
                ctors.filter { matchesParams(it.parameterTypes) }.forEach(action)
            }
            if (ctors.isEmpty() && printError) log(XposedHook, "No constructors in ${clazz.name}")
            return
        }

        var foundAny = false
        val allMethods = clazz.declaredMethods.toList().union(clazz.methods.toList()).toList()
        methodNames.forEach { name ->
            if (isPattern) {
                val pattern = Pattern.compile(name)
                allMethods.filter { pattern.matcher(it.name).matches() }
                    .filter { parameterTypes.isNullOrEmpty() || matchesParams(it.parameterTypes) }
                    .forEach {
                        action(it)
                        foundAny = true
                    }
            } else {
                allMethods.filter { it.name == name }
                    .filter { parameterTypes.isNullOrEmpty() || matchesParams(it.parameterTypes) }
                    .forEach {
                        action(it)
                        foundAny = true
                    }
            }
        }

        if (!foundAny && printError && clazz != null) {
            log(XposedHook, "Method(s) not found: ${methodNames.joinToString()} in ${clazz.simpleName}")
        } else if (!foundAny && throwError) {
            throw Throwable("Method(s) not found: ${methodNames.joinToString()} in ${clazz?.simpleName}")
        }
    }

    private fun matchesParams(actual: Array<Class<*>>): Boolean {
        val expected = parameterTypes ?: return true
        if (actual.size != expected.size) return false
        expected.forEachIndexed { i, e ->
            val exp = e as? Class<*> ?: return false
            if (actual[i] != exp && !actual[i].isAssignableFrom(exp) && !exp.isAssignableFrom(actual[i])) {
                if (!primitiveMatch(actual[i], exp)) return false
            }
        }
        return true
    }

    private fun primitiveMatch(a: Class<*>, b: Class<*>): Boolean {
        if (a == b) return true
        val map = mapOf(
            java.lang.Boolean.TYPE to java.lang.Boolean::class.java,
            java.lang.Byte.TYPE to java.lang.Byte::class.java,
            java.lang.Character.TYPE to java.lang.Character::class.java,
            java.lang.Short.TYPE to java.lang.Short::class.java,
            java.lang.Integer.TYPE to java.lang.Integer::class.java,
            java.lang.Long.TYPE to java.lang.Long::class.java,
            java.lang.Float.TYPE to java.lang.Float::class.java,
            java.lang.Double.TYPE to java.lang.Double::class.java,
            java.lang.Void.TYPE to Void::class.java
        )
        return map[a] == b || map[b] == a
    }

    fun suppressError(): MethodHookHelper {
        printError = false
        return this
    }

    fun throwError(): MethodHookHelper {
        suppressError()
        throwError = true
        return this
    }
}

private fun HookParam.resultSetReset() {
    val field = HookParam::class.java.getDeclaredField("resultSet")
    field.isAccessible = true
    (field.get(this) as? Boolean)
    field.setBoolean(this, false)
}

fun Method.run(hook: MethodHook): MethodHookHelper {
    return MethodHookHelper(this).run(hook)
}

fun Method.runBefore(callback: (HookParam) -> Unit): MethodHookHelper {
    return MethodHookHelper(this).runBefore(callback)
}

fun Method.runAfter(callback: (HookParam) -> Unit): MethodHookHelper {
    return MethodHookHelper(this).runAfter(callback)
}

fun Method.replace(callback: (HookParam) -> Unit): MethodHookHelper {
    return MethodHookHelper(this).replace(callback)
}

object ResourceHookManager {

    private val hookedResources = mutableListOf<HookData>()
    private var contextRef: WeakReference<Context>? = null
    private var hooksApplied = false

    fun init(context: Context) {
        contextRef = WeakReference(context)
        applyHooks()
    }

    fun hookDimen(): HookBuilder {
        return HookBuilder(HookType.DIMENSION)
    }

    fun hookBoolean(): HookBuilder {
        return HookBuilder(HookType.BOOLEAN)
    }

    fun hookInteger(): HookBuilder {
        return HookBuilder(HookType.INTEGER)
    }

    @Synchronized
    private fun applyHooks() {
        if (hooksApplied) return
        hooksApplied = true
        val context = try {
            contextRef?.get() ?: return
        } catch (_: Throwable) {
            return
        }

        HookType.entries.forEach { hookType ->
            hookType.methods.forEach { method ->
                try {
                    Resources::class.java
                        .hookMethod(method)
                        .suppressError()
                        .runBefore { param ->
                            val resId = param.args.getOrNull(0) as? Int ?: return@runBefore
                            val hookData = synchronized(hookedResources) {
                                hookedResources.find {
                                    it.method == method && it.resId == resId && it.condition.invoke()
                                }
                            } ?: return@runBefore

                            if (method == "getDimensionPixelSize") {
                                param.result = context.toPx(hookData.value.invoke() as Int)
                            } else {
                                param.result = hookData.value.invoke()
                            }
                        }
                } catch (_: Throwable) {
                }
            }
        }
    }

    class HookBuilder(private val hookType: HookType) {

        private var packageName: String? = null
        private var condition: () -> Boolean = { true }
        private val resourcesToHook = mutableListOf<HookData>()

        fun whenCondition(condition: () -> Boolean): HookBuilder {
            this.condition = condition
            return this
        }

        fun forPackageName(packageName: String): HookBuilder {
            this.packageName = packageName
            return this
        }

        @SuppressLint("DiscouragedApi")
        fun addResource(name: String, value: () -> Any): HookBuilder {
            val context = contextRef?.get() ?: return this
            if (packageName == null) throw IllegalArgumentException("packageName must be set")

            val resId = context.resources.getIdentifier(
                name,
                hookType.resourceType,
                packageName
            )

            if (resId != 0) {
                hookType.methods.forEach { method ->
                    resourcesToHook.add(HookData(resId, method, value, condition))
                }
            }

            return this
        }

        fun apply() {
            synchronized(hookedResources) {
                resourcesToHook.forEach { resource ->
                    if (!hookedResources.contains(resource)) {
                        hookedResources.add(resource)
                    }
                }
            }
        }
    }

    data class HookData(
        val resId: Int,
        val method: String,
        val value: () -> Any,
        val condition: () -> Boolean
    )

    enum class HookType(val resourceType: String, val methods: List<String>) {
        BOOLEAN(
            "bool",
            listOf("getBoolean")
        ),
        INTEGER(
            "integer",
            listOf("getInteger")
        ),
        DIMENSION(
            "dimen",
            listOf("getDimension", "getDimensionPixelOffset", "getDimensionPixelSize")
        )
    }
}

// Reflection helpers

private fun findFieldRecursive(clazz: Class<*>?, name: String): java.lang.reflect.Field? {
    var c = clazz
    while (c != null && c != Any::class.java) {
        try {
            val f = c.getDeclaredField(name)
            f.isAccessible = true
            return f
        } catch (_: Throwable) {
            c = c.superclass
        }
    }
    return null
}

private fun findMethodRecursive(obj: Any?, name: String, argCount: Int): Method? {
    val clazz = (obj as? Class<*>) ?: obj?.javaClass ?: return null
    var c: Class<*>? = clazz
    while (c != null && c != Any::class.java) {
        c.declaredMethods.filter { it.name == name && it.parameterCount == argCount }.forEach {
            it.isAccessible = true
            return it
        }
        c.methods.filter { it.name == name && it.parameterCount == argCount }.forEach {
            it.isAccessible = true
            return it
        }
        c = c.superclass
    }
    return null
}

fun Any?.callMethod(methodName: String): Any? {
    if (this == null) return null
    val m = findMethodRecursive(this, methodName, 0) ?: return null
    return try {
        if (this is Class<*>) m.invoke(null) else m.invoke(this)
    } catch (t: Throwable) {
        throw t.cause ?: t
    }
}

fun Any?.callMethod(methodName: String, vararg args: Any?): Any? {
    if (this == null) return null
    val m = findMethodRecursive(this, methodName, args.size)
        ?: throw NoSuchMethodException("$methodName(${args.size}) in ${this.javaClass.name}")
    return try {
        if (this is Class<*>) m.invoke(null, *args) else m.invoke(this, *args)
    } catch (t: Throwable) {
        throw t.cause ?: t
    }
}

fun Any?.callMethodSilently(methodName: String): Any? {
    return try {
        callMethod(methodName)
    } catch (_: Throwable) {
        null
    }
}

fun Any?.callMethodSilently(methodName: String, vararg args: Any?): Any? {
    return try {
        callMethod(methodName, *args)
    } catch (_: Throwable) {
        null
    }
}

fun Class<*>?.callStaticMethod(methodName: String): Any? {
    if (this == null) return null
    return this.callMethod(methodName)
}

fun Class<*>?.callStaticMethod(methodName: String, vararg args: Any?): Any? {
    if (this == null) return null
    return this.callMethod(methodName, *args)
}

fun Class<*>?.callStaticMethodSilently(methodName: String): Any? {
    return try {
        callStaticMethod(methodName)
    } catch (_: Throwable) {
        null
    }
}

fun Class<*>?.callStaticMethodSilently(methodName: String, vararg args: Any?): Any? {
    return try {
        callStaticMethod(methodName, *args)
    } catch (_: Throwable) {
        null
    }
}

fun Any?.getField(fieldName: String): Any {
    if (this == null) throw NoSuchFieldError("Field not found: $fieldName, object is null")
    val clazz = if (this is Class<*>) this else this.javaClass
    val f = findFieldRecursive(clazz, fieldName)
        ?: throw NoSuchFieldError("Field not found: $fieldName in ${clazz.name}")
    return try {
        if (this is Class<*>) f.get(null)!! else f.get(this)!!
    } catch (t: Throwable) {
        throw t.cause ?: t
    }
}

fun Any?.getFieldSilently(fieldName: String): Any? {
    return try {
        getField(fieldName)
    } catch (_: Throwable) {
        null
    }
}

fun Any?.setField(fieldName: String, value: Any?) {
    if (this == null) throw NoSuchFieldError("Field not found: $fieldName, object is null")
    val clazz = if (this is Class<*>) this else this.javaClass
    val f = findFieldRecursive(clazz, fieldName)
        ?: throw NoSuchFieldError("Field not found: $fieldName in ${clazz.name}")
    f.set(if (this is Class<*>) null else this, value)
}

fun Any?.setFieldSilently(fieldName: String, value: Any?) {
    try {
        setField(fieldName, value)
    } catch (_: Throwable) {
    }
}

fun Class<*>?.getStaticField(fieldName: String): Any {
    if (this == null) throw NoSuchFieldError("Field not found: $fieldName, class is null")
    return (this as Any).getField(fieldName)
}

fun Class<*>?.getStaticFieldSilently(fieldName: String): Any? {
    return try {
        getStaticField(fieldName)
    } catch (_: Throwable) {
        null
    }
}

fun Class<*>?.setStaticField(fieldName: String, value: Any?) {
    (this as Any?)?.setField(fieldName, value)
}

fun Class<*>?.setStaticFieldSilently(fieldName: String, value: Any?) {
    try {
        setStaticField(fieldName, value)
    } catch (_: Throwable) {
    }
}

fun Any?.setStaticField(fieldName: String, value: Any?) {
    if (this == null) return
    val clazz = if (this is Class<*>) this else this.javaClass
    try {
        val f = findFieldRecursive(clazz, fieldName) ?: return
        if (!Modifier.isStatic(f.modifiers)) return
        f.set(null, value)
    } catch (_: Throwable) {
    }
}

fun Any?.getBooleanField(fieldName: String): Boolean {
    return (getFieldSilently(fieldName) as? Boolean) == true
}

fun Any?.setStaticIntField(fieldName: String, value: Int) {
    setStaticField(fieldName, value)
}

fun Class<*>?.setStaticIntField(fieldName: String, value: Int) {
    (this as Any?)?.setStaticField(fieldName, value)
}

fun Any?.getAnyField(vararg fieldNames: String): Any? {
    fieldNames.forEach { fieldName ->
        try {
            return getField(fieldName)
        } catch (_: Throwable) {
        }
    }
    throw NoSuchFieldError("Field not found: ${fieldNames.joinToString()}")
}

fun Any?.setAnyField(value: Any?, vararg fieldNames: String) {
    fieldNames.forEach { fieldName ->
        try {
            return setField(fieldName, value)
        } catch (_: Throwable) {
        }
    }
    throw NoSuchFieldError("Field not found: ${fieldNames.joinToString()}")
}

fun Class<*>?.getAnyStaticField(vararg fieldNames: String): Any? {
    fieldNames.forEach { fieldName ->
        try {
            return getStaticField(fieldName)
        } catch (_: Throwable) {
        }
    }
    throw NoSuchFieldError("Field not found: ${fieldNames.joinToString()}")
}

// Additional instance fields.
private val extraFields = WeakHashMap<Any, MutableMap<String, Any?>>()
private val extraFieldsLock = Any()

fun Any?.getExtraField(fieldName: String): Any {
    if (this == null) throw NoSuchFieldError("Extra field not found: $fieldName")
    synchronized(extraFieldsLock) {
        return extraFields[this]?.get(fieldName)
            ?: throw NoSuchFieldError("Extra field not found: $fieldName")
    }
}

fun Any?.getExtraFieldSilently(fieldName: String): Any? {
    return try {
        getExtraField(fieldName)
    } catch (_: Throwable) {
        null
    }
}

fun Any?.setExtraField(fieldName: String, value: Any?) {
    if (this == null) return
    synchronized(extraFieldsLock) {
        extraFields.getOrPut(this) { mutableMapOf() }[fieldName] = value
    }
}
