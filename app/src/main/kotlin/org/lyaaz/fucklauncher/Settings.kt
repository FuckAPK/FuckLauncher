package org.lyaaz.fucklauncher

import android.content.SharedPreferences

class Settings private constructor(private val prefs: SharedPreferences) {
    fun enableDoubleTapToSleep(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_DOUBLE_TAP_TO_SLEEP, true)
    }

    fun enableForcedMonoIcon(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCED_MONO_ICON, true)
    }

    var authKey: String?
        get() = prefs.getString(PREF_AUTH_KEY, null)
        set(value) {
            val editor = prefs.edit()
            editor.putString(PREF_AUTH_KEY, value)
            editor.apply()
        }
    fun enableAutoHide(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_AUTO_HIDE, false)
    }

    companion object {
        const val PREF_ENABLE_DOUBLE_TAP_TO_SLEEP = "enable_double_tap_to_sleep"
        const val PREF_ENABLE_FORCED_MONO_ICON = "enable_forced_mono_icon"
        const val PREF_AUTH_KEY = "auth_key"
        const val PREF_ENABLE_AUTO_HIDE = "enable_auto_hide"

        @Volatile
        private var INSTANCE: Settings? = null
        fun getInstance(prefs: SharedPreferences): Settings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Settings(prefs).also { INSTANCE = it }
            }
        }
    }
}
