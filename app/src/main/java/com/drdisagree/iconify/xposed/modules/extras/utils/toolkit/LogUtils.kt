package com.drdisagree.iconify.xposed.modules.extras.utils.toolkit

import android.util.Log
import android.view.View
import android.view.ViewGroup

private const val TAG = "CircleBattery"

fun log(message: String?) {
    try {
        Log.i(TAG, message ?: "null")
    } catch (_: Throwable) {
    }
}

fun log(message: Any?) {
    log(message?.toString())
}

fun log(tag: String, message: Any?) {
    try {
        Log.i(TAG, "Iconify - $tag: $message")
    } catch (_: Throwable) {
    }
}

fun <T : Any> log(clazz: T, message: Any?) {
    try {
        Log.i(
            TAG,
            "Iconify - ${clazz.javaClass.simpleName.replace("\$Companion", "")}: $message"
        )
    } catch (_: Throwable) {
    }
}

fun <T : Any> log(clazz: T, throwable: Throwable?) {
    try {
        Log.e(
            TAG,
            "Iconify - ${clazz.javaClass.simpleName.replace("\$Companion", "")}: $throwable",
            throwable
        )
    } catch (_: Throwable) {
    }
}

fun <T : Any> log(clazz: T, exception: Exception?) {
    log(clazz, exception as Throwable?)
}

fun findAndDumpClass(className: String, classLoader: ClassLoader?): Class<*> {
    dumpClass(className, classLoader)
    return Class.forName(className, false, classLoader)
}

fun findAndDumpClassIfExists(className: String, classLoader: ClassLoader?): Class<*>? {
    dumpClass(className, classLoader)
    return try {
        Class.forName(className, false, classLoader)
    } catch (_: Throwable) {
        null
    }
}

private fun dumpClass(className: String, classLoader: ClassLoader?) {
    val ourClass = try {
        Class.forName(className, false, classLoader)
    } catch (_: Throwable) {
        null
    }
    if (ourClass == null) {
        log("DumpClass: Class is null")
        return
    }
    ourClass.dumpClass()
}

fun Class<*>?.dumpClass() {
    if (this == null) {
        log("DumpClass: Class is null")
        return
    }

    log("\n\nClass: $name")
    log("extends: ${superclass?.name}")
    log("Subclasses:")
    val scs = classes.toList().union(declaredClasses.toList())
    for (c in scs) {
        log("\t" + c.name)
    }
    if (scs.isEmpty()) {
        log("\tNone")
    }

    log("Constructors:")
    val cons = declaredConstructors
    for (m in cons) {
        log("\t" + m.name + " - " + this::class.java.simpleName + " - " + m.parameterCount)
        val cs = m.parameterTypes
        for (c in cs) {
            log("\t\t" + c.typeName)
        }
    }
    if (cons.isEmpty()) {
        log("\tNone")
    }

    log("Methods:")
    val ms = declaredMethods.toList().union(methods.toList())
    for (m in ms) {
        log("\t" + m.name + " - " + m.returnType + " - " + m.parameterCount)
        val cs = m.parameterTypes
        for (c in cs) {
            log("\t\t" + c.typeName)
        }
    }
    if (ms.isEmpty()) {
        log("\tNone")
    }

    log("Fields:")
    val fs = declaredFields
    for (f in fs) {
        log("\t" + f.name + " - " + f.type.name)
    }
    if (fs.isEmpty()) {
        log("\tNone")
    }
    log("End dump\n\n")
}

fun View.dumpChildViews() {
    if (this is ViewGroup) {
        logViewInfo(this, 0)
        dumpChildViewsRecursive(this, 0)
    } else {
        logViewInfo(this, 0)
    }
}

private fun dumpChildViewsRecursive(
    viewGroup: ViewGroup,
    indentationLevel: Int
) {
    for (i in 0 until viewGroup.childCount) {
        val childView = viewGroup.getChildAt(i)
        logViewInfo(childView, indentationLevel + 1)
        if (childView is ViewGroup) {
            dumpChildViewsRecursive(childView, indentationLevel + 1)
        }
    }
}

private fun logViewInfo(view: View, indentationLevel: Int) {
    val indentation = repeatString("\t", indentationLevel)
    val viewName = view.javaClass.simpleName
    val superclassName = view.javaClass.superclass?.simpleName ?: "None"
    val backgroundDrawable = view.background
    val childCount = if (view is ViewGroup) view.childCount else 0
    var resourceIdName = "none"
    try {
        val viewId = view.id
        resourceIdName = view.context.resources.getResourceName(viewId)
    } catch (ignored: Throwable) {
    }
    var logMessage = "$indentation$viewName (Extends: $superclassName) - ID: $resourceIdName"
    if (childCount > 0) {
        logMessage += " - ChildCount: $childCount"
    }
    if (backgroundDrawable != null) {
        logMessage += " - Background: ${backgroundDrawable.javaClass.simpleName}"
    }
    log(logMessage)
}

@Suppress("SameParameterValue")
private fun repeatString(str: String, times: Int): String {
    val result = StringBuilder()
    for (i in 0 until times) {
        result.append(str)
    }
    return result.toString()
}

fun Any.dumpPreferenceKeys() {
    for (i in 0 until callMethod("getPreferenceCount") as Int) {
        val preference = callMethod("getPreference", i)!!

        log("${preference::class.java.simpleName} -> Key: ${preference.callMethod("getKey")}")

        if (preference::class.java.simpleName == "PreferenceCategory") {
            preference.dumpPreferenceKeys()
        }
    }
}
