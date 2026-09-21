package com.drdisagree.iconify.xposed.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.drdisagree.iconify.BuildConfig
import com.drdisagree.iconify.data.common.Const.ACTION_HOOK_CHECK_REQUEST
import com.drdisagree.iconify.data.common.Const.ACTION_HOOK_CHECK_RESULT
import com.drdisagree.iconify.data.common.Const.SYSTEMUI_PACKAGE
import com.drdisagree.iconify.xposed.ModPack
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class HookCheck(context: Context) : ModPack(context) {

    private var intentFilter = IntentFilter()
    private var broadcastRegistered = false

    override fun updatePrefs(vararg key: String) {}

    private fun returnBroadcastResult() {
        Thread {
            mContext.sendBroadcast(
                Intent()
                    .setAction(ACTION_HOOK_CHECK_RESULT)
                    .setPackage(BuildConfig.APPLICATION_ID)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            )
        }.start()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        if (loadPackageParam.packageName == BuildConfig.APPLICATION_ID) {
            try {
                XposedHelpers.findAndHookMethod(
                    HookCheck::class.java.name,
                    loadPackageParam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
            } catch (_: Throwable) {
            }
        }

        if (!broadcastRegistered && loadPackageParam.packageName == SYSTEMUI_PACKAGE) {
            broadcastRegistered = true

            intentFilter.addAction(ACTION_HOOK_CHECK_REQUEST)

            val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == ACTION_HOOK_CHECK_REQUEST &&
                        loadPackageParam.packageName == SYSTEMUI_PACKAGE
                    ) {
                        returnBroadcastResult()
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mContext.registerReceiver(
                    broadcastReceiver,
                    intentFilter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                mContext.registerReceiver(broadcastReceiver, intentFilter)
            }
        }
    }

    companion object {
        @JvmStatic
        fun isModuleActive(): Boolean = false

        /**
         * Fast synchronous check: true only if this process itself is hooked
         * (requires the module's own package to be in LSPosed scope).
         */
        fun isSelfHooked(): Boolean = try {
            isModuleActive()
        } catch (_: Throwable) {
            false
        }

        /**
         * Reliable check for SystemUI hook: sends [ACTION_HOOK_CHECK_REQUEST]
         * to SystemUI and waits for [ACTION_HOOK_CHECK_RESULT].
         * Works with scope = [SYSTEMUI_PACKAGE] only, no self-scope needed.
         */
        fun isSystemUIHookActive(
            context: Context,
            timeoutMs: Long = 2000L,
            callback: (Boolean) -> Unit
        ) {
            if (isSelfHooked()) {
                callback(true)
                return
            }

            val appContext = context.applicationContext
            var done = false
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            lateinit var receiver: BroadcastReceiver

            val complete = { active: Boolean ->
                if (!done) {
                    done = true
                    mainHandler.removeCallbacksAndMessages(null)
                    try {
                        appContext.unregisterReceiver(receiver)
                    } catch (_: Throwable) {
                    }
                    try {
                        callback(active)
                    } catch (_: Throwable) {
                    }
                }
            }

            receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action == ACTION_HOOK_CHECK_RESULT) {
                        complete(true)
                    }
                }
            }

            try {
                val filter = IntentFilter(ACTION_HOOK_CHECK_RESULT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(
                        receiver,
                        filter,
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    @SuppressLint("UnspecifiedRegisterReceiverFlag")
                    appContext.registerReceiver(receiver, filter)
                }
            } catch (_: Throwable) {
                complete(false)
                return
            }

            try {
                val request = Intent(ACTION_HOOK_CHECK_REQUEST)
                    .setPackage(SYSTEMUI_PACKAGE)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                appContext.sendBroadcast(request)
            } catch (_: Throwable) {
                complete(false)
                return
            }

            mainHandler.postDelayed({
                complete(false)
            }, timeoutMs)
        }
    }
}
