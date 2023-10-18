package org.baiyu.fucklauncher;

import android.content.SharedPreferences;

import de.robv.android.xposed.XSharedPreferences;

public class Settings {

    private static final String PREF_ENABLE_DOUBLE_TAP_TO_SLEEP = "enable_double_tap_to_sleep";
    private static final String PREF_ENABLE_FORCED_MONO_ICON = "enable_forced_mono_icon";
    private static final String PREF_AUTH_KEY = "auth_key";
    private volatile static Settings INSTANCE;
    private final SharedPreferences prefs;

    private Settings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static Settings getInstance(SharedPreferences prefs) {
        if (INSTANCE == null) {
            synchronized (Settings.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Settings(prefs);
                }
            }
        }
        return INSTANCE;
    }

    public boolean enableDoubleTapToSleep() {
        if (prefs instanceof XSharedPreferences xprefs) {
            xprefs.reload();
        }
        return prefs.getBoolean(PREF_ENABLE_DOUBLE_TAP_TO_SLEEP, false);
    }

    public boolean enableForcedMonoIcon() {
        return prefs.getBoolean(PREF_ENABLE_FORCED_MONO_ICON, false);
    }

    public String getPrefAuthKey() {
        return PREF_AUTH_KEY;
    }

    public String getAuthKey() {
        return prefs.getString(PREF_AUTH_KEY, null);
    }

    public void setAuthKey(String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_AUTH_KEY, value);
        editor.apply();
    }
}
