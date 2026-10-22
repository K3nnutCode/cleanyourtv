# Phase 4 — Concept B native Android TV screensaver (v5)

## Status

Native `AmbientPhotoDream extends android.service.dreams.DreamService` implemented.
CI verifies the standalone opt-in APK; **TCL Android TV 11 screensaver selection,
idle activation, wake behavior and panel brightness still need a real-device test.**
The previously validated v4 manual slideshow is untouched.

### Design and isolation

- A separate test app: `com.k3nnut.tvhome.ambient.preview.v5`.
- `MainActivity` remains **disabled**: cannot become the TV HOME launcher.
- `AmbientPhotoActivity` stays launchable for Wi-Fi photo import, with the same
  ten-minute/stop-on-close local upload behavior tested in v4.
- `AmbientPhotoDream` is declared with
  `android.permission.BIND_DREAM_SERVICE` and only enabled when
  `-PambientPreview=true -PambientDreamPreview=true`.
- The original CleanYourTV `ClockDream` remains disabled in the test package.
- Source in this branch does **not** change any Android system settings.
  Installation alone does not replace the current screensaver.
- Photo library is **private to v5**. It cannot read photos stored by v4;
  import a couple of photos into v5 once before running a dream test.
- No stock/online photos. If there are none, the DreamService exits.
- The dream uses the 60-second fade slideshow, Brunei clock/date, cached/fetched
  Bandar Seri Begawan weather and the official offline PDI prayer table from
  the manual preview. Verified PDI coverage only Oct–Dec 2026.
- As in upstream `ClockDream`, the DreamService doesn't force the display to
  stay at maximum brightness. It uses a non-interactive dream: the first TV
  remote button should dismiss it and return to the previous app.
- No Wi-Fi upload server runs from the DreamService. Photo imports happen
  only while the user has deliberately opened the preview Activity's dialog.

## Prepare the TCL safely (no system settings change)

1. Install the v5 test APK from the
   [v5 GitHub Actions build](https://github.com/K3nnutCode/cleanyourtv/actions/workflows/ambient-concept-b-v5.yml).
2. Open the v5 photo-management screen using the TV app tile or:
   `adb shell am start -W -n com.k3nnut.tvhome.ambient.preview.v5/com.gathin.tvhome.AmbientPhotoActivity`.
3. Choose Wi-Fi import using OK, then upload **two** local photos using the
   temporary address and code displayed. Close the upload popup with OK or Back.
4. Verify photos display and rotate in the manual preview (no settings change).
5. Examine the current screensaver state without changing it:
   `adb shell settings get secure screensaver_components` and
   `adb shell settings get secure screensaver_enabled`.
   Some TCL firmwares ignore or store these keys differently.
6. Optional: open the **screensaver Settings UI** (does not select a dream
   automatically): `adb shell am start -a android.settings.DREAM_SETTINGS`.
   If unsupported, find **Screen saver / Daydream** in TV Settings.
7. **Stop here for owner approval before selecting/changing the system dream.**

## Owner-approved native dream test (TV settings UI)

Only after the owner explicitly chooses to activate Concept B as the TV's
screensaver: in native screensaver settings, select
**Concept B · Personal Photos** if the system lists it. Use **Start now/Preview**
if offered. Do not disable the stock launcher or grant extra permissions.

Observe:
- Dream shows the same center-cropped images, clock, weather and prayers.
- Photos change roughly every 60 seconds.
- One remote key dismisses the dream to the prior app instead of opening upload.
- Idle activation happens only if the vendor firmware permits external
  DreamServices (some Android TV models override the screensaver).
- Screen brightness and sleep/power behavior are acceptable on TCL.
- If v5 has no photos, the dream exits rather than showing stock photos.

## Rollback

Use TV native screensaver settings to switch back to the former screensaver;
do not disable Google TV HOME. v4 remains installed with its photos.
Avoid `settings put secure` commands until the owner explicitly requests
settings changes after testing.

### ADB note

On Android 11, `adb shell cmd dreams start-dreaming` is **not a guaranteed
non-mutating standalone preview**; some versions require root and others start
the *currently selected* dream. We therefore prefer the native Settings UI's
Start now/Preview feature after the owner actively selects the v5 component.
