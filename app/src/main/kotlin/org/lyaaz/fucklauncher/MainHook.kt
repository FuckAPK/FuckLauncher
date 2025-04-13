package org.lyaaz.fucklauncher

import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
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

        // Hook the double tap gesture
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

        // Hook the AdaptiveIconDrawable to force monochrome icons
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                XposedHelpers.findAndHookMethod(
                    AdaptiveIconDrawable::class.java,
                    "getMonochrome",
                    MonoIconHook(lpparam)
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
                "com.android.launcher3.lineage.trust.HiddenAppsFilter",
                lpparam.classLoader,
                "shouldShowApp",
                ComponentName::class.java,
                AutoHideHook(lpparam)
            )
        }.onFailure {
            XposedBridge.log(it)
        }.onSuccess {
            runCatching {
//                XposedHelpers.findAndHookMethod(
//                    "com.android.launcher3.dragndrop.DragController",
//                    lpparam.classLoader,
//                    "callOnDragEnd",
//                    RefreshAppsHook(lpparam)
//                )
                XposedHelpers.findAndHookMethod(
                    "com.android.launcher3.model.ModelDbController",
                    lpparam.classLoader,
                    "insert",
                    String::class.java,
                    ContentValues::class.java,
                    RefreshAppsHook(lpparam)
                )
                XposedHelpers.findAndHookMethod(
                    "com.android.launcher3.model.ModelDbController",
                    lpparam.classLoader,
                    "delete",
                    String::class.java,
                    String::class.java,
                    Array<String>::class.java,
                    RefreshAppsHook(lpparam)
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

    class MonoIconHook(private val lpparam: LoadPackageParam) : XC_MethodHook() {

        override fun afterHookedMethod(param: MethodHookParam) {
            runCatching {
                if (param.result != null) {
                    return
                }
                prefs.reload()
                if (settings.enableForcedMonoIcon()) {
                    val monoChromeIcon = MonochromeIconFactory(100).wrap(param.thisObject as Drawable)
                    param.result = monoChromeIcon
                }
            }.onFailure {
                XposedBridge.log(it)
            }
        }
    }

    class AutoHideHook(private val lpparam: LoadPackageParam) : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            runCatching {
                prefs.reload()
                if (settings.enableAutoHide()) {
                    val componentName = param.args?.get(0) as? ComponentName ?: return
                    val mContext: Context = AndroidAppHelper.currentApplication()
                    val modelDbControllerClazz = XposedHelpers.findClass(
                        "com.android.launcher3.model.ModelDbController",
                        lpparam.classLoader
                    )
                    val mDbController = XposedHelpers.newInstance(modelDbControllerClazz, mContext)
                    val c = XposedHelpers.callMethod(
                        mDbController,
                        "query",
                        "favorites",
                        arrayOf("intent"),
                        "itemType = ?",
                        arrayOf("0"),
                        "_id"
                    ) as Cursor

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

    class RefreshAppsHook(private val lpparam: LoadPackageParam) : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            runCatching {
                if (param.result is Int && param.result as Int <= 0) {
                    return
                }
                prefs.reload()
                if (settings.enableAutoHide()) {
                    val launcherAppStateClazz = XposedHelpers.findClass(
                        "com.android.launcher3.LauncherAppState",
                        lpparam.classLoader
                    )
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
