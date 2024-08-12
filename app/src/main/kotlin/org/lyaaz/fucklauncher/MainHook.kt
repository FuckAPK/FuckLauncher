package org.lyaaz.fucklauncher

import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (!PACKAGE_NAMES.contains(lpparam.packageName)) {
            return
        }

        try {
            XposedBridge.hookMethod(
                SimpleOnGestureListener::class.java.getMethod(
                    "onDoubleTap",
                    MotionEvent::class.java
                ),
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.thisObject.javaClass.name != "com.android.launcher3.touch.WorkspaceTouchListener") {
                            return
                        }
                        prefs.reload()
                        if (!settings.enableDoubleTapToSleep()) {
                            return
                        }

                        val mContext: Context = AndroidAppHelper.currentApplication()

                        val intent = Intent("org.lyaaz.fucklauncher.LockScreen")
                            .setComponent(
                                ComponentName(
                                    BuildConfig.APPLICATION_ID,
                                    LockScreenReceiver::class.java.name
                                )
                            )
                            .putExtra(Settings.PREF_AUTH_KEY, settings.authKey)
                        mContext.sendBroadcast(intent)
                        XposedBridge.log("LockScreen intent sent")
                        param.result = true
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }

        try {
            XposedBridge.hookAllMethods(
                XposedHelpers.findClass(
                    "com.android.launcher3.uioverrides.flags.FlagsFactory",
                    lpparam.classLoader
                ),
                "getDebugFlag",
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if ("ENABLE_FORCED_MONO_ICON" == param.args[1]) {
                            prefs.reload()
                            if (settings.enableForcedMonoIcon()) {
                                XposedHelpers.setBooleanField(param.result, "mCurrentValue", true)
                                XposedBridge.log("Mono Icon enabled.")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log(t)
        }
    }

    companion object {
        private val PACKAGE_NAMES = ArrayList(
            mutableListOf(
                "com.google.android.apps.nexuslauncher",
                "com.android.launcher3"
            )
        )
        private val prefs: XSharedPreferences by lazy {
            XSharedPreferences(BuildConfig.APPLICATION_ID)
        }
        private val settings: Settings by lazy {
            Settings.getInstance(prefs)
        }
    }
}
