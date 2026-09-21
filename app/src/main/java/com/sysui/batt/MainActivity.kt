package com.sysui.batt

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.Toast
import kotlin.math.roundToInt
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentRingStyle = BATTERY_STYLE_CIRCLE
    private var deviceBatteryDrawable: BatteryDrawable? = null
    private var orderIconFirstDrawable: BatteryDrawable? = null
    private var orderIconSecondDrawable: BatteryDrawable? = null
    private var lastBatteryLevel = -1
    private var lastCharging = false
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
            registerBatteryReceiver()
            refreshDeviceBattery(registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        }
    }

    override fun onPause() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Throwable) {
        }
        super.onPause()
    }

    override fun onDestroy() {
        sizeAnimator?.cancel()
        restartIconFade?.cancel()
        restartSpin?.stop()
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

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) refreshDeviceBattery(intent)
        }
    }

    private fun registerBatteryReceiver() {
        try {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Throwable) {
        }
    }

    private fun refreshDeviceBattery(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).takeIf { it > 0 } ?: 100
        val pct = if (level >= 0) (level * 100 / scale).coerceIn(0, 100) else return
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL && plugged != 0
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        binding.textDeviceLevel.text = "$pct%"
        binding.textOrderPreviewPercentFirst.text = "$pct%"
        binding.textOrderPreviewPercentSecond.text = "$pct%"
        binding.textDeviceStatus.text = deviceStatusText(status, charging)
        binding.textDeviceSource.text = deviceSourceText(plugged)
        binding.textDeviceTemp.text = if (tempTenths != Int.MIN_VALUE) getString(R.string.device_temp_format, tempTenths / 10f) else "--"
        binding.textDeviceVoltage.text = if (voltageMv != Int.MIN_VALUE) getString(R.string.device_voltage_format, voltageMv / 1000f) else "--"
        binding.textDeviceHealth.text = deviceHealthText(health)
        lastBatteryLevel = pct
        lastCharging = charging
        updateDeviceBatteryIcon(pct, charging)
    }

    private fun createBatteryDrawable(): BatteryDrawable {
        val primary = MaterialColors.getColor(binding.root, android.R.attr.colorPrimary)
        val outline = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOutlineVariant)
        val d: BatteryDrawable = when (currentRingStyle) {
            BATTERY_STYLE_FILLED_CIRCLE -> CircleFilledBattery(this, primary)
            else -> CircleBattery(this, primary).apply {
                setMeterStyle(if (currentRingStyle == BATTERY_STYLE_DOTTED_CIRCLE) BATTERY_STYLE_DOTTED_CIRCLE else BATTERY_STYLE_CIRCLE)
            }
        }
        d.setColors(primary, outline, primary)
        if (lastBatteryLevel >= 0) d.setBatteryLevel(lastBatteryLevel)
        d.setChargingEnabled(lastCharging)
        return d
    }

    private fun updateDeviceBatteryIcon(level: Int, charging: Boolean) {
        if (deviceBatteryDrawable == null || orderIconFirstDrawable == null || orderIconSecondDrawable == null) {
            refreshAllBatteryIcons()
        }
        deviceBatteryDrawable?.setBatteryLevel(level)
        deviceBatteryDrawable?.setChargingEnabled(charging)
        orderIconFirstDrawable?.setBatteryLevel(level)
        orderIconFirstDrawable?.setChargingEnabled(charging)
        orderIconSecondDrawable?.setBatteryLevel(level)
        orderIconSecondDrawable?.setChargingEnabled(charging)
    }

    private fun refreshAllBatteryIcons() {
        val device = createBatteryDrawable()
        deviceBatteryDrawable = device
        binding.imageDeviceStatusIcon.setImageDrawable(device)
        val first = createBatteryDrawable()
        orderIconFirstDrawable = first
        binding.imageOrderPreviewIconFirst.setImageDrawable(first)
        val second = createBatteryDrawable()
        orderIconSecondDrawable = second
        binding.imageOrderPreviewIconSecond.setImageDrawable(second)
    }

    private fun deviceStatusText(status: Int, charging: Boolean): String = when {
        status == BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.device_status_full)
        charging || status == BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.device_status_charging)
        status == BatteryManager.BATTERY_STATUS_DISCHARGING -> getString(R.string.device_status_discharging)
        status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> getString(R.string.device_status_discharging)
        else -> getString(R.string.device_status_unknown)
    }

    private fun deviceSourceText(plugged: Int): String = when {
        plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> getString(R.string.device_source_ac)
        plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> getString(R.string.device_source_usb)
        plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> getString(R.string.device_source_wireless)
        else -> getString(R.string.device_source_unplugged)
    }

    private fun deviceHealthText(health: Int): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.device_health_good)
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.device_health_overheat)
        BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.device_health_dead)
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.device_health_over_voltage)
        BatteryManager.BATTERY_HEALTH_COLD -> getString(R.string.device_health_cold)
        else -> getString(R.string.device_health_unknown)
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
        refreshAllBatteryIcons()
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
                updateOrderCardVisuals(true)
                itHaptic(it)
                Snackbar.make(binding.root, R.string.layout_percent_first_msg, Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.cardOrderIconFirst.setOnClickListener {
            if (RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)) {
                RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, false)
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
        binding.sliderBatterySize.setLabelFormatter { v -> "${v.toInt()} dp" }
        binding.sliderBatterySize.addOnChangeListener { _, value, fromUser ->
            val s = value.toInt()
            updateSizeDisplay(s)
            if (fromUser) applyBatterySize(s)
            syncPresetChecked(s)
        }
        binding.sliderBatterySize.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(s: com.google.android.material.slider.Slider) {
                sizeAnimator?.cancel()
            }

            override fun onStopTrackingTouch(s: com.google.android.material.slider.Slider) {
                val snapped = s.value.roundToInt().coerceIn(MIN_BATTERY_SIZE_DP, MAX_BATTERY_SIZE_DP)
                s.value = snapped.toFloat()
                applyBatterySize(snapped)
                syncPresetChecked(snapped)
                itHaptic(s)
            }
        })
        binding.btnSizeMinus.setOnClickListener {
            val c = binding.sliderBatterySize.value.roundToInt()
            if (c > MIN_BATTERY_SIZE_DP) {
                animateSliderTo(c - 1)
                itHaptic(it)
            }
        }
        binding.btnSizePlus.setOnClickListener {
            val c = binding.sliderBatterySize.value.roundToInt()
            if (c < MAX_BATTERY_SIZE_DP) {
                animateSliderTo(c + 1)
                itHaptic(it)
            }
        }
    }

    private var sizeAnimator: ValueAnimator? = null

    private fun animateSliderTo(targetDp: Int) {
        val target = targetDp.coerceIn(MIN_BATTERY_SIZE_DP, MAX_BATTERY_SIZE_DP).toFloat()
        sizeAnimator?.cancel()
        val slider = binding.sliderBatterySize
        if (slider.value == target) {
            applyBatterySize(targetDp)
            syncPresetChecked(targetDp)
            return
        }
        sizeAnimator = ValueAnimator.ofFloat(slider.value, target).apply {
            duration = 350
            interpolator = emphasized
            addUpdateListener { a -> slider.value = a.animatedValue as Float }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    applyBatterySize(targetDp)
                    syncPresetChecked(targetDp)
                }
            })
            start()
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
        animateSliderTo(sizeDp)
    }

    private fun applyBatterySize(sizeDp: Int) {
        RPrefs.putInt(CUSTOM_BATTERY_WIDTH, sizeDp)
        RPrefs.putInt(CUSTOM_BATTERY_HEIGHT, sizeDp)
        updateSizeDisplay(sizeDp)
    }

    private fun updateSizeDisplay(sizeDp: Int) {
        binding.textSizeValueDisplay.text = "$sizeDp dp"
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

    private var restartSpin: IndeterminateDrawable<CircularProgressIndicatorSpec>? = null
    private var restartIconFade: ValueAnimator? = null

    // Expressive loading button: gentle press, then the restart glyph fades
    // out and an indeterminate spinner fades in at the same icon slot while
    // the label reads "Restarting…". Restore plays the same fade in reverse
    // plus a small settle pop so completion reads intuitively. The spinner
    // honors the system animator duration scale for reduced motion.
    private fun playRestartAnimation(v: View) {
        val btn = binding.btnRestartSystemUI
        restartSpin?.stop()
        restartSpin = null
        restartIconFade?.cancel()
        btn.isEnabled = false
        val origText = btn.text.toString()
        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120).setInterpolator(emphasizedAccelerate)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(emphasized).start()
                btn.text = "$origText…"
                fadeSwapRestartIcon(btn, showSpinner = true)
                v.postDelayed({
                    if (isFinishing || isDestroyed) return@postDelayed
                    fadeSwapRestartIcon(btn, showSpinner = false)
                    v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).setInterpolator(emphasizedAccelerate)
                        .withEndAction {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(emphasized).start()
                        }.start()
                    v.postDelayed({
                        if (isFinishing || isDestroyed) return@postDelayed
                        btn.text = origText
                        btn.isEnabled = true
                    }, 300)
                }, 1400)
            }.start()
    }

    private fun fadeSwapRestartIcon(btn: MaterialButton, showSpinner: Boolean) {
        restartIconFade?.cancel()
        val outgoing = btn.icon?.mutate()
        val incoming = if (showSpinner) {
            createRestartSpinner().also {
                restartSpin = it
                it.start()
            }
        } else {
            restartSpin?.stop()
            restartSpin = null
            getDrawable(R.drawable.ic_restart) ?: return
        }
        if (outgoing == null) {
            btn.icon = incoming
            return
        }
        restartIconFade = ValueAnimator.ofInt(255, 0).apply {
            duration = 150
            interpolator = emphasizedAccelerate
            addUpdateListener { a ->
                outgoing.alpha = a.animatedValue as Int
                btn.invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    incoming.alpha = 0
                    btn.icon = incoming
                    btn.invalidate()
                    restartIconFade = ValueAnimator.ofInt(0, 255).apply {
                        duration = 220
                        interpolator = emphasized
                        addUpdateListener { b ->
                            incoming.alpha = b.animatedValue as Int
                            btn.invalidate()
                        }
                        start()
                    }
                }
            })
            start()
        }
    }

    private fun createRestartSpinner(): IndeterminateDrawable<CircularProgressIndicatorSpec> {
        val density = resources.displayMetrics.density
        val onPrimary = MaterialColors.getColor(binding.btnRestartSystemUI, com.google.android.material.R.attr.colorOnPrimary)
        val spec = CircularProgressIndicatorSpec(this, null)
        spec.indicatorSize = (24 * density).toInt()
        spec.trackThickness = (3 * density).toInt()
        spec.indicatorColors = intArrayOf(onPrimary)
        spec.trackColor = Color.TRANSPARENT
        return IndeterminateDrawable.createCircularDrawable(this, spec)
    }

    private fun itHaptic(v: View) {
        try {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Throwable) {
        }
    }
}
