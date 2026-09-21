# Circle Battery

A focused LSPosed module that replaces the SystemUI battery icon with a clean circle gauge. Three ring styles, percent-first layout option, live device readout.

## Setup

1. Install the APK.
2. Enable **Circle Battery** for **SystemUI** in LSPosed Manager.
3. Restart SystemUI (in-app button needs root, otherwise reboot).

Requires Android 12+ on a Pixel / AOSP-based ROM.

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release signing uses `keystore.properties` when present, otherwise debug keys.

## Credits

Based on [Iconify](https://github.com/Mahmud0808/Iconify). GPL-3.0-only, see `LICENSE`.
