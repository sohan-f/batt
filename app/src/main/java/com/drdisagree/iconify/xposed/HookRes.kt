package com.drdisagree.iconify.xposed

import android.content.res.Resources

/**
 * Module resources resolved from a hooked context. Dimension overrides are
 * applied via method hooks (see ResourceHookManager).
 */
object HookRes {
    lateinit var modRes: Resources
}
