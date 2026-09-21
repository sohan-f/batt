package com.sysui.batt.xposed.utils

import android.content.Context
import com.crossbowffs.remotepreferences.RemotePreferences

@Suppress("unused")
class ExtendedRemotePreferences : RemotePreferences {

    constructor(context: Context, authority: String, prefFileName: String) : super(
        context,
        authority,
        prefFileName
    )

    constructor(
        context: Context,
        authority: String,
        prefFileName: String,
        strictMode: Boolean
    ) : super(context, authority, prefFileName, strictMode)

    fun getBoolean(key: String?): Boolean {
        return getBoolean(key, false)
    }

    fun getSliderInt(key: String?, defaultVal: Int): Int {
        return try {
            getInt(key, defaultVal)
        } catch (_: Exception) {
            try {
                getString(key, null)?.toIntOrNull() ?: defaultVal
            } catch (_: Exception) {
                defaultVal
            }
        }
    }

    fun getSliderFloat(key: String?, defaultVal: Float): Float {
        return try {
            getFloat(key, defaultVal)
        } catch (_: Exception) {
            try {
                getString(key, null)?.toFloatOrNull() ?: defaultVal
            } catch (_: Exception) {
                defaultVal
            }
        }
    }

    fun getSliderValues(key: String?, defaultValue: Float): List<Float> {
        return listOf(getSliderFloat(key, defaultValue))
    }
}
