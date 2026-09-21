package com.sysui.batt.xposed.utils

object SystemUtils {

    fun sleep(millis: Int) {
        try {
            Thread.sleep(millis.toLong())
        } catch (ignored: Throwable) {
        }
    }
}
