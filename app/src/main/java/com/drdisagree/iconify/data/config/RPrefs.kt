package com.drdisagree.iconify.data.config

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.drdisagree.iconify.Iconify.Companion.appContext
import com.drdisagree.iconify.data.common.Resources.SHARED_XPREFERENCES

@Suppress("unused")
object RPrefs : SharedPreferences {

    private val prefs: SharedPreferences by lazy {
        appContext.createDeviceProtectedStorageContext().getSharedPreferences(
            SHARED_XPREFERENCES, MODE_PRIVATE
        )
    }

    val instance: RPrefs
        get() = this

    val getPrefs: SharedPreferences
        get() = prefs

    // NOTE: SharedPreferences.Editor instances are single-use and not safe to
    // share across threads. Always use a fresh editor per write.
    // Basic put methods
    fun putBoolean(key: String?, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun putInt(key: String?, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun putFloat(key: String?, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun putLong(key: String?, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun putString(key: String?, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    // Basic get methods
    fun getBoolean(key: String?): Boolean {
        return prefs.getBoolean(key, false)
    }

    fun getInt(key: String?): Int {
        return prefs.getInt(key, 0)
    }

    fun getLong(key: String?): Long {
        return prefs.getLong(key, 0)
    }

    fun getFloat(key: String?): Float {
        return prefs.getFloat(key, 0f)
    }

    fun getString(key: String?): String? {
        return prefs.getString(key, null)
    }

    // Custom slider preference methods
    fun getSliderInt(key: String?, defaultVal: Int): Int {
        return try {
            getInt(key, defaultVal)
        } catch (_: Exception) {
            try {
                getString(key)?.toIntOrNull() ?: defaultVal
            } catch (_: Exception) {
                defaultVal
            }
        }
    }

    fun getSliderValues(key: String?, defaultValue: Float): List<Float> {
        return listOf(getSliderFloat(key, defaultValue))
    }

    fun getSliderFloat(key: String?, defaultVal: Float): Float {
        return try {
            getFloat(key, defaultVal)
        } catch (_: Exception) {
            try {
                getString(key)?.toFloatOrNull() ?: defaultVal
            } catch (_: Exception) {
                defaultVal
            }
        }
    }

    // Clear methods
    fun clearPref(key: String?) {
        prefs.edit().remove(key).apply()
    }

    fun clearPrefs(vararg keys: String?) {
        prefs.edit().apply {
            keys.forEach { key ->
                remove(key)
            }
            apply()
        }
    }

    fun clearAllPrefs() {
        prefs.edit().clear().apply()
    }

    // Implementing SharedPreferences interface
    override fun getAll(): Map<String, *> {
        return prefs.all
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return prefs.getBoolean(key, defValue)
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return prefs.getInt(key, defValue)
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return prefs.getLong(key, defValue)
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return prefs.getFloat(key, defValue)
    }

    override fun getString(key: String?, defValue: String?): String? {
        return prefs.getString(key, defValue)
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return prefs.getStringSet(key, defValues)
    }

    override fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return prefs.edit()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
