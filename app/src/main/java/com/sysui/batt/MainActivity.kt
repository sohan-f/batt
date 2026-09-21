package com.sysui.batt

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.text.method.LinkMovementMethod
import android.transition.TransitionManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_CIRCLE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_DOTTED_CIRCLE
import com.sysui.batt.data.common.Preferences.BATTERY_STYLE_FILLED_CIRCLE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_HEIGHT
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_STYLE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_SWAP_PERCENTAGE
import com.sysui.batt.data.common.Preferences.CUSTOM_BATTERY_WIDTH
import com.sysui.batt.data.config.RPrefs
import com.sysui.batt.databinding.ActivityMainBinding
import com.sysui.batt.utils.SystemUtils
import com.sysui.batt.xposed.modules.batterystyles.BatteryDrawable
import com.sysui.batt.xposed.modules.batterystyles.CircleBattery
import com.sysui.batt.xposed.modules.batterystyles.CircleFilledBattery
import com.sysui.batt.xposed.utils.HookCheck
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var statusBarBatteryDrawable: BatteryDrawable? = null
    private var heroBatteryDrawable: BatteryDrawable? = null
    private var currentBatteryLevel = 85
    private var isSimulatingCharging = false
    private var isLightStatusBarMode = false
    private var currentRingStyle = BATTERY_STYLE_CIRCLE
    private var glowPulseAnimator: ObjectAnimator? = null
    private var chargingButtonAnimator: ValueAnimator? = null
    private var levelAnimator: ValueAnimator? = null
    private val emphasized = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    private val emphasizedAccelerate = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

    companion object {
        private const val DEFAULT_BATTERY_SIZE_DP = 20
        private const val MIN_BATTERY_SIZE_DP = 12
        private const val MAX_BATTERY_SIZE_DP = 32
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()
        initDefaultPreferences()
        setupModuleStatus()
        setupBatteryPreview()
        setupRingGeometrySelector()
        setupOrderSelector()
        setupSizeController()
        setupActions()
        binding.textAboutDesc.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshModuleStatus()
            updatePreviewTime()
        }
    }

    override fun onDestroy() {
        glowPulseAnimator?.cancel()
        chargingButtonAnimator?.cancel()
        levelAnimator?.cancel()
        restartAnimator?.cancel()
        cardColorAnimators.values.forEach { it.cancel() }
        super.onDestroy()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            binding.nestedScrollView.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom + 84)
            binding.layoutBottomDock.updatePadding(left = bars.left + 16, right = bars.right + 16, bottom = bars.bottom + 12)
            insets
        }
    }

    private fun initDefaultPreferences() {
        if (!RPrefs.contains(CUSTOM_BATTERY_STYLE)) RPrefs.putString(CUSTOM_BATTERY_STYLE, "$BATTERY_STYLE_CIRCLE")
        if (!RPrefs.contains(CUSTOM_BATTERY_SWAP_PERCENTAGE)) RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
        if (!RPrefs.contains(CUSTOM_BATTERY_WIDTH)) RPrefs.putInt(CUSTOM_BATTERY_WIDTH, DEFAULT_BATTERY_SIZE_DP)
        if (!RPrefs.contains(CUSTOM_BATTERY_HEIGHT)) RPrefs.putInt(CUSTOM_BATTERY_HEIGHT, DEFAULT_BATTERY_SIZE_DP)
        currentRingStyle = RPrefs.getString(CUSTOM_BATTERY_STYLE, "$BATTERY_STYLE_CIRCLE")?.toIntOrNull() ?: BATTERY_STYLE_CIRCLE
    }

    private fun setupModuleStatus() {
        binding.pillModuleStatus.setOnClickListener {
            val active = binding.textModuleStatusBadge.text == getString(R.string.status_module_active)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.systemui_module_status)
                .setMessage(if (active) "Circle Battery LSPosed hook (API 102) is active and modifying SystemUI." else getString(R.string.status_module_desc))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        refreshModuleStatus()
    }

    private fun refreshModuleStatus() {
        HookCheck.isSystemUIHookActive(this) { active ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val primaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimaryContainer)
                val onPrimaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimaryContainer)
                val surfaceVariant = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceVariant)
                val onSurfaceVariant = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurfaceVariant)
                if (active) {
                    binding.pillModuleStatus.setCardBackgroundColor(primaryContainer)
                    binding.textModuleStatusBadge.setText(R.string.status_module_active)
                    binding.textModuleStatusBadge.setTextColor(onPrimaryContainer)
                    binding.imageModuleStatusDot.setImageResource(R.drawable.ic_shield_check)
                    binding.imageModuleStatusDot.setColorFilter(onPrimaryContainer)
                } else {
                    binding.pillModuleStatus.setCardBackgroundColor(surfaceVariant)
                    binding.textModuleStatusBadge.setText(R.string.status_module_inactive)
                    binding.textModuleStatusBadge.setTextColor(onSurfaceVariant)
                    binding.imageModuleStatusDot.setImageResource(R.drawable.ic_shield_alert)
                    binding.imageModuleStatusDot.setColorFilter(onSurfaceVariant)
                }
            }
        }
    }

    private fun updatePreviewTime() {
        binding.textPreviewTime.text = try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        } catch (_: Throwable) {
            "09:41"
        }
    }

    private fun createBatteryInstance(style: Int, fg: Int, bg: Int): BatteryDrawable {
        val d: BatteryDrawable = when (style) {
            BATTERY_STYLE_FILLED_CIRCLE -> CircleFilledBattery(this, fg)
            else -> CircleBattery(this, fg).apply {
                setMeterStyle(if (style == BATTERY_STYLE_DOTTED_CIRCLE) BATTERY_STYLE_DOTTED_CIRCLE else BATTERY_STYLE_CIRCLE)
            }
        }
        d.setColors(fg, bg, fg)
        d.setBatteryLevel(currentBatteryLevel)
        d.setChargingEnabled(isSimulatingCharging)
        return d
    }

    private fun getPreviewColors(): Pair<Int, Int> =
        if (isLightStatusBarMode) Pair(Color.parseColor("#1C1B1F"), Color.parseColor("#9AA0A6"))
        else Pair(Color.WHITE, Color.DKGRAY)

    private fun setupBatteryPreview() {
        updatePreviewTime()
        recreateBatteryDrawables()
        updatePreviewLayoutDirection(RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true))
        binding.sliderBatteryLevel.addOnChangeListener { _, value, _ -> updateBatteryLevel(value.toInt(), animate = false) }
        binding.sliderBatteryLevel.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(s: com.google.android.material.slider.Slider) = Unit
            override fun onStopTrackingTouch(s: com.google.android.material.slider.Slider) {
                bounceHeroDial()
                itHaptic(s)
            }
        })
        attachMorphThumb(binding.sliderBatteryLevel)
        binding.groupQuickLevels.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val v = when (checkedId) {
                R.id.chipLevel20 -> 20f
                R.id.chipLevel50 -> 50f
                R.id.chipLevel80 -> 80f
                R.id.chipLevel100 -> 100f
                else -> return@addOnButtonCheckedListener
            }
            jumpToLevel(v)
        }
        binding.btnToggleCharging.setOnClickListener {
            isSimulatingCharging = !isSimulatingCharging
            updateChargingState(isSimulatingCharging)
            itHaptic(it)
        }
        binding.btnToggleThemeMode.setOnClickListener {
            isLightStatusBarMode = !isLightStatusBarMode
            applyThemeMode(isLightStatusBarMode)
            itHaptic(it)
        }
    }

    private fun recreateBatteryDrawables() {
        val (fg, bg) = getPreviewColors()
        statusBarBatteryDrawable = createBatteryInstance(currentRingStyle, fg, bg)
        heroBatteryDrawable = createBatteryInstance(currentRingStyle, fg, bg)
        binding.imageStatusBarBattery.setImageDrawable(statusBarBatteryDrawable)
        binding.imageHeroBattery.setImageDrawable(heroBatteryDrawable)
    }

    private fun applyThemeMode(lightMode: Boolean) {
        TransitionManager.beginDelayedTransition(binding.layoutPhoneStage)
        val (fg, _) = getPreviewColors()
        if (lightMode) {
            binding.layoutPhoneStage.setBackgroundResource(R.drawable.bg_phone_stage_light)
            binding.textPreviewTime.setTextColor(fg)
            binding.textStatusBarPercent.setTextColor(fg)
            binding.textHeroPercent.setTextColor(fg)
            binding.imagePreviewWifi.setColorFilter(fg)
            binding.imagePreviewCell.setColorFilter(fg)
            binding.btnToggleThemeMode.setIconResource(R.drawable.ic_moon)
            binding.btnToggleThemeMode.iconTint = ColorStateList.valueOf(fg)
            binding.btnToggleThemeMode.strokeColor = ColorStateList.valueOf(Color.parseColor("#40000000"))
            binding.textSimulationLabel.setTextColor(Color.parseColor("#5A6070"))
        } else {
            binding.layoutPhoneStage.setBackgroundResource(R.drawable.bg_phone_stage)
            binding.textPreviewTime.setTextColor(Color.WHITE)
            binding.textStatusBarPercent.setTextColor(Color.WHITE)
            binding.textHeroPercent.setTextColor(Color.WHITE)
            binding.imagePreviewWifi.setColorFilter(Color.WHITE)
            binding.imagePreviewCell.setColorFilter(Color.WHITE)
            binding.btnToggleThemeMode.setIconResource(R.drawable.ic_sun)
            binding.btnToggleThemeMode.iconTint = ColorStateList.valueOf(Color.WHITE)
            binding.btnToggleThemeMode.strokeColor = ColorStateList.valueOf(Color.parseColor("#40FFFFFF"))
            binding.textSimulationLabel.setTextColor(Color.parseColor("#A0A0A0"))
        }
        recreateBatteryDrawables()
        updateChargingState(isSimulatingCharging, animate = false)
    }

    private fun updateBatteryLevel(level: Int, animate: Boolean = true) {
        if (animate) {
            levelAnimator?.cancel()
            val from = currentBatteryLevel
            currentBatteryLevel = level
            levelAnimator = ValueAnimator.ofInt(from, level).apply {
                duration = 350
                interpolator = emphasized
                addUpdateListener { a ->
                    val v = a.animatedValue as Int
                    statusBarBatteryDrawable?.setBatteryLevel(v)
                    heroBatteryDrawable?.setBatteryLevel(v)
                    binding.textStatusBarPercent.text = "$v%"
                    binding.textHeroPercent.text = "$v%"
                    binding.textScrubLevelBadge.text = "$v%"
                    binding.imageSliderBolt.visibility = if (v > 0) View.VISIBLE else View.GONE
                }
                start()
            }
        } else {
            currentBatteryLevel = level
            statusBarBatteryDrawable?.setBatteryLevel(level)
            heroBatteryDrawable?.setBatteryLevel(level)
            binding.textStatusBarPercent.text = "$level%"
            binding.textHeroPercent.text = "$level%"
            binding.textScrubLevelBadge.text = "$level%"
            binding.imageSliderBolt.visibility = if (level > 0) View.VISIBLE else View.GONE
        }
    }

    private fun jumpToLevel(value: Float) {
        binding.sliderBatteryLevel.value = value
        updateBatteryLevel(value.toInt())
        bounceHeroDial()
    }

    private fun bounceHeroDial() {
        binding.imageHeroBattery.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).setInterpolator(emphasized)
            .withEndAction {
                binding.imageHeroBattery.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(emphasized).start()
            }.start()
    }

    private fun updateChargingState(charging: Boolean, animate: Boolean = true) {
        statusBarBatteryDrawable?.setChargingEnabled(charging)
        heroBatteryDrawable?.setChargingEnabled(charging)
        val (fg, _) = getPreviewColors()
        val amber = Color.parseColor("#FFD54F")
        val amberFill = Color.parseColor("#33FFB300")
        val baseStroke = if (isLightStatusBarMode) Color.parseColor("#40000000") else Color.parseColor("#40FFFFFF")
        val activeStroke = Color.parseColor("#66FFB300")
        binding.btnToggleCharging.setText(if (charging) R.string.preview_stop_simulation else R.string.preview_simulate_charging)
        if (charging) startGlowPulseAnimation() else stopGlowPulseAnimation()
        chargingButtonAnimator?.cancel()
        if (!animate) {
            applyChargingButtonColors(if (charging) 1f else 0f, fg, amber, amberFill, baseStroke, activeStroke)
        } else {
            chargingButtonAnimator = ValueAnimator.ofFloat(if (charging) 0f else 1f, if (charging) 1f else 0f).apply {
                duration = 250
                interpolator = emphasized
                addUpdateListener { a ->
                    applyChargingButtonColors(a.animatedValue as Float, fg, amber, amberFill, baseStroke, activeStroke)
                }
                start()
            }
        }
        binding.imageHeroBattery.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).setInterpolator(emphasizedAccelerate)
            .withEndAction {
                binding.imageHeroBattery.animate().scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(emphasized).start()
            }.start()
    }

    private fun applyChargingButtonColors(f: Float, fg: Int, amber: Int, amberFill: Int, baseStroke: Int, activeStroke: Int) {
        binding.btnToggleCharging.backgroundTintList =
            ColorStateList.valueOf(ColorUtils.blendARGB(Color.TRANSPARENT, amberFill, f))
        val tc = ColorUtils.blendARGB(fg, amber, f)
        binding.btnToggleCharging.setTextColor(tc)
        binding.btnToggleCharging.iconTint = ColorStateList.valueOf(tc)
        binding.btnToggleCharging.strokeColor =
            ColorStateList.valueOf(ColorUtils.blendARGB(baseStroke, activeStroke, f))
    }

    private fun startGlowPulseAnimation() {
        glowPulseAnimator?.cancel()
        glowPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.viewHeroGlow,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.25f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.25f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.4f, 0.9f)
        ).apply {
            duration = 1400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = emphasized
            start()
        }
    }

    private fun stopGlowPulseAnimation() {
        glowPulseAnimator?.cancel()
        glowPulseAnimator = null
        binding.viewHeroGlow.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(250).setInterpolator(emphasized).start()
    }

    private fun setupRingGeometrySelector() {
        updateRingGeometryVisuals(currentRingStyle, animate = false)
        binding.cardStyleRing.setOnClickListener {
            if (currentRingStyle != BATTERY_STYLE_CIRCLE) {
                currentRingStyle = BATTERY_STYLE_CIRCLE
                applyRingStyle(currentRingStyle)
                itHaptic(it)
            }
        }
        binding.cardStyleDotted.setOnClickListener {
            if (currentRingStyle != BATTERY_STYLE_DOTTED_CIRCLE) {
                currentRingStyle = BATTERY_STYLE_DOTTED_CIRCLE
                applyRingStyle(currentRingStyle)
                itHaptic(it)
            }
        }
        binding.cardStyleFilled.setOnClickListener {
            if (currentRingStyle != BATTERY_STYLE_FILLED_CIRCLE) {
                currentRingStyle = BATTERY_STYLE_FILLED_CIRCLE
                applyRingStyle(currentRingStyle)
                itHaptic(it)
            }
        }
    }

    private fun applyRingStyle(style: Int) {
        RPrefs.putString(CUSTOM_BATTERY_STYLE, "$style")
        updateRingGeometryVisuals(style)
        recreateBatteryDrawables()
        binding.imageHeroBattery.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).setInterpolator(emphasizedAccelerate)
            .withEndAction {
                binding.imageHeroBattery.animate().scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(emphasized).start()
            }.start()
    }

    private val cardColorAnimators = mutableMapOf<View, ValueAnimator>()

    private fun updateRingGeometryVisuals(selectedStyle: Int, animate: Boolean = true) {
        val primaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimaryContainer)
        val onPrimaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimaryContainer)
        val primaryColor = MaterialColors.getColor(binding.root, android.R.attr.colorPrimary)
        val surfaceLow = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerLow)
        val onSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
        val outlineVariant = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOutlineVariant)
        val textSecondary = MaterialColors.getColor(binding.root, android.R.attr.textColorSecondary)
        val density = resources.displayMetrics.density
        fun paint(
            card: com.google.android.material.card.MaterialCardView,
            icon: android.widget.ImageView,
            title: android.widget.TextView,
            badge: android.widget.ImageView,
            selected: Boolean,
        ) {
            val targetBg = if (selected) primaryContainer else surfaceLow
            val targetStroke = if (selected) primaryColor else outlineVariant
            if (animate) {
                cardColorAnimators[card]?.cancel()
                val fromBg = card.cardBackgroundColor?.defaultColor ?: targetBg
                val fromStroke = card.strokeColor
                cardColorAnimators[card] = ValueAnimator.ofArgb(fromBg, targetBg).apply {
                    duration = 250
                    interpolator = emphasized
                    addUpdateListener { a -> card.setCardBackgroundColor(a.animatedValue as Int) }
                    start()
                }
                ValueAnimator.ofArgb(fromStroke, targetStroke).apply {
                    duration = 250
                    interpolator = emphasized
                    addUpdateListener { a -> card.strokeColor = a.animatedValue as Int }
                    start()
                }
            } else {
                card.setCardBackgroundColor(targetBg)
                card.strokeColor = targetStroke
            }
            card.strokeWidth = if (selected) (2 * density).toInt() else (1 * density).toInt()
            icon.setColorFilter(if (selected) primaryColor else textSecondary)
            title.setTextColor(if (selected) onPrimaryContainer else onSurface)
            card.contentDescription = "${title.text}, ${if (selected) "selected" else "not selected"}"
            if (selected) {
                if (badge.visibility != View.VISIBLE) {
                    badge.visibility = View.VISIBLE
                    if (animate) {
                        badge.scaleX = 0.4f
                        badge.scaleY = 0.4f
                        badge.alpha = 0f
                        badge.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(emphasized).start()
                    } else {
                        badge.scaleX = 1f
                        badge.scaleY = 1f
                        badge.alpha = 1f
                    }
                }
            } else {
                badge.visibility = View.GONE
            }
        }
        paint(binding.cardStyleRing, binding.iconStyleRing, binding.textStyleRingTitle, binding.checkStyleRing, selectedStyle == BATTERY_STYLE_CIRCLE)
        paint(binding.cardStyleDotted, binding.iconStyleDotted, binding.textStyleDottedTitle, binding.checkStyleDotted, selectedStyle == BATTERY_STYLE_DOTTED_CIRCLE)
        paint(binding.cardStyleFilled, binding.iconStyleFilled, binding.textStyleFilledTitle, binding.checkStyleFilled, selectedStyle == BATTERY_STYLE_FILLED_CIRCLE)
    }

    private fun setupOrderSelector() {
        val isSwapped = RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
        updateOrderCardVisuals(isSwapped)
        binding.cardOrderPercentFirst.setOnClickListener {
            if (!RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)) {
                RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
                TransitionManager.beginDelayedTransition(binding.layoutPhoneStage)
                updatePreviewLayoutDirection(true)
                updateOrderCardVisuals(true)
                itHaptic(it)
                Snackbar.make(binding.root, R.string.layout_percent_first_msg, Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.cardOrderIconFirst.setOnClickListener {
            if (RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)) {
                RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, false)
                TransitionManager.beginDelayedTransition(binding.layoutPhoneStage)
                updatePreviewLayoutDirection(false)
                updateOrderCardVisuals(false)
                itHaptic(it)
                Snackbar.make(binding.root, R.string.layout_icon_first_msg, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateOrderCardVisuals(percentFirst: Boolean) {
        val primaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimaryContainer)
        val onPrimaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimaryContainer)
        val primaryColor = MaterialColors.getColor(binding.root, android.R.attr.colorPrimary)
        val surfaceLow = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerLow)
        val onSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
        val outlineVariant = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOutlineVariant)
        val textSecondary = MaterialColors.getColor(binding.root, android.R.attr.textColorSecondary)
        val density = resources.displayMetrics.density
        if (percentFirst) {
            binding.cardOrderPercentFirst.setCardBackgroundColor(primaryContainer)
            binding.cardOrderPercentFirst.strokeColor = primaryColor
            binding.cardOrderPercentFirst.strokeWidth = (2 * density).toInt()
            binding.checkPercentFirst.visibility = View.VISIBLE
            binding.textPercentFirstTitle.setTextColor(onPrimaryContainer)
            binding.textPercentFirstDesc.setTextColor(onPrimaryContainer)
            binding.cardOrderIconFirst.setCardBackgroundColor(surfaceLow)
            binding.cardOrderIconFirst.strokeColor = outlineVariant
            binding.cardOrderIconFirst.strokeWidth = (1 * density).toInt()
            binding.checkIconFirst.visibility = View.INVISIBLE
            binding.textIconFirstTitle.setTextColor(onSurface)
            binding.textIconFirstDesc.setTextColor(textSecondary)
        } else {
            binding.cardOrderPercentFirst.setCardBackgroundColor(surfaceLow)
            binding.cardOrderPercentFirst.strokeColor = outlineVariant
            binding.cardOrderPercentFirst.strokeWidth = (1 * density).toInt()
            binding.checkPercentFirst.visibility = View.INVISIBLE
            binding.textPercentFirstTitle.setTextColor(onSurface)
            binding.textPercentFirstDesc.setTextColor(textSecondary)
            binding.cardOrderIconFirst.setCardBackgroundColor(primaryContainer)
            binding.cardOrderIconFirst.strokeColor = primaryColor
            binding.cardOrderIconFirst.strokeWidth = (2 * density).toInt()
            binding.checkIconFirst.visibility = View.VISIBLE
            binding.textIconFirstTitle.setTextColor(onPrimaryContainer)
            binding.textIconFirstDesc.setTextColor(onPrimaryContainer)
        }
    }

    private fun setupSizeController() {
        val sizeDp = RPrefs.getSliderInt(CUSTOM_BATTERY_WIDTH, DEFAULT_BATTERY_SIZE_DP).coerceIn(MIN_BATTERY_SIZE_DP, MAX_BATTERY_SIZE_DP)
        binding.groupSizePresets.addOnButtonCheckedListener { group, checkedId, isChecked ->
            val btn = group.findViewById<View>(checkedId)
            if (!isChecked) {
                // Tapping the active preset keeps it selected; programmatic
                // clearChecked() (slider moved off-preset) stays cleared.
                if (btn?.isPressed == true) group.check(checkedId)
                return@addOnButtonCheckedListener
            }
            if (btn?.isPressed != true) return@addOnButtonCheckedListener
            val preset = when (checkedId) {
                R.id.btnPreset16 -> 16
                R.id.btnPreset20 -> 20
                R.id.btnPreset24 -> 24
                R.id.btnPreset28 -> 28
                else -> return@addOnButtonCheckedListener
            }
            setPresetSize(preset)
            itHaptic(btn)
        }
        binding.sliderBatterySize.value = sizeDp.toFloat()
        updateSizeDisplay(sizeDp)
        syncPresetChecked(sizeDp)
        binding.sliderBatterySize.addOnChangeListener { _, value, fromUser ->
            val s = value.toInt()
            updateSizeDisplay(s)
            if (fromUser) applyBatterySize(s)
            syncPresetChecked(s)
        }
        attachMorphThumb(binding.sliderBatterySize)
        binding.btnSizeMinus.setOnClickListener {
            val c = binding.sliderBatterySize.value.toInt()
            if (c > MIN_BATTERY_SIZE_DP) {
                binding.sliderBatterySize.value = (c - 1).toFloat()
                applyBatterySize(c - 1)
                itHaptic(it)
            }
        }
        binding.btnSizePlus.setOnClickListener {
            val c = binding.sliderBatterySize.value.toInt()
            if (c < MAX_BATTERY_SIZE_DP) {
                binding.sliderBatterySize.value = (c + 1).toFloat()
                applyBatterySize(c + 1)
                itHaptic(it)
            }
        }
    }

    private fun syncPresetChecked(sizeDp: Int) {
        val checkedId = when (sizeDp) {
            16 -> R.id.btnPreset16
            20 -> R.id.btnPreset20
            24 -> R.id.btnPreset24
            28 -> R.id.btnPreset28
            else -> View.NO_ID
        }
        if (checkedId == View.NO_ID) {
            binding.groupSizePresets.clearChecked()
        } else if (binding.groupSizePresets.checkedButtonId != checkedId) {
            binding.groupSizePresets.check(checkedId)
        }
    }

    private fun setPresetSize(sizeDp: Int) {
        binding.sliderBatterySize.value = sizeDp.toFloat()
        applyBatterySize(sizeDp)
    }

    private fun applyBatterySize(sizeDp: Int) {
        RPrefs.putInt(CUSTOM_BATTERY_WIDTH, sizeDp)
        RPrefs.putInt(CUSTOM_BATTERY_HEIGHT, sizeDp)
        updateSizeDisplay(sizeDp)
    }

    private fun updateSizeDisplay(sizeDp: Int) {
        binding.textSizeValueDisplay.text = "$sizeDp dp"
        binding.imageSliderSize.visibility =
            if (sizeDp > MIN_BATTERY_SIZE_DP) View.VISIBLE else View.GONE
        val px = (sizeDp * resources.displayMetrics.density).toInt()
        binding.imageStatusBarBattery.layoutParams = binding.imageStatusBarBattery.layoutParams.apply {
            width = px
            height = px
        }
        binding.textStatusBarPercent.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, (sizeDp * 0.7f).coerceIn(9f, 22f))
    }

    private fun updatePreviewLayoutDirection(swapped: Boolean) {
        binding.layoutStatusBarBattery.layoutDirection = if (swapped) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

    private fun setupActions() {
        binding.btnRestartSystemUI.setOnClickListener { v ->
            if (!v.isEnabled) return@setOnClickListener
            itHaptic(v)
            playRestartAnimation(v)
            try {
                SystemUtils.restartSystemUI()
                Toast.makeText(this, R.string.restart_sysui_success, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
                Toast.makeText(this, R.string.restart_sysui_no_root, Toast.LENGTH_LONG).show()
            }
        }
    }

    private var restartAnimator: ObjectAnimator? = null

    private fun playRestartAnimation(v: View) {
        val btn = binding.btnRestartSystemUI
        restartAnimator?.cancel()
        // Loading state: lock double-tap, swap to "Restarting…" without new res.
        btn.isEnabled = false
        val origText = btn.text.toString()
        btn.text = "$origText…"
        // Expressive press: compress, then spring back + subtle icon wobble.
        // Icon-only spin isn't possible on MaterialButton (icon is a drawable,
        // not a view), so we wobble the button ±10° instead of a full 360° spin
        // that makes the label unreadable.
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(110).setInterpolator(emphasizedAccelerate)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(emphasized).start()
                restartAnimator = ObjectAnimator.ofFloat(v, View.ROTATION, 0f, -10f, 10f, 0f).apply {
                    duration = 450
                    interpolator = emphasized
                    start()
                }
                v.postDelayed({
                    btn.text = origText
                    btn.isEnabled = true
                }, 1400)
            }.start()
    }

    private fun itHaptic(v: View) {
        try {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Throwable) {
        }
    }

    // Dynamic physics morphing thumb: press squeezes the round handle into a
    // wide pill (160ms), release springs back to round with overshoot (420ms).
    // The track gap reduction around the handle is handled by the framework
    // via thumbTrackGapSize animating to zero on touch.
    private fun attachMorphThumb(slider: com.google.android.material.slider.Slider) {
        try {
            (getDrawable(R.drawable.thumb_morph_vector)?.mutate())?.let {
                slider.setCustomThumbDrawable(it)
            }
        } catch (_: Throwable) {
        }
        slider.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(s: com.google.android.material.slider.Slider) {
                morphThumb(s, R.drawable.avd_thumb_press)
            }

            override fun onStopTrackingTouch(s: com.google.android.material.slider.Slider) {
                morphThumb(s, R.drawable.avd_thumb_release)
            }
        })
    }

    private fun morphThumb(slider: com.google.android.material.slider.Slider, avdRes: Int) {
        try {
            val avd = getDrawable(avdRes)?.mutate() as? AnimatedVectorDrawable ?: return
            avd.callback = object : Drawable.Callback {
                override fun invalidateDrawable(who: Drawable) {
                    slider.invalidate()
                }

                override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                    slider.postDelayed(what, (`when` - SystemClock.uptimeMillis()).coerceAtLeast(0))
                }

                override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                    slider.removeCallbacks(what)
                }
            }
            slider.setCustomThumbDrawable(avd)
            avd.start()
        } catch (_: Throwable) {
        }
    }
}
