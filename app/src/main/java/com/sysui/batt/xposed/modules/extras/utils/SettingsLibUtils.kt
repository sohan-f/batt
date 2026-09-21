package com.sysui.batt.xposed.modules.extras.utils

import android.content.Context
import android.content.res.ColorStateList
import com.sysui.batt.xposed.ModPack
import com.sysui.batt.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.sysui.batt.xposed.modules.extras.utils.toolkit.callStaticMethod
import com.sysui.batt.xposed.modules.extras.utils.toolkit.log

class SettingsLibUtils(context: Context) : ModPack(context) {

    override fun updatePrefs(vararg key: String) {}

    override fun handleLoadPackage(packageName: String, classLoader: ClassLoader) {
        UtilsClass = findClass(
            "com.android.settingslib.Utils",
            classLoader = classLoader,
            suppressError = true
        )
    }

    companion object {
        private var UtilsClass: Class<*>? = null

        fun getColorAttr(resID: Int, context: Context): ColorStateList {
            return getColorAttr(
                context,
                resID
            )
        }

        fun getColorAttr(context: Context, resID: Int): ColorStateList {
            return getColorStateListFromUtils(
                "getColorAttr",
                context,
                resID
            )
        }

        fun getColorAttrDefaultColor(resID: Int, context: Context, defValue: Int = 0): Int {
            return getColorFromUtils(
                "getColorAttrDefaultColor",
                context,
                resID,
                defValue
            )
        }

        fun getColorAttrDefaultColor(context: Context, resID: Int, defValue: Int = 0): Int {
            return getColorFromUtils(
                "getColorAttrDefaultColor",
                context,
                resID,
                defValue
            )
        }

        fun getColorStateListDefaultColor(context: Context, resID: Int): Int {
            return getColorStateListFromUtils(
                "getColorStateListDefaultColor",
                context,
                resID
            ).defaultColor
        }

        @Suppress("SameParameterValue")
        private fun getColorFromUtils(
            methodName: String,
            context: Context,
            resID: Int,
            defValue: Int = 0
        ): Int {
            if (UtilsClass != null) {
                try {
                    return UtilsClass.callStaticMethod(
                        methodName,
                        resID,
                        context
                    ) as Int
                } catch (ignored: Throwable) {
                    try {
                        return UtilsClass.callStaticMethod(
                            methodName,
                            context,
                            resID
                        ) as Int
                    } catch (ignored: Throwable) {
                        try {
                            return UtilsClass.callStaticMethod(
                                methodName,
                                context,
                                resID,
                                defValue
                            ) as Int
                        } catch (ignored: Throwable) {
                            try {
                                return UtilsClass.callStaticMethod(
                                    methodName,
                                    resID,
                                    defValue,
                                    context
                                ) as Int
                            } catch (throwable: Throwable) {
                                log(SettingsLibUtils, throwable)
                            }
                        }
                    }
                }
            }

            // SettingsLibUtils is never registered as a ModPack in EntryList,
            // so UtilsClass is always null and every lookup above is skipped.
            // Resolve the theme attribute directly instead of returning
            // defValue (0 = fully transparent): a 0 tint makes shade-header
            // icons and the battery drawable disappear on QS pull-down.
            return resolveThemeColor(context, resID, defValue)
        }

        private fun resolveThemeColor(context: Context, resID: Int, defValue: Int): Int {
            return try {
                val attrs = context.theme.obtainStyledAttributes(intArrayOf(resID))
                try {
                    attrs.getColor(0, defValue)
                } finally {
                    attrs.recycle()
                }
            } catch (_: Throwable) {
                defValue
            }
        }

        private fun getColorStateListFromUtils(
            methodName: String,
            context: Context,
            resID: Int
        ): ColorStateList {
            if (UtilsClass == null) return ColorStateList.valueOf(0)

            return try {
                UtilsClass.callStaticMethod(
                    methodName,
                    resID,
                    context
                ) as ColorStateList
            } catch (ignored: Throwable) {
                try {
                    UtilsClass.callStaticMethod(
                        methodName,
                        context,
                        resID
                    ) as ColorStateList
                } catch (throwable: Throwable) {
                    log(SettingsLibUtils, throwable)
                    ColorStateList.valueOf(0)
                }
            }
        }
    }
}
