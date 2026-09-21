package com.drdisagree.iconify.xposed

import com.drdisagree.iconify.BuildConfig
import com.drdisagree.iconify.data.common.Const.SYSTEMUI_PACKAGE
import com.drdisagree.iconify.xposed.modules.BatteryStyleManager
import com.drdisagree.iconify.xposed.utils.HookCheck

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
