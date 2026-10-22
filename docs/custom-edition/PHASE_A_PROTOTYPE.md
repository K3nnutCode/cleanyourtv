# Ambient Photo Prototype — Phase A

**Status:** Source-only proof of concept. The owner approved **Concept B's visual composition**, not production deployment. No screenshots from the TCL or system-screensaver activation have been verified.

## What exists now
- Original upstream CleanYourTV **Home dock and UI code are unchanged**.
- Gradle opt-in `-PambientPreview=true` produces a separate `com.k3nnut.tvhome.ambient.preview` package **if** `-PlauncherApplicationId=com.k3nnut.tvhome.ambient.preview` is supplied. In that build, upstream `MainActivity` (HOME claimant) is **disabled**, and `AmbientPhotoActivity` is the only enabled launchable preview UI. It is deliberately **not** an Android `DreamService`.
- Choose local images via a compatible system Documents picker. Supported: JPEG/PNG/WebP ≤10 MiB per image; first 100 in collection. Selected URIs are copied to app-specific `ambient_photos` directory. No Internet permission has been added.
- If the TV has no Documents picker, the app-specific external folder can be populated manually by the owner with authorized ADB file push. This requires development-only ADB setup and has **not** been tested on TCL. It is not automatic and should not be done until the owner approves a trial.
- A 60-second slideshow uses aspect-preserving center-crop, sampled off-thread decoding, at most two displayed bitmaps, and explicit ~1000ms frame-timed smoothstep crossfade. No online content, stock wallpaper, movie posters or fake data can enter the collection.
- If empty, it displays a dark setup screen rather than bundled scenic wallpaper. In manual preview, Back exits and OK while a photo is visible opens the photo picker. This is a **temporary preview affordance**, not part of Concept B's finished screensaver.

## What does NOT exist yet
- Full Concept B information overlays: time/date, Open-Meteo weather, **verified up-to-date** PDI Brunei-Muara five-prayer timetable.
- Actual system screen-saver selection, DreamService plumbing, clock-only fallback integration, idle trigger, Android TV remote compatibility tests, final photo setup/settings interface.
- No on-device launch, screenshot, permission or performance validation.

## Safety check
Never install this build as HOME; inspect the resulting preview manifest. **Do not use upstream `launcher/build-and-install.sh` for this prototype**, because that script intentionally changes the system launcher and may disable the factory Home.

The separate GitHub Actions workflow `Custom Ambient Preview (non-HOME)` builds the opt-in preview. Its artifact is a **test build**, not a final user-facing APK. The GitHub Action must pass its manifest gate before offering a trial.

## Local build command (for coding environments only, not the TV)

```sh
cd launcher
./gradlew --no-daemon testDebugUnitTest assembleDebug \
  -PambientPreview=true \
  -PlauncherApplicationId=com.k3nnut.tvhome.ambient.preview
```

No `adb` commands are to be run automatically. Keep the original TCL Google TV Home and current system services enabled.

## Next review
1. Examine the CI build result and safety gate.
2. Verify that a manual preview can import and cycle photos in an Android TV emulator without installing or selecting HOME.
3. Once explicitly authorized, test on the real TCL using an independent preview package, capturing real photos **privately**, never committing them to the public repo.
4. Then build the full Concept B overlays and the real opt-in screensaver service.
