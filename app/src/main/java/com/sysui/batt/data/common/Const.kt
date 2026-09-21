package com.sysui.batt.data.common

import com.sysui.batt.BuildConfig
import com.sysui.batt.xposed.utils.BootLoopProtector

object Const {

    // System packages
    const val SYSTEMUI_PACKAGE = "com.android.systemui"
    const val FRAMEWORK_PACKAGE = "android"

    // Github repo
    const val GITHUB_REPO = "https://github.com/sohan-f/batt"

    // Parse new update
    const val LATEST_VERSION_URL =
        "https://raw.githubusercontent.com/sohan-f/batt/main/latestVersion.json"

    // Parse changelogs
    const val CHANGELOG_URL = "https://api.github.com/repos/sohan-f/batt/releases/tags/v"

    // Xposed variables
    val PREF_UPDATE_EXCLUSIONS = listOf(
        BootLoopProtector.LOAD_TIME_KEY_KEY,
        BootLoopProtector.PACKAGE_STRIKE_KEY_KEY,
    )

    const val ACTION_HOOK_CHECK_REQUEST = "${BuildConfig.APPLICATION_ID}.ACTION_HOOK_CHECK_REQUEST"
    const val ACTION_HOOK_CHECK_RESULT = "${BuildConfig.APPLICATION_ID}.ACTION_HOOK_CHECK_RESULT"
}
