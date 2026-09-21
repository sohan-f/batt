package com.sysui.batt.xposed

import com.sysui.batt.BuildConfig
import com.sysui.batt.data.common.Const.SYSTEMUI_PACKAGE
import com.sysui.batt.xposed.modules.BatteryStyleManager
import com.sysui.batt.xposed.utils.HookCheck

object EntryList {

    fun getEntries(packageName: String): ArrayList<Class<out ModPack>> {
        val modPacks = ArrayList<Class<out ModPack>>()

        if (packageName == BuildConfig.APPLICATION_ID) {
            modPacks.add(HookCheck::class.java)
        }

        if (packageName == SYSTEMUI_PACKAGE) {
            modPacks.add(HookCheck::class.java)
            if (!HookEntry.isChildProcess) {
                modPacks.add(BatteryStyleManager::class.java)
            }
        }

        return modPacks
    }
}
