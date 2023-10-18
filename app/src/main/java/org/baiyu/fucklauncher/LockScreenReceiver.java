package org.baiyu.fucklauncher;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.topjohnwu.superuser.Shell;

import java.util.UUID;

public class LockScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = getPrefs(context);
        Settings settings = Settings.getInstance(prefs);

        String PREF_AUTH_KEY = settings.getPrefAuthKey();
        String AUTH_KEY = settings.getAuthKey();

        if (AUTH_KEY == null) {
            settings.setAuthKey(UUID.randomUUID().toString());
            grantRoot();
            return;
        }

        if (AUTH_KEY.equals(intent.getStringExtra(PREF_AUTH_KEY))) {
            lockScreen();
        }
    }

    private void grantRoot() {
        Shell.cmd("ls /data").exec();
    }

    private void lockScreen() {
        Shell.cmd("input keyevent 223").exec();
    }

    /**
     * @noinspection deprecation
     */
    @SuppressLint("WorldReadableFiles")
    private SharedPreferences getPrefs(Context context) {
        SharedPreferences prefs;
        try {
            prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);
        }
        assert prefs != null;
        return prefs;
    }
}