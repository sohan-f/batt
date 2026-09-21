package com.sysui.batt.utils

import com.sysui.batt.data.common.Const.SYSTEMUI_PACKAGE
import com.sysui.batt.data.config.RPrefs
import com.sysui.batt.xposed.utils.BootLoopProtector.LOAD_TIME_KEY_KEY
import com.sysui.batt.xposed.utils.BootLoopProtector.PACKAGE_STRIKE_KEY_KEY
import com.topjohnwu.superuser.Shell
import java.util.Calendar

object SystemUtils {

    fun restartSystemUI() {
        val loadTimeKey = String.format("%s%s", LOAD_TIME_KEY_KEY, SYSTEMUI_PACKAGE)
        val strikeKey = String.format("%s%s", PACKAGE_STRIKE_KEY_KEY, SYSTEMUI_PACKAGE)
        val currentTime = Calendar.getInstance().time.time

        RPrefs.putLong(loadTimeKey, currentTime)
        RPrefs.putInt(strikeKey, 0)
        val result = Shell.cmd("killall $SYSTEMUI_PACKAGE").exec()
        if (!result.isSuccess) {
            throw IllegalStateException("SystemUI restart failed (no root?): ${result.err}")
        }
    }
}
