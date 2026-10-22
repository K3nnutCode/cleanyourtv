# Ambient Photo Prototype — Phases A + B (manual preview)

**Status:** Source-only proof of concept. The owner approved **Concept B's visual composition**, not production deployment. No screenshots from the TCL or system-screensaver activation have been verified.

## What exists now
- Original upstream CleanYourTV **Home dock and UI code are unchanged**.
- Gradle opt-in `-PambientPreview=true` produces a separate `com.k3nnut.tvhome.ambient.preview` package **if** `-PlauncherApplicationId=com.k3nnut.tvhome.ambient.preview` is supplied. In that build, upstream `MainActivity` (HOME claimant) is **disabled**, and `AmbientPhotoActivity` is the only enabled launchable preview UI. It is deliberately **not** an Android `DreamService`.
- Choose local images via a compatible system Documents picker. Supported: JPEG/PNG/WebP ≤10 MiB per image; first 100 in collection. Selected URIs are copied to app-private internal `files/ambient_photos` directory (not shared storage). The **Ambient-only build** declares INTERNET exclusively to query Open-Meteo; the original CleanYourTV launcher build remains network-free.
- If the TV has no Documents picker, the app-private photo directory can be populated by an authorized local ADB `run-as` copy on a debug build, but the preferred path is choosing photos from the picker. This requires development-only ADB setup and has **not** been tested on TCL. It is not automatic and should not be done until the owner approves a trial.
- A 60-second slideshow uses aspect-preserving center-crop, sampled off-thread decoding, at most two displayed bitmaps, and explicit ~1000ms frame-timed smoothstep crossfade. No online content, stock wallpaper, movie posters or fake data can enter the collection.
- If empty, it displays a dark setup screen rather than bundled scenic wallpaper. In manual preview, Back exits and OK while a photo is visible opens the photo picker. This is a **temporary preview affordance**, not part of Concept B's finished screensaver.

## Concept B information overlay (implemented in preview source, pending TV validation)
- **Time/date:** live Android TextClock in Asia/Brunei, placed at the upper-left.
- **Weather:** upper-right, city-level Bandar Seri Begawan fixed coordinates; HTTPS Open-Meteo, cached and age-labelled when offline, refresh every 30 minutes, retry in 5 minutes on errors. Weather requires no device GPS data.
- **Prayers:** lower footer showing all five Brunei-Muara PDI 2026 date-specific prayer times with the next prayer highlighted. Coverage is **ONLY 2026-10-01 through 2026-12-31**; other dates show an unavailable warning rather than inferred numbers. Tomorrow is used for post-Isyak only if its table date is also verified.
- **Layout:** small top/bottom gradients only, clear middle photo; no added glass panels or playback content.

## What does NOT exist yet
- New verified PDI prayer data outside October–December 2026, or actual TCL weather/permission validation.
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
4. Confirm the actual Concept B overlay on the TV and source freshness. Then, separately, implement the **opt-in Android DreamService** after lifecycle and TV power checks.
