package com.sysui.batt.xposed.modules

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.sysui.batt.R
import com.sysui.batt.data.common.Const.SYSTEMUI_PACKAGE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_CIRCLE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_CUSTOM_LANDSCAPE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_CUSTOM_RLANDSCAPE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_DEFAULT
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_DEFAULT_LANDSCAPE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_DEFAULT_RLANDSCAPE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_DOTTED_CIRCLE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_FILLED_CIRCLE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYA
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYB
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYC
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYD
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYF
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYG
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYH
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYI
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYJ
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYK
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYL
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYM
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYN
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_BATTERYO
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_COLOROS
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_IOS_15
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_IOS_16
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_KIM
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_MIUI_PILL
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_ONEUI7
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_SMILEY
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_STYLE_A
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_LANDSCAPE_STYLE_B
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_PORTRAIT_AIROO
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_PORTRAIT_CAPSULE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_PORTRAIT_LORN
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_PORTRAIT_MX
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_PORTRAIT_ORIGAMI
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_RLANDSCAPE_COLOROS
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_RLANDSCAPE_STYLE_A
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_RLANDSCAPE_STYLE_B
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_BLEND_COLOR
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_CHARGING_COLOR
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_CHARGING_ICON_MARGIN_LEFT
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_CHARGING_ICON_MARGIN_RIGHT
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_CHARGING_ICON_STYLE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_CHARGING_ICON_SWITCH
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_CHARGING_ICON_WIDTH_HEIGHT
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_DIMENSION
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_FILL_ALPHA
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_FILL_COLOR
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_FILL_GRAD_COLOR
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_HEIGHT
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_HIDE_BATTERY
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_HIDE_PERCENTAGE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_INSIDE_PERCENTAGE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_LAYOUT_REVERSE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_MARGIN_BOTTOM
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_MARGIN_LEFT
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_MARGIN_RIGHT
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_MARGIN_TOP
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_PERIMETER_ALPHA
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_POWERSAVE_FILL_COLOR
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_POWERSAVE_INDICATOR_COLOR
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_RAINBOW_FILL_COLOR
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_STYLE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_SWAP_PERCENTAGE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_WIDTH
import com.sysui.batt.data.common.Preferences.ICONIFY_CHARGING_ICON_TAG
import com.sysui.batt.xposed.ModPack
import com.sysui.batt.xposed.modules.batterystyles.BatteryDrawable
import com.sysui.batt.xposed.modules.batterystyles.CircleBattery
import com.sysui.batt.xposed.modules.batterystyles.CircleFilledBattery
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBattery
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryA
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryB
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryC
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryColorOS
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryD
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryE
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryF
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryG
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryH
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryI
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryJ
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryK
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryKim
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryL
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryM
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryMIUIPill
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryN
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryO
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryOneUI7
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatterySmiley
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryStyleA
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryStyleB
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryiOS15
import com.sysui.batt.xposed.modules.batterystyles.LandscapeBatteryiOS16
import com.sysui.batt.xposed.modules.batterystyles.PortraitBatteryAiroo
import com.sysui.batt.xposed.modules.batterystyles.PortraitBatteryCapsule
import com.sysui.batt.xposed.modules.batterystyles.PortraitBatteryLorn
import com.sysui.batt.xposed.modules.batterystyles.PortraitBatteryMx
import com.sysui.batt.xposed.modules.batterystyles.PortraitBatteryOrigami
import com.sysui.batt.xposed.modules.batterystyles.RLandscapeBattery
import com.sysui.batt.xposed.modules.batterystyles.RLandscapeBatteryColorOS
import com.sysui.batt.xposed.modules.batterystyles.RLandscapeBatteryStyleA
import com.sysui.batt.xposed.modules.batterystyles.RLandscapeBatteryStyleB
import com.sysui.batt.xposed.modules.extras.utils.SettingsLibUtils
import com.sysui.batt.xposed.modules.extras.utils.ViewHelper.toPx
import com.sysui.batt.xposed.modules.extras.utils.toolkit.HookParam
import com.sysui.batt.xposed.modules.extras.utils.toolkit.MethodHook
import com.sysui.batt.xposed.modules.extras.utils.toolkit.XposedHook.Companion.findClass
import com.sysui.batt.xposed.modules.extras.utils.toolkit.callMethod
import com.sysui.batt.xposed.modules.extras.utils.toolkit.getBooleanField
import com.sysui.batt.xposed.modules.extras.utils.toolkit.getExtraField
import com.sysui.batt.xposed.modules.extras.utils.toolkit.getExtraFieldSilently
import com.sysui.batt.xposed.modules.extras.utils.toolkit.getField
import com.sysui.batt.xposed.modules.extras.utils.toolkit.getFieldSilently
import com.sysui.batt.xposed.modules.extras.utils.toolkit.hookConstructor
import com.sysui.batt.xposed.modules.extras.utils.toolkit.hookMethod
import com.sysui.batt.xposed.modules.extras.utils.toolkit.hookMethodMatchPattern
import com.sysui.batt.xposed.modules.extras.utils.toolkit.log
import com.sysui.batt.xposed.modules.extras.utils.toolkit.setExtraField
import com.sysui.batt.xposed.modules.extras.utils.toolkit.setField
import com.sysui.batt.xposed.modules.extras.utils.toolkit.setStaticIntField
import com.sysui.batt.xposed.utils.XPrefs.Xprefs
import kotlin.math.roundToInt

@Suppress("unused")
@SuppressLint("DiscouragedApi")
class BatteryStyleManager(context: Context) : ModPack(context) {

    private var defaultLandscapeBatteryEnabled = false
    private var frameColor = Color.WHITE
    private var batteryController: Any? = null
    private var batteryMeterViewParam: HookParam? = null
    private var mBatteryLayoutReverse = false
    private var mScaledPerimeterAlpha = false
    private var mScaledFillAlpha = false
    private var mRainbowFillColor = false
    private var mCustomBlendColor = false
    private var mCustomChargingColor = Color.BLACK
    private var mCustomFillColor = Color.BLACK
    private var mCustomFillGradColor = Color.BLACK
    private var mCustomPowerSaveColor = Color.BLACK
    private var mCustomPowerSaveFillColor = Color.BLACK
    private var mSwapPercentage = true
    private val mChargingIconView: ImageView? = null
    private var mChargingIconSwitch = false
    private var mChargingIconStyle = 0
    private var mChargingIconML = 1
    private var mChargingIconMR = 0
    private var mChargingIconWH = 14

    override fun updatePrefs(vararg key: String) {
        var batteryStyle: Int

        Xprefs.apply {
            batteryStyle =
                getString(CUSTOM_BATTERY_STYLE, "$BATTERY_STYLE_CIRCLE")?.toIntOrNull()
                    ?: BATTERY_STYLE_CIRCLE

            val hidePercentage = getBoolean(CUSTOM_BATTERY_HIDE_PERCENTAGE, false)
            val defaultInsidePercentage = batteryStyle in listOf(
                BATTERY_STYLE_LANDSCAPE_IOS_16,
                BATTERY_STYLE_LANDSCAPE_BATTERYL,
                BATTERY_STYLE_LANDSCAPE_BATTERYM,
                BATTERY_STYLE_LANDSCAPE_ONEUI7
            )
            val insidePercentage = defaultInsidePercentage ||
                    getBoolean(CUSTOM_BATTERY_INSIDE_PERCENTAGE, false)
            defaultLandscapeBatteryEnabled = batteryStyle in listOf(
                BATTERY_STYLE_DEFAULT_LANDSCAPE,
                BATTERY_STYLE_DEFAULT_RLANDSCAPE
            )
            customBatteryEnabled = batteryStyle !in listOf(
                BATTERY_STYLE_DEFAULT,
                BATTERY_STYLE_DEFAULT_LANDSCAPE,
                BATTERY_STYLE_DEFAULT_RLANDSCAPE
            )

            mBatteryRotation = if (defaultLandscapeBatteryEnabled) {
                if (batteryStyle == BATTERY_STYLE_DEFAULT_RLANDSCAPE) 90 else 270
            } else {
                0
            }

            mHidePercentage = hidePercentage || insidePercentage
            mShowPercentInside = insidePercentage && (defaultInsidePercentage || !hidePercentage)
            mHideBattery = getBoolean(CUSTOM_BATTERY_HIDE_BATTERY, false)
            mBatteryLayoutReverse = getBoolean(CUSTOM_BATTERY_LAYOUT_REVERSE, false)
            mBatteryCustomDimension = getBoolean(CUSTOM_BATTERY_DIMENSION, false)
            mBatteryScaleWidth = getSliderInt(CUSTOM_BATTERY_WIDTH, 20)
            mBatteryScaleHeight = getSliderInt(CUSTOM_BATTERY_HEIGHT, 20)
            mScaledPerimeterAlpha = getBoolean(CUSTOM_BATTERY_PERIMETER_ALPHA, false)
            mScaledFillAlpha = getBoolean(CUSTOM_BATTERY_FILL_ALPHA, false)
            mRainbowFillColor = getBoolean(CUSTOM_BATTERY_RAINBOW_FILL_COLOR, false)
            mCustomBlendColor = getBoolean(CUSTOM_BATTERY_BLEND_COLOR, false)
            mCustomChargingColor = getInt(CUSTOM_BATTERY_CHARGING_COLOR, Color.BLACK)
            mCustomFillColor = getInt(CUSTOM_BATTERY_FILL_COLOR, Color.BLACK)
            mCustomFillGradColor = getInt(CUSTOM_BATTERY_FILL_GRAD_COLOR, Color.BLACK)
            mCustomPowerSaveColor = getInt(CUSTOM_BATTERY_POWERSAVE_INDICATOR_COLOR, Color.BLACK)
            mCustomPowerSaveFillColor = getInt(CUSTOM_BATTERY_POWERSAVE_FILL_COLOR, Color.BLACK)
            mSwapPercentage = getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
            mChargingIconSwitch = getBoolean(CUSTOM_BATTERY_CHARGING_ICON_SWITCH, false)
            mChargingIconStyle =
                getString(CUSTOM_BATTERY_CHARGING_ICON_STYLE, "0")?.toIntOrNull() ?: 0
            mChargingIconML = getSliderInt(CUSTOM_BATTERY_CHARGING_ICON_MARGIN_LEFT, 1)
            mChargingIconMR = getSliderInt(CUSTOM_BATTERY_CHARGING_ICON_MARGIN_RIGHT, 0)
            mChargingIconWH = getSliderInt(CUSTOM_BATTERY_CHARGING_ICON_WIDTH_HEIGHT, 14)
            mBatteryMarginLeft = mContext.toPx(getSliderInt(CUSTOM_BATTERY_MARGIN_LEFT, 4))
            mBatteryMarginTop = mContext.toPx(getSliderInt(CUSTOM_BATTERY_MARGIN_TOP, 0))
            mBatteryMarginRight = mContext.toPx(getSliderInt(CUSTOM_BATTERY_MARGIN_RIGHT, 4))
            mBatteryMarginBottom = mContext.toPx(getSliderInt(CUSTOM_BATTERY_MARGIN_BOTTOM, 0))
        }

        val styleChanged = mBatteryStyle != batteryStyle
        mBatteryStyle = batteryStyle

        for (view in batteryViews.toList()) {
            val mBatteryIconView = view.getFieldSilently("mBatteryIconView") as? ImageView
            mBatteryIconView?.let {
                updateBatteryRotation(it)
                updateFlipper(it.parent)
            }

            val mBatteryPercentView = view.getFieldSilently("mBatteryPercentView") as? TextView
            mBatteryPercentView?.visibility = if (mHidePercentage) View.GONE else View.VISIBLE

            if (styleChanged) {
                val mCharging = view.isBatteryCharging()
                val mLevel = view.getFieldSilently("mLevel") as? Int ?: continue
                val mPowerSave =
                    batteryController?.getFieldSilently("mPowerSave") as? Boolean == true

                if (customBatteryEnabled) {
                    val mBatteryDrawable = getNewBatteryDrawable(mContext)

                    if (mBatteryDrawable != null) {
                        if (mBatteryIconView != null) {
                            mBatteryIconView.setImageDrawable(mBatteryDrawable)
                            mBatteryIconView.setVisibility(if (mHideBattery) View.GONE else View.VISIBLE)
                        }

                        view.setExtraField("mBatteryDrawable", mBatteryDrawable)

                        mBatteryDrawable.setBatteryLevel(mLevel)
                        mBatteryDrawable.setChargingEnabled(mCharging)
                        mBatteryDrawable.setPowerSavingEnabled(mPowerSave)

                        // A fresh drawable defaults to WHITE (the constructor's
                        // frameColor is unused), so replay this view's last
                        // SystemUI colors — otherwise the icon keeps the wrong
                        // color until the next theme-driven updateColors.
                        (view.getExtraFieldSilently("mLastBatteryColors") as? IntArray)
                            ?.takeIf { it.size == 3 }
                            ?.let { mBatteryDrawable.setColors(it[0], it[1], it[2]) }

                        updateCustomizeBatteryDrawable(mBatteryDrawable)
                    }
                }
            }
        }

        refreshBatteryIcons()

        when (key.firstOrNull()) {
            in setOf(
                CUSTOM_BATTERY_WIDTH,
                CUSTOM_BATTERY_HEIGHT
            ) -> {
                setDefaultBatteryDimens()
                // Dimension changes also affect the live views, not just resources.
                batteryMeterViewParam?.let {
                    updateSettings(it)
                }
            }

            in setOf(
                CUSTOM_BATTERY_STYLE,
                CUSTOM_BATTERY_HIDE_PERCENTAGE,
                CUSTOM_BATTERY_LAYOUT_REVERSE,
                CUSTOM_BATTERY_DIMENSION,
                CUSTOM_BATTERY_PERIMETER_ALPHA,
                CUSTOM_BATTERY_FILL_ALPHA,
                CUSTOM_BATTERY_RAINBOW_FILL_COLOR,
                CUSTOM_BATTERY_BLEND_COLOR,
                CUSTOM_BATTERY_CHARGING_COLOR,
                CUSTOM_BATTERY_FILL_COLOR,
                CUSTOM_BATTERY_FILL_GRAD_COLOR,
                CUSTOM_BATTERY_POWERSAVE_INDICATOR_COLOR,
                CUSTOM_BATTERY_POWERSAVE_FILL_COLOR,
                CUSTOM_BATTERY_SWAP_PERCENTAGE,
                CUSTOM_BATTERY_CHARGING_ICON_SWITCH,
                CUSTOM_BATTERY_CHARGING_ICON_STYLE,
                CUSTOM_BATTERY_CHARGING_ICON_MARGIN_LEFT,
                CUSTOM_BATTERY_CHARGING_ICON_MARGIN_RIGHT,
                CUSTOM_BATTERY_CHARGING_ICON_WIDTH_HEIGHT,
                CUSTOM_BATTERY_MARGIN_LEFT,
                CUSTOM_BATTERY_MARGIN_TOP,
                CUSTOM_BATTERY_MARGIN_RIGHT,
                CUSTOM_BATTERY_MARGIN_BOTTOM
            ) -> {
                batteryMeterViewParam?.let {
                    updateSettings(it)
                }
            }
        }
    }

    override fun handleLoadPackage(packageName: String, classLoader: ClassLoader) {
        val batteryControllerImplClass =
            findClass("$SYSTEMUI_PACKAGE.statusbar.policy.BatteryControllerImpl")
        val batteryMeterViewClass = findClass(
            "$SYSTEMUI_PACKAGE.battery.BatteryMeterView",
            "$SYSTEMUI_PACKAGE.BatteryMeterView"
        )

        batteryControllerImplClass
            .hookConstructor()
            .runAfter { param ->
                batteryController = param.thisObject
            }

        batteryControllerImplClass
            .hookMethod("fireBatteryUnknownStateChanged")
            .suppressError()
            .runAfter {
                if (!customBatteryEnabled) return@runAfter

                for (view in batteryViews.toList()) {
                    val mBatteryDrawable = view.getExtraFieldSilently(
                        "mBatteryDrawable"
                    ) as? BatteryDrawable ?: continue

                    val mBatteryIconView =
                        view.getFieldSilently("mBatteryIconView") as? ImageView
                    mBatteryIconView?.setImageDrawable(mBatteryDrawable)
                }
            }

        val batteryDataRefreshHook: MethodHook = object : MethodHook() {
            override fun afterHookedMethod(param: HookParam) {
                if (!customBatteryEnabled) return

                val mLevel = param.thisObject.getField("mLevel") as Int
                val mCharging = param.thisObject.isBatteryCharging()
                val mPowerSave = param.thisObject.getBooleanField("mPowerSave")

                refreshBatteryData(mLevel, mCharging, mPowerSave)
                // refreshing twice to avoid a bug where the battery icon updates incorrectly
                refreshBatteryData(mLevel, mCharging, mPowerSave)
            }
        }

        batteryControllerImplClass
            .hookMethodMatchPattern(".*fireBatteryLevelChanged.*")
            .run(batteryDataRefreshHook)

        batteryControllerImplClass
            .hookMethodMatchPattern(".*firePowerSaveChanged.*")
            .run(batteryDataRefreshHook)

        batteryControllerImplClass
            .hookMethod("onReceive")
            .run(batteryDataRefreshHook)

        val listener: OnAttachStateChangeListener = object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                batteryViews.add(v)

                Thread {
                    try {
                        if (batteryController != null) {
                            Thread.sleep(500)
                            batteryController.callMethod("fireBatteryLevelChanged")
                        }
                    } catch (ignored: Throwable) {
                    }
                }.start()
            }

            override fun onViewDetachedFromWindow(v: View) {
                batteryViews.remove(v)
            }
        }

        batteryMeterViewClass
            .hookConstructor()
            .parameters(
                Context::class.java,
                AttributeSet::class.java,
                Int::class.javaPrimitiveType
            )
            .runAfter { param ->
                if (batteryMeterViewParam == null) {
                    batteryMeterViewParam = param
                }

                val styleableBatteryMeterView = intArrayOf(
                    mContext.resources.getIdentifier(
                        "frameColor",
                        "attr",
                        mContext.packageName
                    ),
                    mContext.resources.getIdentifier(
                        "textAppearance",
                        "attr",
                        mContext.packageName
                    )
                )
                try {
                    val attrs = mContext.obtainStyledAttributes(
                        param.args[1] as AttributeSet,
                        styleableBatteryMeterView,
                        param.args[2] as Int,
                        0
                    )

                    // NOTE: obtainStyledAttributes is indexed positionally (0, 1),
                    // not by resource id. The previous code passed a styleable
                    // resource id as the index, which throws on most ROMs.
                    val fallbackColor = try {
                        val bgId = mContext.resources.getIdentifier(
                            "meter_background_color",
                            "color",
                            mContext.packageName
                        )
                        if (bgId != 0) mContext.getColor(bgId) else Color.WHITE
                    } catch (_: Throwable) {
                        Color.WHITE
                    }
                    frameColor = try {
                        attrs.getColor(0, fallbackColor)
                    } catch (_: Throwable) {
                        fallbackColor
                    }
                    attrs.recycle()
                } catch (throwable: Throwable) {
                    log(this@BatteryStyleManager, throwable)
                }

                (param.thisObject as View).addOnAttachStateChangeListener(listener)

                val mBatteryIconView = initBatteryIfNull(
                    param,
                    param.thisObject.getFieldSilently("mBatteryIconView") as? ImageView
                )

                if (customBatteryEnabled || mBatteryStyle == BATTERY_STYLE_DEFAULT_LANDSCAPE || mBatteryStyle == BATTERY_STYLE_DEFAULT_RLANDSCAPE) {
                    updateBatteryRotation(mBatteryIconView)
                    updateFlipper(mBatteryIconView.parent)
                }

                if (!customBatteryEnabled) return@runAfter

                val mBatteryDrawable = getNewBatteryDrawable(mContext)

                if (mBatteryDrawable != null) {
                    param.thisObject.setExtraField("mBatteryDrawable", mBatteryDrawable)

                    mBatteryIconView.setImageDrawable(mBatteryDrawable)

                    param.thisObject.setField(
                        "mBatteryIconView",
                        mBatteryIconView
                    )

                    mBatteryIconView.setVisibility(if (mHideBattery) View.GONE else View.VISIBLE)
                }

                updateChargingIconView(param.thisObject)
                updateSettings(param)

                batteryController?.callMethod("fireBatteryLevelChanged")
            }

        batteryMeterViewClass
            .hookMethod("updateColors")
            .parameters(
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            .runAfter { param ->
                if (batteryMeterViewParam == null) {
                    batteryMeterViewParam = param
                }

                // Remember the last colors pushed by SystemUI for this view so a
                // later style swap can replay them onto the fresh drawable.
                // Stored unconditionally (even when disabled) so switching from
                // stock to a custom style also picks up the current theme.
                try {
                    param.thisObject.setExtraField(
                        "mLastBatteryColors",
                        intArrayOf(
                            param.args[0] as Int,
                            param.args[1] as Int,
                            param.args[2] as Int
                        )
                    )
                } catch (_: Throwable) {
                }

                if (!customBatteryEnabled) return@runAfter

                val mBatteryDrawable = param.thisObject.getExtraFieldSilently(
                    "mBatteryDrawable"
                ) as? BatteryDrawable

                mBatteryDrawable?.setColors(
                    param.args[0] as Int,
                    param.args[1] as Int,
                    param.args[2] as Int
                )

                val mChargingIconView =
                    (param.thisObject as ViewGroup).findViewWithTag<ImageView>(
                        ICONIFY_CHARGING_ICON_TAG
                    )
                mChargingIconView?.setImageTintList(ColorStateList.valueOf(param.args[2] as Int))
            }

        val shadeHeaderControllerClass = findClass(
            "$SYSTEMUI_PACKAGE.shade.ShadeHeaderController",
            "$SYSTEMUI_PACKAGE.shade.LargeScreenShadeHeaderController"
        )

        shadeHeaderControllerClass
            .hookMethod("onInit")
            .runAfter { param ->
                try {
                    val configurationControllerListener = param.thisObject.getField(
                        "configurationControllerListener"
                    )

                    configurationControllerListener.javaClass
                        .hookMethod("onConfigChanged")
                        .runAfter {
                            if (customBatteryEnabled) {
                                updateBatteryResources(param)
                            }
                        }

                    if (customBatteryEnabled) {
                        updateBatteryResources(param)
                    }
                } catch (throwable: Throwable) {
                    log(this@BatteryStyleManager, throwable)
                }
            }

        if (customBatteryEnabled) {
            batteryMeterViewClass
                .hookMethod("scaleBatteryMeterViews")
                .replace { refreshBatteryIcons() }

            batteryMeterViewClass
                .hookMethod("scaleBatteryMeterViewsLegacy")
                .suppressError()
                .replace { refreshBatteryIcons() }
        }

        batteryMeterViewClass
            .hookMethod("setPercentShowMode")
            .runBefore { param ->
                if (batteryMeterViewParam == null) {
                    batteryMeterViewParam = param
                }

                if ((customBatteryEnabled || defaultLandscapeBatteryEnabled) && (mHidePercentage || mShowPercentInside)) {
                    param.result = 2
                }
            }

        batteryMeterViewClass
            .hookMethod("updateShowPercent")
            .runAfter { param ->
                if (batteryMeterViewParam == null) {
                    batteryMeterViewParam = param
                }

                val mBatteryPercentView = param.thisObject.getFieldSilently(
                    "mBatteryPercentView"
                ) as? TextView

                mBatteryPercentView?.visibility = if (mHidePercentage) View.GONE else View.VISIBLE
            }

        removeBatteryMeterViewMethods(batteryMeterViewClass)
        setDefaultBatteryDimens()
    }

    private fun refreshBatteryData(mLevel: Int, mCharging: Boolean, mPowerSave: Boolean) {
        for (view in batteryViews.toList()) {
            try {
                view.post {
                    val mBatteryDrawable = view.getExtraFieldSilently(
                        "mBatteryDrawable"
                    ) as? BatteryDrawable

                    mBatteryDrawable?.let {
                        it.setBatteryLevel(mLevel)
                        it.setChargingEnabled(mCharging)
                        it.setPowerSavingEnabled(mPowerSave)
                        updateCustomizeBatteryDrawable(it)
                    }

                    val mBatteryPercentView =
                        view.getFieldSilently("mBatteryPercentView") as? TextView
                    mBatteryPercentView?.visibility =
                        if (mHidePercentage) View.GONE else View.VISIBLE

                    scaleBatteryMeterViews(view)
                    updateChargingIconView(view, mCharging)
                }
            } catch (ignored: Throwable) {
            }
        }
    }

    private fun updateBatteryResources(param: HookParam) {
        try {
            val header = param.thisObject.getField("header") as View
            val textColorPrimary = SettingsLibUtils.getColorAttrDefaultColor(
                header.context,
                android.R.attr.textColorPrimary
            )
            val textColorPrimaryInverse = SettingsLibUtils.getColorAttrDefaultColor(
                header.context,
                android.R.attr.textColorPrimaryInverse
            )
            val textColorSecondary = SettingsLibUtils.getColorAttrDefaultColor(
                header.context,
                android.R.attr.textColorSecondary
            )
            val batteryIcon = try {
                param.thisObject.getField(
                    "batteryIcon"
                ) as LinearLayout
            } catch (throwable: Throwable) {
                param.thisObject.getField(
                    "batteryIcon"
                ) as FrameLayout
            }

            if (param.thisObject.getFieldSilently("iconManager") != null) {
                try {
                    param.thisObject
                        .getField("iconManager")
                        .callMethod("setTint", textColorPrimary)
                } catch (ignored: Throwable) {
                    param.thisObject
                        .getField("iconManager")
                        .callMethod("setTint", textColorPrimary, textColorPrimaryInverse)
                }
            }

            batteryIcon.callMethod(
                "updateColors",
                textColorPrimary,
                textColorSecondary,
                textColorPrimary
            )

            scaleBatteryMeterViews(batteryIcon)
        } catch (ignored: NoSuchFieldError) {
        } catch (throwable: Throwable) {
            log(this@BatteryStyleManager, throwable)
        }
    }

    private fun refreshBatteryIcons() {
        for (view in batteryViews.toList()) {
            val mBatteryIconView = view.getFieldSilently("mBatteryIconView") as? ImageView

            if (mBatteryIconView != null) {
                updateBatteryRotation(mBatteryIconView)
                updateFlipper(mBatteryIconView.parent)
            }

            val mBatteryPercentView = view.getFieldSilently("mBatteryPercentView") as? TextView?
            mBatteryPercentView?.visibility = if (mHidePercentage) View.GONE else View.VISIBLE

            if (customBatteryEnabled) {
                scaleBatteryMeterViews(mBatteryIconView)

                mBatteryIconView?.setVisibility(if (mHideBattery) View.GONE else View.VISIBLE)

                val mBatteryDrawable = view.getExtraFieldSilently(
                        "mBatteryDrawable"
                ) as? BatteryDrawable

                mBatteryDrawable?.let { batteryDrawable ->
                    batteryDrawable.setShowPercentEnabled(mShowPercentInside)
                    batteryDrawable.alpha = (BATTERY_ICON_OPACITY * 2.55f).roundToInt()
                    updateCustomizeBatteryDrawable(batteryDrawable)
                }
            }

            updateChargingIconView(view)
        }
    }

    private fun Any?.isBatteryCharging(): Boolean {
        val mCharging = getFieldSilently("mPluggedIn") as? Boolean == true
                || getFieldSilently("mCharging") as? Boolean == true
                || getFieldSilently("mWirelessCharging") as? Boolean == true
                || batteryMeterViewParam?.thisObject?.getFieldSilently("mPluggedIn") as? Boolean == true

        val mIsIncompatibleCharging =
            getFieldSilently("mIsIncompatibleCharging") as? Boolean == true

        return mCharging && !mIsIncompatibleCharging
    }

    private fun initBatteryIfNull(param: HookParam, batteryIconView: ImageView?): ImageView {
        var mBatteryIconView: ImageView? = batteryIconView

        if (mBatteryIconView == null) {
            mBatteryIconView = ImageView(mContext)
            try {
                mBatteryIconView.setImageDrawable(
                    param.thisObject.getField("mAccessorizedDrawable") as Drawable
                )
            } catch (throwable: Throwable) {
                try {
                    mBatteryIconView.setImageDrawable(
                        param.thisObject.getField("mThemedDrawable") as Drawable
                    )
                } catch (throwable1: Throwable) {
                    mBatteryIconView.setImageDrawable(
                        param.thisObject.getField("mDrawable") as Drawable
                    )
                }
            }

            val typedValue = TypedValue()
            mContext.resources.getValue(
                mContext.resources.getIdentifier(
                    "status_bar_icon_scale_factor",
                    "dimen",
                    mContext.packageName
                ), typedValue, true
            )

            val iconScaleFactor = typedValue.float
            val batteryWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mBatteryScaleWidth.toFloat(),
                mBatteryIconView.context.resources.displayMetrics
            ).toInt()
            val batteryHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mBatteryScaleHeight.toFloat(),
                mBatteryIconView.context.resources.displayMetrics
            ).toInt()
            val mlp = MarginLayoutParams(
                (batteryWidth * iconScaleFactor).toInt(),
                (batteryHeight * iconScaleFactor).toInt()
            )

            mlp.setMargins(
                0,
                0,
                0,
                mContext.resources.getDimensionPixelOffset(
                    mContext.resources.getIdentifier(
                        "battery_margin_bottom",
                        "dimen",
                        mContext.packageName
                    )
                )
            )

            param.thisObject.setField("mBatteryIconView", mBatteryIconView)
            param.thisObject.callMethod("addView", mBatteryIconView, mlp)

            mBatteryIconView.setVisibility(if (mHideBattery) View.GONE else View.VISIBLE)
        }

        return mBatteryIconView
    }

    private fun getNewBatteryDrawable(context: Context): BatteryDrawable? {
        val mBatteryDrawable = when (mBatteryStyle) {
            BATTERY_STYLE_CUSTOM_RLANDSCAPE -> RLandscapeBattery(context, frameColor)
            BATTERY_STYLE_CUSTOM_LANDSCAPE -> LandscapeBattery(context, frameColor)
            BATTERY_STYLE_PORTRAIT_CAPSULE -> PortraitBatteryCapsule(context, frameColor)
            BATTERY_STYLE_PORTRAIT_LORN -> PortraitBatteryLorn(context, frameColor)
            BATTERY_STYLE_PORTRAIT_MX -> PortraitBatteryMx(context, frameColor)
            BATTERY_STYLE_PORTRAIT_AIROO -> PortraitBatteryAiroo(context, frameColor)
            BATTERY_STYLE_RLANDSCAPE_STYLE_A -> RLandscapeBatteryStyleA(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_STYLE_A -> LandscapeBatteryStyleA(context, frameColor)
            BATTERY_STYLE_RLANDSCAPE_STYLE_B -> RLandscapeBatteryStyleB(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_STYLE_B -> LandscapeBatteryStyleB(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_IOS_15 -> LandscapeBatteryiOS15(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_IOS_16 -> LandscapeBatteryiOS16(context, frameColor)
            BATTERY_STYLE_PORTRAIT_ORIGAMI -> PortraitBatteryOrigami(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_SMILEY -> LandscapeBatterySmiley(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_MIUI_PILL -> LandscapeBatteryMIUIPill(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_COLOROS -> LandscapeBatteryColorOS(context, frameColor)
            BATTERY_STYLE_RLANDSCAPE_COLOROS -> RLandscapeBatteryColorOS(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYA -> LandscapeBatteryA(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYB -> LandscapeBatteryB(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYC -> LandscapeBatteryC(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYD -> LandscapeBatteryD(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYE -> LandscapeBatteryE(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYF -> LandscapeBatteryF(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYG -> LandscapeBatteryG(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYH -> LandscapeBatteryH(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYI -> LandscapeBatteryI(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYJ -> LandscapeBatteryJ(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYK -> LandscapeBatteryK(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYL -> LandscapeBatteryL(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYM -> LandscapeBatteryM(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYN -> LandscapeBatteryN(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_BATTERYO -> LandscapeBatteryO(context, frameColor)
            BATTERY_STYLE_CIRCLE,
            BATTERY_STYLE_DOTTED_CIRCLE -> CircleBattery(context, frameColor).apply {
                setMeterStyle(mBatteryStyle)
            }
            BATTERY_STYLE_FILLED_CIRCLE -> CircleFilledBattery(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_KIM -> LandscapeBatteryKim(context, frameColor)
            BATTERY_STYLE_LANDSCAPE_ONEUI7 -> LandscapeBatteryOneUI7(context, frameColor)
            else -> null
        }

        mBatteryDrawable?.apply {
            setShowPercentEnabled(mShowPercentInside)
            alpha = (BATTERY_ICON_OPACITY * 2.55f).roundToInt()
        }

        return mBatteryDrawable
    }

    private var dimensHooked = false

    private fun setDefaultBatteryDimens() {
        if (dimensHooked) return
        dimensHooked = true

        try {
            val res = mContext.resources
            val widthId = res.getIdentifier(
                "status_bar_battery_icon_width", "dimen", SYSTEMUI_PACKAGE
            )
            val heightId = res.getIdentifier(
                "status_bar_battery_icon_height", "dimen", SYSTEMUI_PACKAGE
            )
            val paddingId = res.getIdentifier(
                "signal_cluster_battery_padding", "dimen", SYSTEMUI_PACKAGE
            )
            if (widthId == 0 && heightId == 0 && paddingId == 0) return

            fun dimenPx(dp: Float): Float = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, res.displayMetrics
            )

            fun dimenPxInt(dp: Float): Int = dimenPx(dp).toInt()

            Resources::class.java
                .hookMethod("getDimension")
                .suppressError()
                .runBefore { param ->
                    val id = param.args.getOrNull(0) as? Int ?: return@runBefore
                    when (id) {
                        widthId -> if (defaultLandscapeBatteryEnabled) {
                            param.result = dimenPx(mBatteryScaleWidth.toFloat())
                        }
                        heightId -> if (defaultLandscapeBatteryEnabled) {
                            param.result = dimenPx(mBatteryScaleHeight.toFloat())
                        }
                        paddingId -> when {
                            defaultLandscapeBatteryEnabled -> param.result = dimenPx(4f)
                            customBatteryEnabled -> param.result = dimenPx(3f)
                        }
                    }
                }

            Resources::class.java
                .hookMethod("getDimensionPixelOffset", "getDimensionPixelSize")
                .suppressError()
                .runBefore { param ->
                    val id = param.args.getOrNull(0) as? Int ?: return@runBefore
                    when (id) {
                        widthId -> if (defaultLandscapeBatteryEnabled) {
                            param.result = dimenPxInt(mBatteryScaleWidth.toFloat())
                        }
                        heightId -> if (defaultLandscapeBatteryEnabled) {
                            param.result = dimenPxInt(mBatteryScaleHeight.toFloat())
                        }
                        paddingId -> when {
                            defaultLandscapeBatteryEnabled -> param.result = dimenPxInt(4f)
                            customBatteryEnabled -> param.result = dimenPxInt(3f)
                        }
                    }
                }
        } catch (t: Throwable) {
            log(this@BatteryStyleManager, t)
        }
    }

    private fun removeBatteryMeterViewMethods(batteryMeterViewClass: Class<*>?) {
        if (customBatteryEnabled) {
            batteryMeterViewClass
                .hookMethod(
                    "updateDrawable",
                    "updateBatteryStyle",
                    "updateSettings",
                    "updateVisibility"
                )
                .suppressError()
                .replace { }

            val batteryMeterViewExClass = findClass(
                "com.nothing.systemui.battery.BatteryMeterViewEx",
                suppressError = true
            )

            batteryMeterViewExClass?.let { batteryMeterViewEx ->
                batteryMeterViewEx
                    .hookMethod("refreshByBatteryStateEx")
                    .replace { }

                batteryMeterViewEx
                    .hookMethod("addBatteryImageView")
                    .replace { param ->
                        val context = param.args[0] as Context
                        val batteryMeterView = param.args[1] as ViewGroup
                        val batteryIconView = param.args[2] as ImageView
                        val batteryWidth = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            mBatteryScaleWidth.toFloat(),
                            context.resources.displayMetrics
                        ).toInt()
                        val batteryHeight = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            mBatteryScaleHeight.toFloat(),
                            context.resources.displayMetrics
                        ).toInt()

                        batteryMeterViewEx.setStaticIntField(
                            "sTempMax",
                            context.resources.getInteger(
                                context.resources.getIdentifier(
                                    "config_wire_charging_temp_max",
                                    "integer",
                                    context.packageName
                                )
                            )
                        )
                        batteryMeterViewEx.setStaticIntField(
                            "sVoltMax",
                            context.resources.getInteger(
                                context.resources.getIdentifier(
                                    "config_wire_charging_voltage_max",
                                    "integer",
                                    context.packageName
                                )
                            )
                        )

                        batteryMeterView.addView(
                            batteryIconView,
                            ViewGroup.LayoutParams(batteryWidth, batteryHeight)
                        )
                    }

                batteryMeterViewEx
                    .hookMethod("updateView")
                    .replace { refreshBatteryIcons() }
            }
        }
    }

    private fun updateChargingIconView(
        thisObject: Any,
        mCharging: Boolean = thisObject.isBatteryCharging()
    ) {
        val parent = thisObject as? ViewGroup ?: return
        var mChargingIconView =
            parent.findViewWithTag<ImageView>(ICONIFY_CHARGING_ICON_TAG)

        // The custom charging icon drawable was never assigned (always null),
        // so an enabled switch only injected an empty ImageView that shifted
        // the layout. Don't add anything unless the feature is enabled.
        if (!mChargingIconSwitch) {
            mChargingIconView?.visibility = View.GONE
            return
        }

        if (mChargingIconView == null) {
            mChargingIconView = ImageView(mContext)
            mChargingIconView.tag = ICONIFY_CHARGING_ICON_TAG
            parent.addView(mChargingIconView, 1)
        }

        val drawable: Drawable? = null

        if (drawable != null && drawable !== mChargingIconView.getDrawable()) {
            mChargingIconView.setImageDrawable(drawable)
        }

        val left: Int = mContext.toPx(mChargingIconML)
        val right: Int = mContext.toPx(mChargingIconMR)
        val size: Int = mContext.toPx(mChargingIconWH)
        val lp = if (thisObject is LinearLayout) {
            LinearLayout.LayoutParams(size, size)
        } else {
            FrameLayout.LayoutParams(size, size)
        }

        lp.setMargins(
            left,
            0,
            right,
            mContext.resources.getDimensionPixelSize(
                mContext.resources.getIdentifier(
                    "battery_margin_bottom",
                    "dimen",
                    mContext.packageName
                )
            )
        )

        mChargingIconView.apply {
            setLayoutParams(lp)
            setVisibility(if (mCharging && mChargingIconSwitch) View.VISIBLE else View.GONE)
        }
    }

    private fun updateSettings(param: HookParam) {
        val obj = param.thisObject ?: return
        updateCustomizeBatteryDrawable(obj)
        updateChargingIconView(obj)
        updateBatteryRotation(obj)
        updateFlipper(obj)
    }

    private fun updateFlipper(thisObject: Any) {
        val batteryView = if (thisObject is LinearLayout) {
            thisObject.orientation = LinearLayout.HORIZONTAL
            thisObject.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            thisObject
        } else {
            thisObject as View
        }

        batteryView.layoutDirection = if (mSwapPercentage) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
    }

    private fun updateBatteryRotation(thisObject: Any) {
        val mBatteryIconView = thisObject.getField("mBatteryIconView") as View
        updateBatteryRotation(mBatteryIconView)
    }

    private fun updateBatteryRotation(mBatteryIconView: View) {
        mBatteryIconView.rotation = if (!defaultLandscapeBatteryEnabled && mBatteryLayoutReverse) {
            180
        } else {
            mBatteryRotation
        }.toFloat()
    }

    private fun updateCustomizeBatteryDrawable(thisObject: Any) {
        if (!customBatteryEnabled) return

        val mBatteryDrawable = thisObject.getExtraFieldSilently("mBatteryDrawable") as? BatteryDrawable
            ?: return

        updateCustomizeBatteryDrawable(mBatteryDrawable)
    }

    private fun updateCustomizeBatteryDrawable(mBatteryDrawable: BatteryDrawable) {
        if (!customBatteryEnabled) return

        mBatteryDrawable.customizeBatteryDrawable(
            mBatteryLayoutReverse,
            mScaledPerimeterAlpha,
            mScaledFillAlpha,
            mCustomBlendColor,
            mRainbowFillColor,
            mCustomFillColor,
            mCustomFillGradColor,
            mCustomChargingColor,
            mCustomPowerSaveColor,
            mCustomPowerSaveFillColor,
            mChargingIconSwitch
        )
    }

    private fun scaleBatteryMeterViews(thisObject: Any?) {
        if (thisObject == null) return

        if (thisObject is ImageView) {
            scaleBatteryMeterViews(thisObject)
        } else {
            val mBatteryIconView = thisObject.getFieldSilently("mBatteryIconView") as? ImageView
            mBatteryIconView?.let { scaleBatteryMeterViews(it) }
        }
    }

    private fun scaleBatteryMeterViews(mBatteryIconView: ImageView) {
        try {
            val context = mBatteryIconView.context
            val res = context.resources
            val typedValue = TypedValue()

            res.getValue(
                res.getIdentifier(
                    "status_bar_icon_scale_factor",
                    "dimen",
                    context.packageName
                ), typedValue, true
            )

            val iconScaleFactor = typedValue.float
            val batteryWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mBatteryScaleWidth.toFloat(),
                mBatteryIconView.context.resources.displayMetrics
            ).toInt()
            val batteryHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                mBatteryScaleHeight.toFloat(),
                mBatteryIconView.context.resources.displayMetrics
            ).toInt()

            val scaledLayoutParams = when (val lp = mBatteryIconView.layoutParams) {
                is LinearLayout.LayoutParams -> lp
                is FrameLayout.LayoutParams -> lp
                is MarginLayoutParams -> lp
                else -> return
            }
            scaledLayoutParams.width = (batteryWidth * iconScaleFactor).toInt()
            scaledLayoutParams.height = (batteryHeight * iconScaleFactor).toInt()

            if (mBatteryCustomDimension) {
                scaledLayoutParams.setMargins(
                    mBatteryMarginLeft,
                    mBatteryMarginTop,
                    mBatteryMarginRight,
                    mBatteryMarginBottom
                )
            } else {
                scaledLayoutParams.setMargins(
                    0,
                    0,
                    0,
                    context.resources.getDimensionPixelOffset(
                        context.resources.getIdentifier(
                            "battery_margin_bottom",
                            "dimen",
                            context.packageName
                        )
                    )
                )
            }

            mBatteryIconView.setLayoutParams(scaledLayoutParams)
            mBatteryIconView.setVisibility(if (mHideBattery) View.GONE else View.VISIBLE)
        } catch (throwable: Throwable) {
            log(this@BatteryStyleManager, throwable)
        }
    }

    companion object {
        private val batteryViews = java.util.concurrent.CopyOnWriteArrayList<View>()
        private var mBatteryStyle = BATTERY_STYLE_CIRCLE
        private var mShowPercentInside = false
        private var mHidePercentage = false
        private var mHideBattery = false
        private var mBatteryRotation = 0
        private var customBatteryEnabled = true
        private var mBatteryScaleWidth = 20
        private var mBatteryScaleHeight = 20
        private var mBatteryCustomDimension = false
        private var mBatteryMarginLeft = 0
        private var mBatteryMarginTop = 0
        private var mBatteryMarginRight = 0
        private var mBatteryMarginBottom = 0
        private const val BATTERY_ICON_OPACITY = 100
    }
}