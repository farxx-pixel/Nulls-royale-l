# Cinder Coach

Clash Royale read-only advisor overlay. Watches your screen via MediaProjection,
reads elixir + hand + tower HP, and shows a floating HUD with a suggested play.
No input injection. No automation. You still tap everything.

## Build

Requires Android Studio or a local Android SDK with JDK 17.

```
./gradlew assembleDebug
```

APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

## Install

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or download the APK artifact from a GitHub Actions run and sideload it.

## First run

1. Open Cinder Coach.
2. Tap **Grant overlay permission** → enable in system settings.
3. Tap **Start capture** → accept the screen recording prompt.
4. Switch to Clash Royale. A draggable HUD bubble appears with the suggestion.

## Customizing your deck

Drop 40×40 PNG crops of each card in your deck into `app/src/main/assets/cards/`,
named `<card_name>.png` (e.g. `hog_rider.png`). The identifier matches live
hand slots against these templates. No matching template = "unknown".

## Tuning regions

All pixel coordinates live in `ArenaMap.kt`, calibrated for 1080×2400.
On other resolutions, the code scales automatically but you may need to nudge
the constants. Verify by comparing against your own screenshot.

## License

Personal use.
