package org.lyaaz.fucklauncher

import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            return
        }

        runCatching {
            XposedBridge.hookMethod(
                SimpleOnGestureListener::class.java.getDeclaredMethod(
                    "onDoubleTap",
                    MotionEvent::class.java
                ),
                OnDoubleTapHook
            )
        }.onFailure {
            XposedBridge.log(it)
        }

        runCatching {
            val lineageos20Method by lazy {
                XposedHelpers.findMethodExact(
                    "com.android.launcher3.uioverrides.flags.FlagsFactory",
                    lpparam.classLoader,
                    "getDebugFlag",
                    Int::class.java,
                    String::class.java,
                    Boolean::class.java,
                    String::class.java
                )
            }
            val lineageos21Method by lazy {
                XposedHelpers.findMethodExact(
                    "com.android.launcher3.uioverrides.flags.FlagsFactory",
                    lpparam.classLoader,
                    "getDebugFlag",
                    Int::class.java,
                    String::class.java,
                    "com.android.launcher3.config.FeatureFlags\$FlagState",
                    String::class.java
                )
            }
            when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.TIRAMISU -> {
                    XposedBridge.hookMethod(
                        lineageos20Method,
                        GetDebugFlagHook
                    )
                }

                Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    XposedBridge.hookMethod(
                        lineageos21Method,
                        GetDebugFlagHook
                    )
                }

                else -> {
                    XposedBridge.log("Unsupported API level: ${Build.VERSION.SDK_INT}")
                }
            }
        }.onFailure {
            XposedBridge.log(it)
        }
    }

    object OnDoubleTapHook : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            runCatching {
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
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }

    object GetDebugFlagHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            runCatching {
                if ("ENABLE_FORCED_MONO_ICON" == param.args[1]) {
                    prefs.reload()
                    if (settings.enableForcedMonoIcon()) {
                        XposedHelpers.setBooleanField(param.result, "mCurrentValue", true)
                        XposedBridge.log("Mono Icon enabled.")
                    }
                }
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }

    companion object {
        private val prefs: XSharedPreferences by lazy {
            XSharedPreferences(BuildConfig.APPLICATION_ID)
        }
        private val settings: Settings by lazy {
            Settings.getInstance(prefs)
        }
    }
}
