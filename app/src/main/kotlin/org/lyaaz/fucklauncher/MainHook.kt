package org.lyaaz.fucklauncher

import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        lpparam = loadPackageParam
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            return
        }

        // Hook the double tap gesture
        runCatching {
            XposedHelpers.findAndHookMethod(
                SimpleOnGestureListener::class.java,
                "onDoubleTap",
                MotionEvent::class.java,
                OnDoubleTapHook
            )
        }.onFailure {
            XposedBridge.log(it)
        }

        // Hook the AdaptiveIconDrawable to force monochrome icons
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    XposedBridge.hookAllConstructors(
                        BaseIconFactoryClazz,
                        IconBitmapSizeHook
                    )
                }.onFailure {
                    XposedBridge.log("Failed to load icon bitmap size: $it")
                }
                XposedHelpers.findAndHookMethod(
                    AdaptiveIconDrawable::class.java,
                    "getMonochrome",
                    MonoIconHook
                )
            } else {
                XposedBridge.log("Unsupported API level: ${Build.VERSION.SDK_INT}")
            }
        }.onFailure {
            XposedBridge.log(it)
        }

        // Hook the HiddenAppsFilter to auto-hide apps
        runCatching {

            XposedHelpers.findAndHookMethod(
                hiddenAppsFilterClazz,
                "shouldShowApp",
                ComponentName::class.java,
                AutoHideHook
            )
        }.onFailure {
            XposedBridge.log(it)
        }.onSuccess {
            runCatching {
                XposedBridge.hookAllMethods(
                    modelDbControllerClazz,
                    "insert",
                    RefreshAppsHook
                )
                XposedBridge.hookAllMethods(
                    modelDbControllerClazz,
                    "delete",
                    RefreshAppsHook
                )
            }.onFailure {
                XposedBridge.log(it)
            }
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

    object MonoIconHook : XC_MethodHook() {

        override fun afterHookedMethod(param: MethodHookParam) {
            runCatching {
                if (param.result != null) {
                    return
                }
                prefs.reload()
                if (settings.enableForcedMonoIcon()) {
                    if (Thread.currentThread().stackTrace.any { it.methodName == "getIcon" }) {
                        return
                    }
                    val drawable = param.thisObject as AdaptiveIconDrawable
                    val monoChromeIcon = MonochromeIconFactory(mIconBitmapSize).wrap(drawable)
                    param.result = monoChromeIcon
                }
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }

    object AutoHideHook : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            runCatching {
                prefs.reload()
                if (settings.enableAutoHide()) {
                    val componentName = param.args?.get(0) as? ComponentName ?: return
                    val mContext: Context = AndroidAppHelper.currentApplication()
                    val launcherAppState = XposedHelpers.callStaticMethod(
                        launcherAppStateClazz,
                        "getInstance",
                        mContext
                    )
                    val mModel = XposedHelpers.getObjectField(launcherAppState, "model")
                    val mDbController = XposedHelpers.getObjectField(mModel, "modelDbController")

                    val c = if (Build.VERSION.SDK_INT >= 36) {
                        XposedHelpers.callMethod(
                            mDbController,
                            "query",
                            arrayOf("intent"),
                            "itemType = ?",
                            arrayOf("0"),
                            "_id"
                        ) as Cursor
                    } else {
                        XposedHelpers.callMethod(
                            mDbController,
                            "query",
                            "favorites",
                            arrayOf("intent"),
                            "itemType = ?",
                            arrayOf("0"),
                            "_id"
                        ) as Cursor
                    }

                    while (c.moveToNext()) {
                        val intentStr = c.getString(0)
                        if (intentStr != null) {
                            val intent = Intent.parseUri(intentStr, 0)
                            if (componentName.packageName == intent.component?.packageName) {
                                param.result = false
                                return
                            }
                        }
                    }
                }
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }

    object RefreshAppsHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            if (param.result is Int && param.result as Int <= 0) {
                return
            }

            Handler(Looper.getMainLooper()).postDelayed({
                runCatching {
                    prefs.reload()
                    if (settings.enableAutoHide()) {
                        val mContext: Context = AndroidAppHelper.currentApplication()
                        val mLauncherAppState = XposedHelpers.callStaticMethod(
                            launcherAppStateClazz,
                            "getInstance",
                            mContext
                        )
                        val mModel = XposedHelpers.getObjectField(mLauncherAppState, "mModel")
                        XposedHelpers.callMethod(
                            mModel,
                            "forceReload"
                        )
                    }
                }.onFailure {
                    XposedBridge.log(it)
                }
            }, 1000)
        }
    }

    object IconBitmapSizeHook : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            mIconBitmapSize = XposedHelpers.getIntField(param.thisObject, "mIconBitmapSize")
        }
    }

    companion object {
        private val prefs: XSharedPreferences by lazy {
            XSharedPreferences(BuildConfig.APPLICATION_ID)
        }
        private val settings: Settings by lazy {
            Settings.getInstance(prefs)
        }
        private lateinit var lpparam: LoadPackageParam

        private var mIconBitmapSize = 100

        private val modelDbControllerClazz by lazy {
            XposedHelpers.findClass(
                "com.android.launcher3.model.ModelDbController",
                lpparam.classLoader
            )
        }
        private val BaseIconFactoryClazz by lazy {
            XposedHelpers.findClass(
                "com.android.launcher3.icons.BaseIconFactory",
                lpparam.classLoader
            )
        }
        private val launcherAppStateClazz by lazy {
            XposedHelpers.findClass(
                "com.android.launcher3.LauncherAppState",
                lpparam.classLoader
            )
        }
        private val hiddenAppsFilterClazz by lazy {
            XposedHelpers.findClass(
                "com.android.launcher3.lineage.trust.HiddenAppsFilter",
                lpparam.classLoader
            )
        }
    }
}
