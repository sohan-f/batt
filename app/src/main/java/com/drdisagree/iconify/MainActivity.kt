package com.drdisagree.iconify

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.transition.TransitionManager
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_CIRCLE
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_DOTTED_CIRCLE
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_FILLED_CIRCLE
import com.drdisagree.iconify.data.common.Preferences.CUSTOM_BATTERY_HEIGHT
import com.drdisagree.iconify.data.common.Preferences.CUSTOM_BATTERY_STYLE
import com.drdisagree.iconify.data.common.Preferences.CUSTOM_BATTERY_SWAP_PERCENTAGE
import com.drdisagree.iconify.data.common.Preferences.CUSTOM_BATTERY_WIDTH
import com.drdisagree.iconify.data.config.RPrefs
import com.drdisagree.iconify.databinding.ActivityMainBinding
import com.drdisagree.iconify.utils.SystemUtils
import com.drdisagree.iconify.xposed.modules.batterystyles.BatteryDrawable
import com.drdisagree.iconify.xposed.modules.batterystyles.CircleBattery
import com.drdisagree.iconify.xposed.modules.batterystyles.CircleFilledBattery
import com.drdisagree.iconify.xposed.utils.HookCheck
import com.google.android.material.button.MaterialButton
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

    private var currentBatteryLevel: Int = 85
    private var isSimulatingCharging: Boolean = false
    private var isLightStatusBarMode: Boolean = false
    private var currentRingStyle: Int = BATTERY_STYLE_CIRCLE

    private var glowPulseAnimator: ObjectAnimator? = null

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
        setupAbout()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshModuleStatus()
            updatePreviewTime()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        glowPulseAnimator?.cancel()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right
            )
            binding.nestedScrollView.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom + 84
            )
            binding.layoutBottomDock.updatePadding(
                left = systemBars.left + 16,
                right = systemBars.right + 16,
                bottom = systemBars.bottom + 12
            )
            insets
        }
    }

    private fun initDefaultPreferences() {
        if (!RPrefs.contains(CUSTOM_BATTERY_STYLE)) {
            RPrefs.putString(CUSTOM_BATTERY_STYLE, "$BATTERY_STYLE_CIRCLE")
        }
        if (!RPrefs.contains(CUSTOM_BATTERY_SWAP_PERCENTAGE)) {
            RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
        }
        if (!RPrefs.contains(CUSTOM_BATTERY_WIDTH)) {
            RPrefs.putInt(CUSTOM_BATTERY_WIDTH, DEFAULT_BATTERY_SIZE_DP)
        }
        if (!RPrefs.contains(CUSTOM_BATTERY_HEIGHT)) {
            RPrefs.putInt(CUSTOM_BATTERY_HEIGHT, DEFAULT_BATTERY_SIZE_DP)
        }
        currentRingStyle = RPrefs.getString(CUSTOM_BATTERY_STYLE, "$BATTERY_STYLE_CIRCLE")
            ?.toIntOrNull() ?: BATTERY_STYLE_CIRCLE
    }

    private fun setupModuleStatus() {
        binding.pillModuleStatus.setOnClickListener {
            val isActive = binding.textModuleStatusBadge.text == getString(R.string.status_module_active)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.systemui_module_status)
                .setMessage(
                    if (isActive)
                        "Circle Battery LSPosed hook is active and modifying SystemUI."
                    else
                        getString(R.string.status_module_desc)
                )
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        refreshModuleStatus()
    }

    private fun refreshModuleStatus() {
        HookCheck.isSystemUIHookActive(this) { active ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                val primaryContainer = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorPrimaryContainer
                )
                val onPrimaryContainer = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnPrimaryContainer
                )
                val surfaceVariant = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorSurfaceVariant
                )
                val onSurfaceVariant = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )

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
        try {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.textPreviewTime.text = formatter.format(Date())
        } catch (_: Throwable) {
            binding.textPreviewTime.text = "09:41"
        }
    }

    private fun createBatteryInstance(style: Int, fg: Int, bg: Int): BatteryDrawable {
        return when (style) {
            BATTERY_STYLE_FILLED_CIRCLE -> {
                CircleFilledBattery(this, fg).apply {
                    setColors(fg, bg, fg)
                    setBatteryLevel(currentBatteryLevel)
                    setChargingEnabled(isSimulatingCharging)
                }
            }
            BATTERY_STYLE_DOTTED_CIRCLE -> {
                CircleBattery(this, fg).apply {
                    setMeterStyle(BATTERY_STYLE_DOTTED_CIRCLE)
                    setColors(fg, bg, fg)
                    setBatteryLevel(currentBatteryLevel)
                    setChargingEnabled(isSimulatingCharging)
                }
            }
            else -> {
                CircleBattery(this, fg).apply {
                    setMeterStyle(BATTERY_STYLE_CIRCLE)
                    setColors(fg, bg, fg)
                    setBatteryLevel(currentBatteryLevel)
                    setChargingEnabled(isSimulatingCharging)
                }
            }
        }
    }

    private fun getPreviewColors(): Pair<Int, Int> {
        return if (isLightStatusBarMode) {
            Pair(Color.parseColor("#1C1B1F"), Color.parseColor("#9AA0A6"))
        } else {
            Pair(Color.WHITE, Color.DKGRAY)
        }
    }

    private fun setupBatteryPreview() {
        updatePreviewTime()
        recreateBatteryDrawables()

        val isSwapped = RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
        updatePreviewLayoutDirection(isSwapped)

        // Battery Scrub Slider
        binding.sliderBatteryLevel.addOnChangeListener { _, value, _ ->
            val level = value.toInt()
            updateBatteryLevel(level)
        }

        // Quick Jump Chips
        binding.chipLevel20.setOnClickListener { binding.sliderBatteryLevel.value = 20f }
        binding.chipLevel50.setOnClickListener { binding.sliderBatteryLevel.value = 50f }
        binding.chipLevel80.setOnClickListener { binding.sliderBatteryLevel.value = 80f }
        binding.chipLevel100.setOnClickListener { binding.sliderBatteryLevel.value = 100f }

        // Simulate Charging Button
        binding.btnToggleCharging.setOnClickListener {
            isSimulatingCharging = !isSimulatingCharging
            updateChargingState(isSimulatingCharging)
        }

        // Status Bar Theme Mode (Day/Night) Toggle
        binding.btnToggleThemeMode.setOnClickListener {
            isLightStatusBarMode = !isLightStatusBarMode
            applyThemeMode(isLightStatusBarMode)
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
        updateChargingState(isSimulatingCharging)
    }

    private fun updateBatteryLevel(level: Int) {
        currentBatteryLevel = level
        statusBarBatteryDrawable?.setBatteryLevel(level)
        heroBatteryDrawable?.setBatteryLevel(level)

        binding.textStatusBarPercent.text = "$level%"
        binding.textHeroPercent.text = "$level%"
        binding.textScrubLevelBadge.text = "$level%"

        // Overshoot spring tactile animation on hero dial
        binding.imageHeroBattery.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(80)
            .setInterpolator(OvershootInterpolator(2f))
            .withEndAction {
                binding.imageHeroBattery.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun updateChargingState(charging: Boolean) {
        statusBarBatteryDrawable?.setChargingEnabled(charging)
        heroBatteryDrawable?.setChargingEnabled(charging)

        if (charging) {
            binding.btnToggleCharging.setBackgroundColor(Color.parseColor("#33FFB300"))
            binding.btnToggleCharging.setTextColor(Color.parseColor("#FFD54F"))
            binding.btnToggleCharging.iconTint = ColorStateList.valueOf(Color.parseColor("#FFD54F"))
            binding.btnToggleCharging.setText(R.string.charging_active)

            // Start ambient breathing pulse aura
            startGlowPulseAnimation()
        } else {
            val (fg, _) = getPreviewColors()
            binding.btnToggleCharging.setBackgroundColor(Color.TRANSPARENT)
            binding.btnToggleCharging.setTextColor(fg)
            binding.btnToggleCharging.iconTint = ColorStateList.valueOf(fg)
            binding.btnToggleCharging.setText(R.string.preview_simulate_charging)

            stopGlowPulseAnimation()
        }

        binding.imageHeroBattery.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(120)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                binding.imageHeroBattery.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
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
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopGlowPulseAnimation() {
        glowPulseAnimator?.cancel()
        glowPulseAnimator = null
        binding.viewHeroGlow.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun setupRingGeometrySelector() {
        updateRingGeometryVisuals(currentRingStyle)

        binding.cardStyleRing.setOnClickListener {
            if (currentRingStyle != BATTERY_STYLE_CIRCLE) {
                currentRingStyle = BATTERY_STYLE_CIRCLE
                applyRingStyle(currentRingStyle)
            }
        }

        binding.cardStyleDotted.setOnClickListener {
            if (currentRingStyle != BATTERY_STYLE_DOTTED_CIRCLE) {
                currentRingStyle = BATTERY_STYLE_DOTTED_CIRCLE
                applyRingStyle(currentRingStyle)
            }
        }

        binding.cardStyleFilled.setOnClickListener {
            if (currentRingStyle != BATTERY_STYLE_FILLED_CIRCLE) {
                currentRingStyle = BATTERY_STYLE_FILLED_CIRCLE
                applyRingStyle(currentRingStyle)
            }
        }
    }

    private fun applyRingStyle(style: Int) {
        RPrefs.putString(CUSTOM_BATTERY_STYLE, "$style")
        updateRingGeometryVisuals(style)
        recreateBatteryDrawables()

        binding.imageHeroBattery.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(100)
            .withEndAction {
                binding.imageHeroBattery.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun updateRingGeometryVisuals(selectedStyle: Int) {
        val primaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimaryContainer)
        val onPrimaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimaryContainer)
        val primaryColor = MaterialColors.getColor(binding.root, android.R.attr.colorPrimary)
        val surfaceLow = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerLow)
        val onSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
        val outlineVariant = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOutlineVariant)
        val textSecondary = MaterialColors.getColor(binding.root, android.R.attr.textColorSecondary)
        val density = resources.displayMetrics.density

        // Card 1: Ring
        val isRing = selectedStyle == BATTERY_STYLE_CIRCLE
        binding.cardStyleRing.setCardBackgroundColor(if (isRing) primaryContainer else surfaceLow)
        binding.cardStyleRing.strokeColor = if (isRing) primaryColor else outlineVariant
        binding.cardStyleRing.strokeWidth = if (isRing) (2 * density).toInt() else (1 * density).toInt()
        binding.iconStyleRing.setColorFilter(if (isRing) primaryColor else textSecondary)
        binding.textStyleRingTitle.setTextColor(if (isRing) onPrimaryContainer else onSurface)

        // Card 2: Dotted
        val isDotted = selectedStyle == BATTERY_STYLE_DOTTED_CIRCLE
        binding.cardStyleDotted.setCardBackgroundColor(if (isDotted) primaryContainer else surfaceLow)
        binding.cardStyleDotted.strokeColor = if (isDotted) primaryColor else outlineVariant
        binding.cardStyleDotted.strokeWidth = if (isDotted) (2 * density).toInt() else (1 * density).toInt()
        binding.iconStyleDotted.setColorFilter(if (isDotted) primaryColor else textSecondary)
        binding.textStyleDottedTitle.setTextColor(if (isDotted) onPrimaryContainer else onSurface)

        // Card 3: Filled
        val isFilled = selectedStyle == BATTERY_STYLE_FILLED_CIRCLE
        binding.cardStyleFilled.setCardBackgroundColor(if (isFilled) primaryContainer else surfaceLow)
        binding.cardStyleFilled.strokeColor = if (isFilled) primaryColor else outlineVariant
        binding.cardStyleFilled.strokeWidth = if (isFilled) (2 * density).toInt() else (1 * density).toInt()
        binding.iconStyleFilled.setColorFilter(if (isFilled) primaryColor else textSecondary)
        binding.textStyleFilledTitle.setTextColor(if (isFilled) onPrimaryContainer else onSurface)
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
                Snackbar.make(binding.root, "Layout: Percent First", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.cardOrderIconFirst.setOnClickListener {
            if (RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)) {
                RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, false)
                TransitionManager.beginDelayedTransition(binding.layoutPhoneStage)
                updatePreviewLayoutDirection(false)
                updateOrderCardVisuals(false)
                Snackbar.make(binding.root, "Layout: Icon First", Snackbar.LENGTH_SHORT).show()
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
            // Percent First Active
            binding.cardOrderPercentFirst.setCardBackgroundColor(primaryContainer)
            binding.cardOrderPercentFirst.strokeColor = primaryColor
            binding.cardOrderPercentFirst.strokeWidth = (2 * density).toInt()
            binding.checkPercentFirst.visibility = View.VISIBLE
            binding.textPercentFirstTitle.setTextColor(onPrimaryContainer)
            binding.textPercentFirstDesc.setTextColor(onPrimaryContainer)

            // Icon First Inactive
            binding.cardOrderIconFirst.setCardBackgroundColor(surfaceLow)
            binding.cardOrderIconFirst.strokeColor = outlineVariant
            binding.cardOrderIconFirst.strokeWidth = (1 * density).toInt()
            binding.checkIconFirst.visibility = View.INVISIBLE
            binding.textIconFirstTitle.setTextColor(onSurface)
            binding.textIconFirstDesc.setTextColor(textSecondary)
        } else {
            // Percent First Inactive
            binding.cardOrderPercentFirst.setCardBackgroundColor(surfaceLow)
            binding.cardOrderPercentFirst.strokeColor = outlineVariant
            binding.cardOrderPercentFirst.strokeWidth = (1 * density).toInt()
            binding.checkPercentFirst.visibility = View.INVISIBLE
            binding.textPercentFirstTitle.setTextColor(onSurface)
            binding.textPercentFirstDesc.setTextColor(textSecondary)

            // Icon First Active
            binding.cardOrderIconFirst.setCardBackgroundColor(primaryContainer)
            binding.cardOrderIconFirst.strokeColor = primaryColor
            binding.cardOrderIconFirst.strokeWidth = (2 * density).toInt()
            binding.checkIconFirst.visibility = View.VISIBLE
            binding.textIconFirstTitle.setTextColor(onPrimaryContainer)
            binding.textIconFirstDesc.setTextColor(onPrimaryContainer)
        }
    }

    private fun setupSizeController() {
        val sizeDp = RPrefs.getSliderInt(CUSTOM_BATTERY_WIDTH, DEFAULT_BATTERY_SIZE_DP)
            .coerceIn(MIN_BATTERY_SIZE_DP, MAX_BATTERY_SIZE_DP)

        binding.sliderBatterySize.value = sizeDp.toFloat()
        updateSizeDisplay(sizeDp)

        binding.sliderBatterySize.addOnChangeListener { _, value, fromUser ->
            val size = value.toInt()
            updateSizeDisplay(size)
            if (fromUser) {
                applyBatterySize(size)
            }
        }

        binding.btnSizeMinus.setOnClickListener {
            val current = binding.sliderBatterySize.value.toInt()
            if (current > MIN_BATTERY_SIZE_DP) {
                val next = current - 1
                binding.sliderBatterySize.value = next.toFloat()
                applyBatterySize(next)
            }
        }

        binding.btnSizePlus.setOnClickListener {
            val current = binding.sliderBatterySize.value.toInt()
            if (current < MAX_BATTERY_SIZE_DP) {
                val next = current + 1
                binding.sliderBatterySize.value = next.toFloat()
                applyBatterySize(next)
            }
        }

        binding.btnPreset16.setOnClickListener { setPresetSize(16) }
        binding.btnPreset20.setOnClickListener { setPresetSize(20) }
        binding.btnPreset24.setOnClickListener { setPresetSize(24) }
        binding.btnPreset28.setOnClickListener { setPresetSize(28) }
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

        val px = (sizeDp * resources.displayMetrics.density).toInt()
        binding.imageStatusBarBattery.layoutParams = binding.imageStatusBarBattery.layoutParams.apply {
            width = px
            height = px
        }

        updatePresetButtonStyles(sizeDp)
    }

    private fun updatePresetButtonStyles(activeSizeDp: Int) {
        val presetButtons = listOf(
            Pair(binding.btnPreset16, 16),
            Pair(binding.btnPreset20, 20),
            Pair(binding.btnPreset24, 24),
            Pair(binding.btnPreset28, 28)
        )

        val primaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimaryContainer)
        val onPrimaryContainer = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimaryContainer)
        val outlineVariant = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOutlineVariant)
        val onSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)

        for ((btn, size) in presetButtons) {
            if (size == activeSizeDp) {
                btn.setBackgroundColor(primaryContainer)
                btn.setTextColor(onPrimaryContainer)
                btn.strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(onSurface)
                btn.strokeColor = ColorStateList.valueOf(outlineVariant)
            }
        }
    }

    private fun updatePreviewLayoutDirection(swapped: Boolean) {
        binding.layoutStatusBarBattery.layoutDirection =
            if (swapped) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

    private fun setupActions() {
        binding.btnRestartSystemUI.setOnClickListener {
            // Tactile rotation animation on restart icon
            it.animate()
                .rotationBy(360f)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            try {
                SystemUtils.restartSystemUI()
                Toast.makeText(this, R.string.restart_sysui_success, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
                Toast.makeText(this, R.string.restart_sysui_no_root, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupAbout() {
        binding.textAboutDesc.movementMethod = LinkMovementMethod.getInstance()
    }
}
