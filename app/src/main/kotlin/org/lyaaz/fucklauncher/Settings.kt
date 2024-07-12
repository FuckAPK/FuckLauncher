package org.lyaaz.fucklauncher

import android.content.SharedPreferences
import kotlin.concurrent.Volatile

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

    companion object {
        private const val PREF_ENABLE_DOUBLE_TAP_TO_SLEEP = "enable_double_tap_to_sleep"
        private const val PREF_ENABLE_FORCED_MONO_ICON = "enable_forced_mono_icon"
        const val PREF_AUTH_KEY = "auth_key"

        @Volatile
        private var INSTANCE: Settings? = null
        fun getInstance(prefs: SharedPreferences): Settings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Settings(prefs).also { INSTANCE = it }
            }
        }
    }
}
