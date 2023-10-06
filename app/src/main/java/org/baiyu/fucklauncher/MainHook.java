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
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final ArrayList<String> PACKAGE_NAMES = new ArrayList<>(Arrays.asList(
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3"
    ));
    private static final String CLASS_NAME = "com.android.launcher3.touch.WorkspaceTouchListener";
    private static final String METHOD_NAME = "onDoubleTap";
    private static final String ACTION = "org.baiyu.fucklauncher.LockScreen";
    private static final String PREF_KEY = "key";

    private static final XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (! PACKAGE_NAMES.contains(lpparam.packageName)) {
            return;
        }

        XposedBridge.hookMethod(
                GestureDetector.SimpleOnGestureListener.class.getMethod(METHOD_NAME, MotionEvent.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!param.thisObject.getClass().getName().equals(CLASS_NAME)) {
                            return;
                        }
                        Context mContext = AndroidAppHelper.currentApplication();

                        prefs.reload();
                        String key = prefs.getString(PREF_KEY, null);

                        Intent intent = new Intent(ACTION)
                                .setComponent(new ComponentName(BuildConfig.APPLICATION_ID, LockScreenReceiver.class.getName()))
                                .putExtra(PREF_KEY, key);
                        mContext.sendBroadcast(intent);

                        param.setResult(true);
                    }
                }
        );
    }
}
