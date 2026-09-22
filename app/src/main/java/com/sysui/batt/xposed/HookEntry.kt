package com.sysui.batt.xposed

import android.app.Instrumentation
import android.content.Context
import com.sysui.batt.BuildConfig
import com.sysui.batt.data.common.Const.FRAMEWORK_PACKAGE
import com.sysui.batt.xposed.modules.extras.utils.toolkit.ResourceHookManager
import com.sysui.batt.xposed.modules.extras.utils.toolkit.XposedHook
import com.sysui.batt.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.sysui.batt.xposed.modules.extras.utils.toolkit.hookMethod
import com.sysui.batt.xposed.modules.extras.utils.toolkit.log
import com.sysui.batt.xposed.utils.BootLoopProtector
import com.sysui.batt.xposed.utils.SystemUtils
import com.sysui.batt.xposed.utils.XPrefs
import com.sysui.batt.xposed.utils.XPrefs.Xprefs
import com.sysui.batt.xposed.utils.XPrefs.XprefsIsInitialized
import io.github.libxposed.api.XposedModule
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture

class HookEntry {

    private lateinit var mContext: Context
    private var hostPackage: String = ""
    private var hostClassLoader: ClassLoader? = null

    fun handleLoadPackage(module: XposedModule, packageName: String, classLoader: ClassLoader) {
        synchronized(handledPackages) {
            if (!handledPackages.add(packageName)) return
        }
        XposedHook.init(module, packageName, classLoader)
        hostPackage = packageName
        hostClassLoader = classLoader

        isChildProcess = try {
            val processName = try {
                Class.forName("android.app.ActivityThread")
                    .getMethod("currentProcessName")
                    .invoke(null) as? String
            } catch (_: Throwable) {
                null
            }
            processName?.contains(":") == true
        } catch (ignored: Throwable) {
            false
        }

        when (packageName) {
            FRAMEWORK_PACKAGE -> {
                val phoneWindowManagerClass =
                    findClass("com.android.server.policy.PhoneWindowManager")

                phoneWindowManagerClass
                    .hookMethod("init")
                    .runBefore { param ->
                        try {
                            if (!::mContext.isInitialized) {
                                mContext = param.args[0] as Context

                                HookRes.modRes = mContext.createPackageContext(
                                    BuildConfig.APPLICATION_ID,
                                    Context.CONTEXT_IGNORE_SECURITY
                                ).resources

                                XPrefs.init(mContext)
                                ResourceHookManager.init(mContext)

                                CompletableFuture.runAsync { waitForXprefsLoad(packageName) }
                            }
                        } catch (throwable: Throwable) {
                            log(this@HookEntry, throwable)
                        }
                    }
            }

            else -> {
                if (!isChildProcess) {
                    Instrumentation::class.java
                        .hookMethod("newApplication")
                        .parameters(
                            ClassLoader::class.java,
                            String::class.java,
                            Context::class.java
                        )
                        .runAfter { param ->
                            try {
                                if (!::mContext.isInitialized) {
                                    mContext = param.args[2] as Context

                                    HookRes.modRes = mContext.createPackageContext(
                                        BuildConfig.APPLICATION_ID,
                                        Context.CONTEXT_IGNORE_SECURITY
                                    ).resources

                                    XPrefs.init(mContext)
                                    ResourceHookManager.init(mContext)

                                    CompletableFuture.runAsync { waitForXprefsLoad(packageName) }
                                }
                            } catch (throwable: Throwable) {
                                log(this@HookEntry, throwable)
                            }
                        }
                }
            }
        }
    }

    private fun onXPrefsReady(packageName: String) {
        if (!isChildProcess && BootLoopProtector.isBootLooped(packageName)) {
            log("Possible crash in $packageName ; Batt will not load for now...")
            return
        }

        loadModPacks(packageName)
    }

    private fun loadModPacks(packageName: String) {
        val loader = hostClassLoader

        for (mod in EntryList.getEntries(packageName)) {
            try {
                val modInstance = mod.getConstructor(Context::class.java).newInstance(mContext)

                if (XprefsIsInitialized) {
                    try {
                        modInstance.updatePrefs()
                    } catch (throwable: Throwable) {
                        log(this@HookEntry, "Failed to update prefs in ${mod.name}")
                        log(this@HookEntry, throwable)
                    }
                }

                hostClassLoader?.let {
                    try {
                        XposedHook.init(
                            XposedHook.module,
                            packageName,
                            it
                        )
                    } catch (_: Throwable) {
                    }
                }
                modInstance.handleLoadPackage(packageName, loader ?: mContext.classLoader)
                runningMods.add(modInstance)
            } catch (invocationTargetException: InvocationTargetException) {
                log(this@HookEntry, "Start Error Dump - Occurred in ${mod.name}")
                log(this@HookEntry, invocationTargetException.cause)
            } catch (throwable: Throwable) {
                log(this@HookEntry, "Start Error Dump - Occurred in ${mod.name}")
                log(this@HookEntry, throwable)
            }
        }
    }

    private fun waitForXprefsLoad(packageName: String) {
        while (true) {
            try {
                Xprefs.getBoolean("LoadTestBooleanValue", false)
                break
            } catch (ignored: Throwable) {
                SystemUtils.sleep(1000);
            }
        }

        log("Batt Version: ${BuildConfig.VERSION_NAME}")
        log("Hooked $packageName")

        onXPrefsReady(packageName)
    }

    companion object {
        val runningMods = ArrayList<ModPack>()
        var isChildProcess = false
        private val handledPackages = mutableSetOf<String>()
    }
}
