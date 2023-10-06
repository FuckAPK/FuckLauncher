package org.baiyu.fucklauncher;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.util.UUID;

public class LockScreenReceiver extends BroadcastReceiver {

    private static final String PREF_KEY = "key";

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = getPrefs(context);

        String key = prefs.getString(PREF_KEY, null);

        if (key == null) {
            generateKey(prefs);
            return;
        }

        if (key.equals(intent.getStringExtra(PREF_KEY))) {
            lockScreen();
        }
    }

    private void lockScreen() {
        Shell.cmd("input keyevent 223").exec();
    }

    /** @noinspection deprecation*/
    @SuppressLint("WorldReadableFiles")
    private SharedPreferences getPrefs(Context context) {
        SharedPreferences prefs = null;
        try {
            prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            Log.e("fucklauncher", e.toString());
        }
        assert prefs != null;
        return prefs;
    }

    private void generateKey(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        String key = UUID.randomUUID().toString();
        editor.putString(PREF_KEY, key);
        editor.apply();
    }
}