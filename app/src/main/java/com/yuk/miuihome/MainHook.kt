package com.yuk.miuihome

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.Keep
import com.yuk.miuihome.module.*
import com.yuk.miuihome.utils.*
import com.yuk.miuihome.utils.ktx.getObjectField
import com.yuk.miuihome.utils.ktx.hookAfterMethod
import com.yuk.miuihome.utils.ktx.setObjectField
import com.yuk.miuihome.view.*
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.concurrent.thread
import kotlin.system.exitProcess

@Keep
class MainHook {

    private val sharedPreferences = OwnSP.ownSP
    private val editor by lazy { sharedPreferences.edit() }
    private val myRes by lazy { HomeContext.resInstance.moduleRes.resources }

    fun doHook() {
        "com.miui.home.settings.MiuiHomeSettingActivity".hookAfterMethod(
            "onCreate",
            Bundle::class.java
        ) {
            HomeContext.activity = it.thisObject as Activity
        }

        "com.miui.home.settings.MiuiHomeSettings".hookAfterMethod(
            "onCreatePreferences",
            Bundle::class.java,
            String::class.java
        ) {
            (it.thisObject.getObjectField("mDefaultHomeSetting")).apply {
                setObjectField("mTitle", myRes.getString(R.string.ModuleSettings))
                setObjectField("mClickListener", object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        showSettingDialog()
                    }
                })
                if (XposedInit.hasHookPackageResources) {
                    setObjectField("mTitle", myRes.getString(R.string.ModuleSettingsResHook))
                }
            }
        }

        //修改设备分级
        SetDeviceLevel().init()
        //修改模糊等级
        ModifyBlurLevel().init()
        //开启平滑动画
        EnableSmoothAnimation().init()
        //开启文件夹模糊
        EnableBlurWhenOpenFolder().init()
        //开启水波纹
        EnableMamlDownload().init()
        //开启时钟常显
        EnableClockGadget().init()
        //动画速度调节
        ModifyAnimDurationRatio().init()
        //后台卡片圆角大小调节
        ModifyRoundedCorners().init()
        //后台卡片图标文字间距调节
        ModifyHeaderHeight().init()
        //进入后台是否隐藏状态栏
        EnableHideStatusBarWhenEnterRecents().init()
        //禁用Log
        DisableLog().init()
        //桌面搜索框模糊
        EnableSearchBarBlur().init()
        //允许最近任务横屏
        EnableRecentsViewHorizontal().init()
        //取消最近任务壁纸压暗
        DisableRecentsViewWallpaperDarken().init()
        //隐藏桌面小部件标题
        ModifyHideWidgetTitles().init()
        //允许桌面安卓小部件移到负一屏
        AllowWidgetToMinus().init()
        //允许在安卓小部件显示MIUI组件
        AlwaysShowMIUIWidget().init()
        //纵向后台卡片大小
        ModifyTaskVertical().init()
        //横向后台卡片大小
        ModifyTaskHorizontal().init()
        //开启简单动画
        EnableSimpleAnimation().init()
        //屏幕无限滚动
        ModifyInfiniteScroll().init()
        //隐藏桌面应用标题
        ModifyHideIconTitles().init()
        //解锁桌面布局限制
        ModifyUnlockGrids().init()
        //打开应用时关闭文件夹
        ModifyCloseFolderOnLaunch().init()
        //显示底栏应用标题
        ModifyShowDockIconTitles().init()
        //显示底栏应用阴影
        EnableDockIconShadow().init()
        //允许所有应用使用小窗口
        AllowAllAppsToUseSmallWindow().init()
        //允许低端机使用MIUI小组件
        EnableLowEndDeviceUseMIUIWidgets().init()
        //禁用文件夹编辑内的今日推荐
        DisableRecommendServer().init()
        HookSystemProperties().init()
//        CustomHook.init()
        ResourcesHook().init()
    }

    private fun showSettingDialog() {
        if (sharedPreferences.getBoolean("isFirstUse", true)) {
            firstUseDialog()
            return
        }
        val dialogBuilder = SettingBaseDialog().get()
        lateinit var dialog: AlertDialog
        dialogBuilder.setView(ScrollView(HomeContext.activity).apply {
            overScrollMode = 2
            addView(LinearLayout(HomeContext.activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp2px(HomeContext.context, 10f),
                    dp2px(HomeContext.context, 10f),
                    dp2px(HomeContext.context, 10f),
                    dp2px(HomeContext.context, 10f)
                )
                addView(
                    SettingTextView.FastBuilder(
                        mText = myRes.getString(R.string.app_name),
                        mSize = SettingTextView.titleSize
                    ).build()
                )
                addView(
                    SettingTextView.FastBuilder(
                        mText = myRes.getString(R.string.Warn),
                        mColor = "#ff0c0c",
                        mSize = SettingTextView.textSize
                    ).build()
                )
                addView(
                    SettingTextView.FastBuilder(
                        mText = myRes.getString(R.string.BaseFeature),
                        mColor = "#0C84FF",
                        mSize = SettingTextView.text2Size
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.SmoothAnimation),
                        mKey = "smoothAnimation"
                    ).build()
                )
                if (!OwnSP.ownSP.getBoolean(
                        "simpleAnimation",
                        false
                    ) or !OwnSP.ownSP.getBoolean("testUser", false)
                ) {
                    addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.TaskViewBlurLevel)) { showModifyBlurLevel() }
                        .build())
                }
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.AnimationLevel)) { showModifyAnimationLevel() }
                    .build())
                addView(
                    SettingTextView.FastBuilder(
                        mText = myRes.getString(R.string.AdvancedFeature),
                        mColor = "#0C84FF",
                        mSize = SettingTextView.text2Size
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.HideAppTitles),
                        mKey = "hideIconTitles"
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.UnlockGrids),
                        mKey = "unlockGrids"
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.ShowDockIconTitles),
                        mKey = "showDockIconTitles"
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.HideStatusBar),
                        mKey = "hideStatusBar"
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.MamlDownload),
                        mKey = "mamlDownload"
                    ).build()
                )
                if (!OwnSP.ownSP.getBoolean(
                        "simpleAnimation",
                        false
                    ) or !OwnSP.ownSP.getBoolean("testUser", false)
                ) {
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.WallpaperDarken),
                            mKey = "wallpaperDarken"
                        ).build()
                    )
                }
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.RoundCorner)) { showModifyRoundCorner() }
                    .build())
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.AppTextSize)) { showModifyTextSize() }
                    .build())
                addView(
                    SettingTextView.FastBuilder(
                        mText = myRes.getString(R.string.Folder),
                        mColor = "#0C84FF",
                        mSize = SettingTextView.text2Size
                    ).build()
                )
                if (HomeContext.isAlpha) {
                    if (!OwnSP.ownSP.getBoolean(
                            "simpleAnimation",
                            false
                        ) or !OwnSP.ownSP.getBoolean("testUser", false)
                    ) {
                        addView(
                            SettingSwitch.FastBuilder(
                                mText = myRes.getString(R.string.BlurWhenOpenFolder),
                                mKey = "blurWhenOpenFolder",
                                show = EnableBlurWhenOpenFolder.checked
                            ).build()
                        )
                    }
                }
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.CloseFolder),
                        mKey = "closeFolder"
                    ).build()
                )
                if (HomeContext.isWidgetLauncher) {
                    addView(
                        SettingTextView.FastBuilder(
                            mText = myRes.getString(R.string.Widget),
                            mColor = "#0C84FF",
                            mSize = SettingTextView.text2Size
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.HideWidgetTitles),
                            mKey = "hideWidgetTitles"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.WidgetToMinus),
                            mKey = "widgetToMinus"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.AlwaysShowMIUIWidget),
                            mKey = "alwaysShowMIUIWidget"
                        ).build()
                    )
                }
                if (XposedInit.hasHookPackageResources) {
                    addView(
                        SettingTextView.FastBuilder(
                            mText = myRes.getString(R.string.ResourceHooks),
                            mColor = "#0C84FF",
                            mSize = SettingTextView.text2Size
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.HideTaskViewAppIcon),
                            mKey = "buttonPadding"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.HideTaskViewCleanUpIcon),
                            mKey = "cleanUp"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.HideTaskViewSmallWindowIcon),
                            mKey = "smallWindow"
                        ).build()
                    )
                    addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.TaskViewAppCardTextSize)) { showModifyBackgroundTextSize() }
                        .build())
                }
                if (OwnSP.ownSP.getBoolean("testUser", false)) {
                    addView(
                        SettingTextView.FastBuilder(
                            mText = myRes.getString(R.string.TestFeature),
                            mColor = "#0C84FF",
                            mSize = SettingTextView.text2Size
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.SimpleAnimation),
                            mKey = "simpleAnimation"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.InfiniteScroll),
                            mKey = "infiniteScroll"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.RecommendServer),
                            mKey = "recommendServer"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.RealTaskViewHorizontal),
                            mKey = "horizontal"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.EnableIconShadow),
                            mKey = "isEnableIconShadow"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.SmallWindow),
                            mKey = "supportSmallWindow"
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.LowEndDeviceUseMIUIWidgets),
                            mKey = "useMIUIWidgets"
                        ).build()
                    )
                    addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.VerticalTaskViewOfAppCardSize)) { showModifyVertical() }
                        .build())
                    addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.HorizontalTaskViewOfAppCardSize)) { showModifyHorizontal() }
                        .build())

                }
                addView(
                    SettingTextView.FastBuilder(
                        mText = myRes.getString(R.string.OtherFeature),
                        mColor = "#0C84FF",
                        mSize = SettingTextView.text2Size
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.AlwaysShowStatusBarClock),
                        mKey = "clockGadget"
                    ).build()
                )
                if (!OwnSP.ownSP.getBoolean(
                        "dockSettings", false
                    )
                ) {
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.SearchBarBlur),
                            mKey = "searchBarBlur"
                        ).build()
                    )
                }
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.DockFeature),
                        mKey = "dockSettings"
                    ) {
                        dialog.cancel()
                        showSettingDialog()
                    }.build()
                )
                if (OwnSP.ownSP.getBoolean(
                        "dockSettings", false
                    )
                ) {
                    addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.DockSettings)) { showDockDialog() }
                        .build())
                }
                addView(
                    SettingTextView.FastBuilder(
                        mText = myRes.getString(R.string.ModuleFeature),
                        mColor = "#0C84FF",
                        mSize = SettingTextView.text2Size
                    ).build()
                )
                addView(
                    SettingSwitch.FastBuilder(
                        mText = myRes.getString(R.string.TestFeature),
                        mKey = "testUser"
                    ) {
                        dialog.cancel()
                        showSettingDialog()
                    }.build()
                )
//                if (BuildConfig.DEBUG) {
//                    addView(SettingTextView.FastBuilder(mText = "自定义Hook") { customHookDialog() }.build())
//                }
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.EveryThingBuild)) { BuildWithEverything().init() }
                    .build())
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.CleanModuleSettings)) {
                    editor.clear(); editor.commit(); exitProcess(
                    0
                )
                }.build())
            })
        })
        dialogBuilder.setPositiveButton(myRes.getString(R.string.Close), null)
        dialogBuilder.setNeutralButton(myRes.getString(R.string.Reboot)) { _, _ -> exitProcess(0) }
        dialogBuilder.setCancelable(false)
        dialog = dialogBuilder.show()
    }

    private fun customHookDialog() {
        val argsEditText = arrayListOf<EditText>()
        val argsLinearLayout = LinearLayout(HomeContext.activity).also {
            it.orientation = LinearLayout.VERTICAL
        }

        fun createArgsEditText(): View {
            val linearView = LinearLayout(HomeContext.activity).also { layout ->
                lateinit var editText: EditText
                layout.orientation = LinearLayout.HORIZONTAL
                layout.addView(EditText(HomeContext.activity).apply {
                    argsEditText.add(this)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    editText = this
                })
                layout.addView(Button(HomeContext.activity).apply {
                    setText("X")
                    setOnClickListener {
                        argsEditText.remove(editText)
                        argsLinearLayout.removeView(layout)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        5f
                    )
                })
            }
            return linearView
        }

        val dialogBuilder = SettingBaseDialog().get()
        dialogBuilder.apply {
            setTitle("自定义Hook")
            setView(ScrollView(HomeContext.activity).apply {
                overScrollMode = 2
                addView(LinearLayout(HomeContext.activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f)
                    )
                    addView(SettingTextView.FastBuilder(mText = "Class:").build())
                    val classEditText = EditText(HomeContext.activity)
                    addView(classEditText)
                    addView(SettingTextView.FastBuilder(mText = "Method name:").build())
                    val methodEditText = EditText(HomeContext.activity)
                    addView(methodEditText)
                    addView(SettingTextView.FastBuilder(mText = "arg(s):").build())
                    addView(argsLinearLayout)
                    addView(Button(HomeContext.activity).apply {
                        setText("Add arg")
                        setOnClickListener { argsLinearLayout.addView(createArgsEditText()) }
                    })
                    addView(SettingTextView.FastBuilder(mText = "result(null直接不输入即可):").build())
                    val resultEditText = EditText(HomeContext.activity)
                    addView(resultEditText)
                    addView(
                        SettingTextView.FastBuilder(mText = "result type(null直接不输入即可):").build()
                    )
                    val resultTypeEditText = EditText(HomeContext.activity)
                    addView(resultTypeEditText)
                })
            })
        }.show()
    }

    private fun showDockDialog() {
        val dialogBuilder = SettingBaseDialog().get()
        dialogBuilder.apply {
            setView(ScrollView(HomeContext.activity).apply {
                overScrollMode = 2
                addView(LinearLayout(HomeContext.activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f)
                    )
                    addView(
                        SettingTextView.FastBuilder(
                            mText = "「" + myRes.getString(R.string.DockSettings) + "」",
                            mColor = "#0C84FF",
                            mSize = SettingTextView.text2Size
                        ).build()
                    )
                    addView(
                        SettingTextView.FastBuilder(
                            mText = myRes.getString(R.string.DockWarn),
                            mColor = "#ff0c0c",
                            mSize = SettingTextView.textSize
                        ).build()
                    )
                    addView(
                        SettingSwitch.FastBuilder(
                            mText = myRes.getString(R.string.EnableDockBlur),
                            mKey = "searchBarBlur"
                        ).build()
                    )
                    addView(
                        SettingSeekBar.FastBuilder(
                            mText = myRes.getString(R.string.DockRoundedCorners),
                            mKey = "dockRadius",
                            defValue = 25,
                            minValue = 0,
                            maxValue = 50,
                            canUserInput = false
                        ).build()
                    )
                    addView(
                        SettingSeekBar.FastBuilder(
                            mText = myRes.getString(R.string.DockHeight),
                            mKey = "dockHeight",
                            defValue = 84,
                            minValue = 50,
                            maxValue = 200,
                            canUserInput = false
                        ).build()
                    )
                    addView(
                        SettingSeekBar.FastBuilder(
                            mText = myRes.getString(R.string.DockSide),
                            mKey = "dockSide",
                            defValue = 30,
                            minValue = 0,
                            maxValue = 200,
                            canUserInput = false
                        ).build()
                    )
                    addView(
                        SettingSeekBar.FastBuilder(
                            mText = myRes.getString(R.string.DockBottom),
                            mKey = "dockBottom",
                            defValue = 23,
                            minValue = 0,
                            maxValue = 200,
                            canUserInput = false
                        ).build()
                    )
                    addView(
                        SettingSeekBar.FastBuilder(
                            mText = myRes.getString(R.string.DockIconBottom),
                            mKey = "dockIconBottom",
                            defValue = 35,
                            minValue = 0,
                            maxValue = 200,
                            canUserInput = false
                        ).build()
                    )
                })
            })
            setPositiveButton(myRes.getString(R.string.Save), null)
            dialogBuilder.setNeutralButton(myRes.getString(R.string.Reset)) { _, _ -> showModifyReset() }
            setCancelable(false)
        }.show()
    }


    private fun showModifyRoundCorner() {
        SettingUserInput(
            myRes.getString(R.string.RoundCorner),
            "recents_task_view_rounded_corners_radius",
            0,
            100,
            1,
            20
        ).build()
    }

    private fun showModifyTextSize() {
        SettingUserInput(
            myRes.getString(R.string.AppTextSize),
            "recents_task_view_header_height",
            0,
            200,
            1,
            40
        ).build()
    }

    private fun showModifyAnimationLevel() {
        SettingSeekBarDialog(
            myRes.getString(R.string.AnimationLevel),
            "animationLevel",
            5,
            500,
            canUserInput = true,
            defValue = 100
        ).build()
    }

    private fun showModifyBackgroundTextSize() {
        SettingUserInput(
            myRes.getString(R.string.TaskViewAppCardTextSize),
            "backgroundTextSize",
            0,
            100,
            1,
            13
        ).build()
    }

    private fun showModifyVertical() {
        SettingUserInput(
            myRes.getString(R.string.VerticalTaskViewOfAppCardSize),
            "task_vertical",
            50,
            150,
            100,
            100
        ).build()
    }

    private fun showModifyHorizontal() {
        SettingUserInput(
            myRes.getString(R.string.HorizontalTaskViewOfAppCardSize),
            "task_horizontal",
            100,
            1000,
            1000,
            544
        ).build()
    }

    private fun showModifyBlurLevel() {
        val dialogBuilder = SettingBaseDialog().get()
        val mKey = "blurLevel"
        lateinit var dialog: AlertDialog
        lateinit var onClick: View
        fun saveValue(value: String) {
            editor.putString(mKey, value)
            editor.apply()
        }
        dialogBuilder.setView(ScrollView(HomeContext.activity).apply {
            overScrollMode = 2
            addView(LinearLayout(HomeContext.activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp2px(HomeContext.context, 10f),
                    dp2px(HomeContext.context, 10f),
                    dp2px(HomeContext.context, 10f),
                    dp2px(HomeContext.context, 10f)
                )
                addView(
                    SettingTextView.FastBuilder(
                        mText = "「" + myRes.getString(R.string.TaskViewBlurLevel) + "」",
                        mSize = SettingTextView.text2Size,
                        mColor = "#0C84FF"
                    )
                        .build()
                )
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.CompleteBlur)) {
                    saveValue("COMPLETE")
                    onClick = it
                    dialog.dismiss()
                }.build())
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.TestBlur)) {
                    saveValue("TEST")
                    onClick = it
                    dialog.dismiss()
                }.build())
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.SimpleBlur)) {
                    saveValue("SIMPLE")
                    onClick = it
                    dialog.dismiss()
                }.build())
                addView(SettingTextView.FastBuilder(mText = myRes.getString(R.string.NoneBlur)) {
                    saveValue("NONE")
                    onClick = it
                    dialog.dismiss()
                }.build())
            })
        })
        dialog = dialogBuilder.show().apply {
            setOnDismissListener {
                try {
                    LogUtil.toast(myRes.getString(R.string.TaskViewBlurSetTo) + " : ${(onClick as TextView).text}")
                } catch (ignore: Exception) {
                }
            }
        }
    }

    fun dockHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (OwnSP.ownSP.getBoolean(
                "dockSettings", false
            )
        ) {
            val _DEVICE_CONFIG_CLASS = XposedHelpers.findClassIfExists(
                "com.miui.home.launcher.DeviceConfig",
                lpparam.classLoader
            )
            try {
                XposedHelpers.findAndHookMethod(
                    _DEVICE_CONFIG_CLASS,
                    "calcHotSeatsMarginTop",
                    Context::class.java,
                    Boolean::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.args[1] = false
                            super.beforeHookedMethod(param)
                        }
                    })
                // 图标区域底部边距
                XposedHelpers.findAndHookMethod(
                    _DEVICE_CONFIG_CLASS,
                    "calcHotSeatsMarginBottom",
                    Context::class.java,
                    Boolean::class.java,
                    Boolean::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result =
                                dip2px(
                                    (OwnSP.ownSP.getFloat("dockIconBottom", -1f) * 10).toInt()
                                )
                        }
                    })
                // 搜索框宽度
                XposedHelpers.findAndHookMethod(
                    _DEVICE_CONFIG_CLASS,
                    "calcSearchBarWidth",
                    Context::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val context = param.args[0] as Context
                            val deviceWidth = px2dip(
                                context.resources.displayMetrics.widthPixels
                            )
                            param.result =
                                dip2px(
                                    deviceWidth - (OwnSP.ownSP.getFloat(
                                        "dockSide",
                                        -1f
                                    ) * 10).toInt()
                                )
                        }
                    })
                // Dock底部边距
                XposedHelpers.findAndHookMethod(
                    _DEVICE_CONFIG_CLASS,
                    "calcSearchBarMarginBottom",
                    Context::class.java,
                    Boolean::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = dip2px(
                                (OwnSP.ownSP.getFloat("dockBottom", -1f) * 10).toInt()
                            )
                        }
                    })
                // 宽度变化量
                XposedHelpers.findAndHookMethod(
                    _DEVICE_CONFIG_CLASS,
                    "getSearchBarWidthDelta",
                    XC_MethodReplacement.returnConstant(0)
                )
            } catch (e: Exception) {
                XposedBridge.log("[MiHome] DockHook Error:" + e.message)
            }

            val _LAUNCHER_CLASS = XposedHelpers.findClassIfExists(
                "com.miui.home.launcher.Launcher",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                _LAUNCHER_CLASS,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        val SearchBarObject = XposedHelpers.callMethod(
                            param.thisObject,
                            "getSearchBar"
                        ) as FrameLayout
                        val SearchBarDesktop = SearchBarObject.getChildAt(0) as RelativeLayout
                        val SearchBarDrawer = SearchBarObject.getChildAt(1) as RelativeLayout
                        val SearchBarContainer = SearchBarObject.parent as FrameLayout
                        val SearchEdgeLayout = SearchBarContainer.parent as FrameLayout
                        // 重新给Searbar容器排序
                        SearchEdgeLayout.removeView(SearchBarContainer)
                        SearchEdgeLayout.addView(SearchBarContainer, 0)
                        // 清空搜索图标和小爱同学
                        SearchBarDesktop.removeAllViews()
                        // 修改高度
                        SearchBarObject.layoutParams.height = dip2px(
                            (OwnSP.ownSP.getFloat("dockHeight", -1f) * 10).toInt()
                        )
                        // 修改应用列表搜索框
                        val mAllAppViewField = _LAUNCHER_CLASS.getDeclaredField("mAppsView")
                        mAllAppViewField.isAccessible = true
                        val mAllAppView =
                            mAllAppViewField.get(param.thisObject) as RelativeLayout
                        val mAllAppSearchView =
                            mAllAppView.getChildAt(mAllAppView.childCount - 1) as FrameLayout
                        SearchBarObject.removeView(SearchBarDrawer)
                        mAllAppSearchView.addView(SearchBarDrawer)
                        SearchBarDrawer.bringToFront()
                        val layoutParams =
                            SearchBarDrawer.layoutParams as FrameLayout.LayoutParams
                        SearchBarDrawer.layoutParams.height = dip2px(45)
                        layoutParams.leftMargin = dip2px(15)
                        layoutParams.rightMargin = dip2px(15)
                        SearchBarDrawer.layoutParams = layoutParams
                    }
                })
        }
    }


    private fun showModifyReset() {
        val dialogBuilder = SettingBaseDialog().get()
        dialogBuilder.apply {
            setView(ScrollView(HomeContext.activity).apply {
                overScrollMode = 2
                addView(LinearLayout(HomeContext.activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f),
                        dp2px(HomeContext.context, 10f)
                    )
                    addView(
                        SettingTextView.FastBuilder(
                            mText = "「" + myRes.getString(R.string.Reset) + "」",
                            mColor = "#0C84FF",
                            mSize = SettingTextView.text2Size
                        ).build()
                    )
                    addView(
                        SettingTextView.FastBuilder(
                            mText = myRes.getString(R.string.Tips1)
                        ).build()
                    )
                })
            })
            setNeutralButton(myRes.getString(R.string.Yes)) { _, _ ->
                OwnSP.set("searchBarBlur", true)
                OwnSP.set("dockRadius", 2.5f)
                OwnSP.set("dockHeight", 8.4f)
                OwnSP.set("dockSide", 3.0f)
                OwnSP.set("dockBottom", 1.6f)
                OwnSP.set("dockIconBottom", 2.5f)
                thread {
                    LogUtil.toast(myRes.getString(R.string.Reboot2))
                    Thread.sleep(1000)
                    exitProcess(0)
                }
            }
            setPositiveButton(myRes.getString(R.string.Cancel), null)
            setCancelable(false)
        }
        dialogBuilder.show()
    }

    private fun firstUseDialog() {
        val dialogBuilder = SettingBaseDialog().get().apply {
            setTitle("「" + myRes.getString(R.string.Welcome) + "」")
            setMessage(myRes.getString(R.string.Tips))
            setOnDismissListener {
                OwnSP.set("blurLevel", "COMPLETE")
                OwnSP.set("smoothAnimation", true)
                OwnSP.set("searchBarBlur", true)
                OwnSP.set("animationLevel", 1.25f)
                OwnSP.set("isFirstUse", false)
                OwnSP.set("dockRadius", 2.5f)
                OwnSP.set("dockHeight", 8.4f)
                OwnSP.set("dockSide", 3.0f)
                OwnSP.set("dockBottom", 2.3f)
                OwnSP.set("dockIconBottom", 3.5f)
                thread {
                    LogUtil.toast(myRes.getString(R.string.Reboot2))
                    Thread.sleep(1000)
                    exitProcess(0)
                }
            }
            setPositiveButton(myRes.getString(R.string.Yes), null)
            setCancelable(false)
        }
        dialogBuilder.show()
    }
}