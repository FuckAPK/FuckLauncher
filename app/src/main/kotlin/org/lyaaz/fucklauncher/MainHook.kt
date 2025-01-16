package org.lyaaz.fucklauncher

import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import de.robv.android.xposed.*
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

            val baseIconFactoryClazz = XposedHelpers.findClass(
                "com.android.launcher3.icons.BaseIconFactory",
                lpparam.classLoader
            )

            when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.TIRAMISU,
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {

                    XposedHelpers.findAndHookMethod(
                        baseIconFactoryClazz,
                        "getMonochromeDrawable",
                        Drawable::class.java,
                        MonoIconHook(lpparam)
                    )
                }

                Build.VERSION_CODES.VANILLA_ICE_CREAM -> {

                    XposedHelpers.findAndHookMethod(
                        baseIconFactoryClazz,
                        "getMonochromeDrawable",
                        AdaptiveIconDrawable::class.java,
                        MonoIconHook(lpparam)
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

    class MonoIconHook(private val lpparam: LoadPackageParam) : XC_MethodHook() {

        override fun afterHookedMethod(param: MethodHookParam) {
            runCatching {
                if (param.result != null) {
                    return
                }
                prefs.reload()
                if (settings.enableForcedMonoIcon()) {
                    val base = param.args[0] as? Drawable ?: return
                    val monoChromeIcon = MonochromeIconFactory(100).wrap(base)
                    if (base is AdaptiveIconDrawable) {
                        val clippedMonoDrawableClazz = XposedHelpers.findClass(
                            "com.android.launcher3.icons.BaseIconFactory.ClippedMonoDrawable",
                            lpparam.classLoader
                        )
                        param.result =
                            XposedHelpers.newInstance(clippedMonoDrawableClazz, MonochromeIconFactory(100).wrap(base))
                    } else {
                        param.result = monoChromeIcon
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
