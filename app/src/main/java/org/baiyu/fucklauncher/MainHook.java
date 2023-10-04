package org.baiyu.fucklauncher;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final ArrayList<String> PACKAGE_NAMES = new ArrayList<>(Arrays.asList(
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3"
    ));
    private static final String CLASS_NAME = "com.android.launcher3.touch.WorkspaceTouchListener";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (! PACKAGE_NAMES.contains(lpparam.packageName)) {
            return;
        }

        XposedBridge.hookMethod(
                GestureDetector.SimpleOnGestureListener.class.getMethod("onDoubleTap", MotionEvent.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!param.thisObject.getClass().getName().equals(CLASS_NAME)) {
                            return;
                        }
                        Shell.cmd("input keyevent 223").exec();
                        param.setResult(true);
                    }
                }
        );
    }
}
