package org.baiyu.fucklauncher;

import android.app.AndroidAppHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final ArrayList<String> PACKAGE_NAMES = new ArrayList<>(Arrays.asList(
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3"
    ));
    private static final XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
    private static final Settings settings = Settings.getInstance(prefs);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PACKAGE_NAMES.contains(lpparam.packageName)) {
            return;
        }

        try {
            XposedBridge.hookMethod(
                    GestureDetector.SimpleOnGestureListener.class.getMethod("onDoubleTap", MotionEvent.class),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!param.thisObject.getClass().getName().equals("com.android.launcher3.touch.WorkspaceTouchListener")) {
                                return;
                            }
                            if (!settings.enableDoubleTapToSleep()) {
                                return;
                            }

                            Context mContext = AndroidAppHelper.currentApplication();

                            prefs.reload();
                            String PREF_AUTH_KEY = settings.getPrefAuthKey();
                            String AUTH_KEY = settings.getAuthKey();

                            Intent intent = new Intent("org.baiyu.fucklauncher.LockScreen")
                                    .setComponent(new ComponentName(BuildConfig.APPLICATION_ID, LockScreenReceiver.class.getName()))
                                    .putExtra(PREF_AUTH_KEY, AUTH_KEY);
                            mContext.sendBroadcast(intent);

                            param.setResult(true);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.launcher3.uioverrides.flags.FlagsFactory",
                    lpparam.classLoader,
                    "getDebugFlag",
                    int.class,
                    String.class,
                    boolean.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if ("ENABLE_FORCED_MONO_ICON".equals((String) param.args[1])) {
                                prefs.reload();
                                if (settings.enableForcedMonoIcon()) {
                                    XposedBridge.log("ENABLE_FORCED_MONO_ICON hooked");
                                    param.args[2] = true;
                                }
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
