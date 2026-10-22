# Ambient Screensaver — Implementation Blueprint (Pre-code)

**Status:** design/architecture plan only. Nothing in this document means software has been built, the Android screensaver has been activated, or the TV has been modified.

## Recommended visual target

- Compare [A — bottom grouping](mockups/ambient-mode.svg) with [B — split corners](mockups/ambient-mode-split.svg). B is recommended: clock/date top-left; weather top-right; the five Brunei-Muara prayer times as one narrow bottom footer; the middle of the photo remains unoccupied.
- Both are illustrative SVGs using synthetic landscape artwork, and fake/demo values where present. **No placeholder photos, numbers or geography should be bundled as a real user slideshow.**
- Artwork rule: real user's selected local photos only, no internet image searches, posters, video thumbnails or stock imagery.

## Verified upstream code boundaries

- `launcher/app/src/main/java/com/gathin/tvhome/ClockDream.java` is the Android `DreamService`; uses `ClockScene` on `dream_clock.xml` (clock, date, ambient glow) and the OS controls when it appears.
- `ClockActivity.java` is a distinct full-screen Activity started by the upstream Home-only idle timer. It returns to Home when key input is detected.
- `ClockScene.java` animates the clock location (one minute intervals) and a radial color glow. It is not a photo viewer or network client.
- `WallpaperPlayer.java` already renders local pictures for **Home wallpaper**, aspect preserving `CENTER_CROP`, backed by a single decoder executor. `WallpaperRotation.java` handles sequence, shuffle, and supported file scans. Do **not** accidentally turn Home's wallpaper back on just to get its slideshow.
- `AndroidManifest.xml` registers `ClockDream` as an Android DreamService. Do **not** remove, enable, select or force a system setting until deliberate compatibility testing.

## Proposed modular implementation

```text
AmbientPhotoRepository           user-selected local image set and permission checks
           |
AmbientSlideshowController       scan, sort/shuffle, sample decode on worker,
           |                     prefetch next, release inactive bitmaps
           v
AmbientScene (FrameLayout)       full-screen image layers + minimal visual overlays
      |          |         |
      |          |         +--- AmbientPrayerPresenter (verified timetable, current & next)
      |          +------------- AmbientWeatherPresenter (opt-in Open-Meteo, cached, TTL)
      +------------------------ TimeDatePresenter (device clock + Asia/Brunei)
           |
  +--------+-------------+
  |                      |
ClockActivity         ClockDream
Home-only idle        OS screensaver (opt-in)
```

Prefer shared view lifecycle methods (`start`, `pause/stop`, `destroy`) so only one timer and one decode queue can run at a time. Existing `ClockScene` remains as a fallback for zero-photo configuration and can be refactored only after test coverage.

### Phase A — Photo-only ambient (first code milestone)

1. Introduce **an explicit user-owned photo collection**, separate from Home wallpaper. Use Android Storage Access Framework (`ACTION_OPEN_DOCUMENT` with multiple image selection and persistable URI read grants) if a usable system file picker is installed on TCL. Check actual picker availability; **do not assume**. Provide a second explicit import route (e.g. USB/ADB-copy to a documented app directory) for a TV without the picker, with no hidden remote access service.
2. Persist only URI permissions or user-imported app-private file references. Users can remove photos and revoke grants. A factory reset removes all data; don't claim photos survive it.
3. Decode photos to display-size sample, maintain ≤2 active frame bitmaps, run decode on worker, render two `ImageView` layers in a stable FrameLayout, crossfade ~1s; no continuous full-screen animation. Use `CENTER_CROP` and never distort aspect ratio. Avoid decoding full camera originals at 8K.
4. Change photos every 60 seconds by default, only **while the screensaver is active**; no wallpaper timer on dark Home. Support sequential/shuffle, resume after pause without unnecessary repeated decode.
5. If collection is empty/unreadable, display dark fallback plus unobtrusive `Add photos in Ambient settings`. **Never auto-fill with bundled or downloaded images**.
6. A test preview action must be available without promoting the app to Android default HOME. Evaluate a separate preview package/application ID before any TV trial.

### Phase B — Clock, prayer and weather

1. Overlay time/date using `TextClock` or minute-update timestamps and zone `Asia/Brunei`. Respect TV clock format, detect manual time changes and crossing midnight.
2. **Five Brunei-Muara prayers**: Subuh, Zohor, Asar, Maghrib, Isyak. Data provider must include source, location, coverage period and date-specific values. The old `tv-detox` project only had a **date-limited 2026 offline timetable**, which must **not be copied as though it will always cover future dates**. If coverage missing, use an honest unavailable state and hide next-prayer countdown.
3. **Weather**: user opts into area-level Bandar Seri Begawan/Brunei-Muara; use Open-Meteo with declared `INTERNET` permission and asynchronous, rate-limited requests. Cache last valid observation, show age/source if stale, never block image transitions. Weather fails independently of prayer calculations.
4. Keep only small locally shaded text regions. B concept: time/date upper-left, weather upper-right, five prayers lower ~150px; slight bounded slow shift to avoid long-running static content retention. Respect TV screen-off/power-saver behavior.

### Phase C — Android system screensaver

1. `ClockDream` is system-invoked; **do not set screensaver_components or screensaver_enabled for the user**. Add a Settings UI showing how to select the custom screensaver manually if available.
2. Verify DreamService `setInteractive(false)`, `onDreamingStarted`, `onDreamingStopped`, `onDetachedFromWindow`, power/memory cleanup, and appropriate return to the **prior app**. Wake/Back/HOME must not cause accidental app selection.
3. Keep Home-only inactivity independent from OS DreamService. Never start a custom idle Activity while another streaming app/HDMI input is in the foreground.
4. Test both paths on TCL Android 11 with owner screenshot and real remote. Some OEM systems may disable/supersede Android standard dreams; surface this honestly. A manual screensaver preview can still work without any system takeover.

## Verification gates before install

- Unit tests for photo filter/ordering, invalid/deleted persisted URIs, empty collection and no-photo fallback.
- Date boundary and time-zone tests, five-prayer ordering, post-Isyak next-day behavior only when the next day's data exists, and out-of-range timetable warnings.
- UI screenshot checks at 1920×1080 for bright/low-contrast/portrait photo samples using **synthetic fixtures** (don't commit private family photos).
- Rotation test with 50 images over ≥15min: sampled image memory bounded, no main-thread image decode, no growing background worker after leaving screensaver.
- Real remote D-pad: exit from manual ambient preview and OS Dream; return to previous screen; all physical input/Settings keys still work.
- Launch launcher as **non-default independent preview** first; do not disable stock Google TV launcher, setup wizard, services or system Home before owner approval.
- Do not merge into `main` until validated, and keep the original CleanYourTV dock unchanged.

## Next step after design sign-off

Implement **Phase A photo-only ambient** on the existing feature branch with an isolated, reversible preview and test automation; **no APK deployment or Home role modification until separately approved**. Only then layer weather/prayers, compare screenshots with concept B, and consider TV testing.
