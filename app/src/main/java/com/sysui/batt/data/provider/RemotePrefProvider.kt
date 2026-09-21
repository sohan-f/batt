package com.sysui.batt.data.provider

import com.crossbowffs.remotepreferences.RemotePreferenceFile
import com.crossbowffs.remotepreferences.RemotePreferenceProvider
import com.sysui.batt.BuildConfig
import com.sysui.batt.data.common.Resources

class RemotePrefProvider : RemotePreferenceProvider(
    BuildConfig.APPLICATION_ID,
    arrayOf(RemotePreferenceFile(Resources.SHARED_XPREFERENCES, true))
)