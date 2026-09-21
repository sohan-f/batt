package com.sysui.batt.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * LSPosed module entry (see `META-INF/xposed/java_init.list`).
 */
class ModernInitHook : XposedModule() {

    private val hookEntry = HookEntry()

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        try {
            Log.i(TAG, "LSPosed API 102 module loaded: ${param.processName}")
        } catch (_: Throwable) {
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        try {
            Log.i(TAG, "Package loaded: ${param.packageName} (first=${param.isFirstPackage})")
        } catch (_: Throwable) {
        }
        // Hooking is done in onPackageReady where the app ClassLoader is final.
    }

    override fun onPackageReady(param: io.github.libxposed.api.XposedModuleInterface.PackageReadyParam) {
        super.onPackageReady(param)
        try {
            val packageName = param.packageName
            // Only SystemUI (and self for hook-check) is in scope.
            if (packageName != SYSTEMUI && packageName != SELF) {
                return
            }
            try {
                hookEntry.handleLoadPackage(this, packageName, param.classLoader)
            } catch (t: Throwable) {
                Log.e(TAG, "onPackageReady failed for $packageName", t)
            }
        } catch (t: Throwable) {
            try {
                Log.e(TAG, "onPackageReady failed", t)
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        private const val TAG = "CircleBattery"
        private const val SYSTEMUI = "com.android.systemui"
        private const val SELF = "com.sysui.batt"
    }
}
