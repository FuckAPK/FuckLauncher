package org.baiyu.fucklauncher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.topjohnwu.superuser.Shell
import java.util.UUID

class LockScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = getPrefs(context)
        val settings: Settings = Settings.getInstance(prefs)

        val prefAuthKey = Settings.PREF_AUTH_KEY
        val authKey = settings.authKey

        if (authKey == null) {
            settings.authKey = UUID.randomUUID().toString()
            // grant root
            lockScreen()
            return
        }

        if (authKey == intent.getStringExtra(prefAuthKey)) {
            lockScreen()
        }
    }

    private fun lockScreen() {
        Shell.cmd("input keyevent 223").exec()
    }

    /**
     * @noinspection deprecation
     */
    @SuppressLint("WorldReadableFiles")
    private fun getPrefs(context: Context): SharedPreferences {
        val prefsName = "${BuildConfig.APPLICATION_ID}_preferences"
        return try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(
                prefsName,
                Context.MODE_WORLD_READABLE
            )
        } catch (ignore: SecurityException) {
            context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
            )
        }
    }
}