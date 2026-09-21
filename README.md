# Circle Battery (LSPosed)

LSPosed / Xposed module to replace the SystemUI battery icon with a circle style and swap the percentage position.

- App name: `Circle Battery` (`app/src/main/res/values/strings.xml:2`)
- Motto: `Replace battery icon with circle style and swap percentage`
- Package: `com.sysui.batt`, version `7.3.0` (`versionCode 25`) (`app/build.gradle.kts:19-25`)
- `minSdk 31` (Android 12+), `targetSdk / compileSdk 35`

Fork / stripped-down build of Iconify focused on a single tweak: `BATTERY_STYLE_CIRCLE = 35`.

## Features

- Circular battery gauge drawn by `xposed/modules/batterystyles/CircleBattery.kt:30`:
  - 360° arc (`canvas.drawArc(mFrame, 270f, 360f, ...)`), level sweep `3.6f * batteryLevel`
  - Charging bolt overlay with pulsing `ValueAnimator`
  - Power-save and charging color states, low-battery `!` under `CRITICAL_LEVEL = 5`
  - Dotted variant via `DashPathEffect` (`setMeterStyle()`), filled variant in `CircleFilledBattery.kt`
- Swap icon & percentage: flips `BatteryMeterView` to `LAYOUT_DIRECTION_RTL` so `%` renders before the icon (`BatteryStyleManager.kt:989-994`, `MainActivity.kt:113-117`). Default ON.
- App UI (`MainActivity.kt`, `res/layout/activity_main.xml`):
  - Module status card (active check via `xposed/utils/HookCheck.kt`)
  - Live icon preview with level slider (0-100) + simulate charging switch
  - Swap toggle + size slider persisted to `RPrefs`
  - Restart button (requires root)
- Defaults on first launch: style = Circle (35), swap = true (`MainActivity.kt:52-60`)

## Requirements

- Android 12+ Pixel / AOSP-based ROM (SystemUI `BatteryMeterView` hooks)
- LSPosed / Xposed framework, Xposed API `82` (`AndroidManifest.xml:40-41`)
- Module scope (`res/values/arrays.xml:4-8`): `android`, `com.android.systemui`, `com.android.settings`
- Root only needed for the in-app SystemUI restart button, not for the hook itself

## Installation / Usage

1. Install the APK.
2. Enable **Circle Battery** for **SystemUI** in LSPosed Manager.
3. Reboot / restart SystemUI.
4. Open the app:
   - Verify `Active`, otherwise the hook is not loaded (`status_module_desc`).
   - Toggle `Swap Icon & Percentage` as desired.
   - Tap `Restart SystemUI` to apply immediately.

## How it works

- Entry: `app/src/main/resources/META-INF/xposed/java_init.list` → `com.sysui.batt.xposed.ModernInitHook`
- `InitHook.kt` delegates to `HookRes` (resources) + `HookEntry` (package hooks).
- `EntryList.kt:10-25` loads:
  - own package → `HookCheck` (reports `isModuleActive() = true` when hooked)
  - `com.android.systemui` (non-child process) → `HookCheck` + `BatteryStyleManager`
- `BatteryStyleManager.kt`:
  - Hooks `systemui.statusbar.policy.BatteryControllerImpl` (`fireBatteryLevelChanged`, `firePowerSaveChanged`, `onReceive`) to push level/charging/power-save into custom drawables
  - Hooks `systemui.battery.BatteryMeterView` constructor / `updateColors` / `setPercentShowMode` / `updateShowPercent`, replaces icon `ImageView` drawable with `CircleBattery`, handles colors, scaling, RTL flip, charging icon view, and `ShadeHeaderController.onInit` for QS header
  - Reads prefs via `XPrefs` (`CUSTOM_BATTERY_STYLE`, `CUSTOM_BATTERY_SWAP_PERCENTAGE`), exposed by app via `RemotePrefProvider` (`AndroidManifest.xml:47-52`)
- In-app preview instantiates the same `CircleBattery` drawable directly, no hook needed.

## Project structure

```
app/src/main/
  AndroidManifest.xml          # xposedmodule=true, scope=@array/module_scope, RemotePrefProvider
  assets/xposed_init           # InitHook entry
  java/com/sysui/batt/
    MainActivity.kt            # status, preview, swap toggle, size slider, restart
    BattApp.kt                 # Application subclass
    xposed/ModernInitHook.kt EntryList.kt HookEntry.kt HookRes.kt ModPack.kt
    xposed/modules/BatteryStyleManager.kt
    xposed/modules/batterystyles/CircleBattery.kt CircleFilledBattery.kt BatteryDrawable.kt
    xposed/utils/HookCheck.kt XPrefs.kt
    data/common/Preferences.kt # BATTERY_STYLE_CIRCLE=35, CUSTOM_BATTERY_SWAP_PERCENTAGE
  res/values/strings.xml arrays.xml
  res/layout/activity_main.xml
```

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release signing uses `keystore.properties` if present (`app/build.gradle.kts:29-45`),
otherwise it falls back to debug keys. CI signs with the `KEYSTORE_BASE64`,
`KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD` repo secrets. Back up
`keystore.jks` plus its passwords — losing them means published apps can
never be updated in place.

## License

GPL-3.0-only, see `LICENSE`.

Circle Battery is a modified version of [Iconify](https://github.com/Mahmud0808/Iconify)
by Mahmudul Hasan (DrDisagree / Mahmud0808).

- Modified by sohan-f in 2026: stripped to the Circle Battery tweak only,
  rebranded app name/motto, replaced `MainActivity`, removed unused
  activities/fragments/adapters/services, added CI/release workflows.
- The entire work is conveyed under GPL-3.0, same as the Program.
- Corresponding Source: this repository (`https://github.com/sohan-f/batt`).

Upstream credits (preserved from Iconify): Android Open Source Project,
Substratum overlay tricks, [AOSPMods](https://github.com/siavash79/AOSPMods),
icons8.com / iconsax.io artwork, and all
[contributors](docs/contributors.md) and [translators](docs/translators.md).
