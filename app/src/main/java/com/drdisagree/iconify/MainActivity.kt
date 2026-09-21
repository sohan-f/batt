package com.drdisagree.iconify

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.drdisagree.iconify.data.common.Preferences.BATTERY_STYLE_CIRCLE
import com.drdisagree.iconify.data.common.Preferences.CUSTOM_BATTERY_STYLE
import com.drdisagree.iconify.data.common.Preferences.CUSTOM_BATTERY_SWAP_PERCENTAGE
import com.drdisagree.iconify.data.config.RPrefs
import com.drdisagree.iconify.databinding.ActivityMainBinding
import com.drdisagree.iconify.utils.SystemUtils
import com.drdisagree.iconify.xposed.modules.batterystyles.CircleBattery
import com.drdisagree.iconify.xposed.utils.HookCheck
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var previewBatteryDrawable: CircleBattery? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        initDefaultPreferences()
        setupStatusCard()
        setupBatteryPreview()
        setupOptions()
        setupActions()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun initDefaultPreferences() {
        // Ensure default preference values are set to Circle Battery (35) and Swap = true
        if (!RPrefs.contains(CUSTOM_BATTERY_STYLE)) {
            RPrefs.putString(CUSTOM_BATTERY_STYLE, "$BATTERY_STYLE_CIRCLE")
        }
        if (!RPrefs.contains(CUSTOM_BATTERY_SWAP_PERCENTAGE)) {
            RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
        }
    }

    private fun setupStatusCard() {
        val isHookActive = HookCheck.isModuleActive()
        if (isHookActive) {
            binding.textStatusTitle.setText(R.string.status_module_active)
            binding.textStatusTitle.setTextColor(getColor(R.color.colorSuccess))
            binding.textStatusDesc.text = getString(R.string.status_module_active) + " - Circle Battery is active in SystemUI."
        } else {
            binding.textStatusTitle.setText(R.string.status_module_inactive)
            binding.textStatusDesc.setText(R.string.status_module_desc)
        }
    }

    private fun setupBatteryPreview() {
        val circleBattery = CircleBattery(this, Color.WHITE).apply {
            setColors(Color.WHITE, Color.DKGRAY, Color.WHITE)
            setBatteryLevel(85)
            setChargingEnabled(false)
        }
        previewBatteryDrawable = circleBattery
        binding.imageBatteryPreview.setImageDrawable(circleBattery)

        updatePreviewLayoutDirection(RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true))

        binding.sliderBatteryLevel.addOnChangeListener { _, value, _ ->
            val level = value.toInt()
            previewBatteryDrawable?.setBatteryLevel(level)
            binding.textPreviewPercent.text = "$level%"
            binding.textLevelLabel.text = "Battery Level: $level%"
        }

        binding.switchSimulateCharging.setOnCheckedChangeListener { _, isChecked ->
            previewBatteryDrawable?.setChargingEnabled(isChecked)
        }
    }

    private fun setupOptions() {
        val isSwapped = RPrefs.getBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, true)
        binding.switchSwapPercentage.isChecked = isSwapped
        updatePreviewLayoutDirection(isSwapped)

        binding.switchSwapPercentage.setOnCheckedChangeListener { _, isChecked ->
            RPrefs.putBoolean(CUSTOM_BATTERY_SWAP_PERCENTAGE, isChecked)
            updatePreviewLayoutDirection(isChecked)
            Snackbar.make(
                binding.root,
                if (isChecked) "Percentage position swapped (placed before icon)" else "Default position restored",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun updatePreviewLayoutDirection(swapped: Boolean) {
        // When swapped, status bar flips direction (RTL) placing percent before icon
        binding.layoutBatteryPreview.layoutDirection =
            if (swapped) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

    private fun setupActions() {
        binding.btnRestartSystemUI.setOnClickListener {
            try {
                SystemUtils.restartSystemUI()
                Toast.makeText(this, R.string.restart_sysui_success, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
                Toast.makeText(this, R.string.restart_sysui_no_root, Toast.LENGTH_LONG).show()
            }
        }
    }
}
