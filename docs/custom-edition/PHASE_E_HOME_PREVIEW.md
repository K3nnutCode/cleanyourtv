# Home Mode — v6 Side-by-side TCL Preview

**Target**: TCL 4K-55V6B, Android TV 11. **Status**: code and CI test
build; on-device checks still required.

## Scope and design

This mode preserves the original CleanYourTV `MainActivity`,
`AppAdapter`, `DockDecoration`, and spring/glass focus handling.
Default upstream design already uses a **4-column dock row**, a single
near-black continuous dock band, landscape TV app banners from installed apps,
one selected app title ABOVE the dock, and the original app grid reachable with
Down. **No ambient photo slideshow, weather or prayer overlays on Home**.

Configuration defaults used in the preview include `dock_row=true`,
`dock_title=true`, `columns=4`, `rows_below_dock=0`,
`always_show_names=false`, `wallpaper_enabled=false` (unless a
theme override applies), and `cinematic_home=false`.
The upstream order has Netflix, Prime Video, YouTube and Disney+ placeholders,
but only apps actually installed on the TV are shown; `fill_dock=true`
populates missing positions with installed apps. Never invent app artwork.

The Home preview is deliberately launched as **an ordinary TV app**:

- Package `com.k3nnut.tvhome.home.preview.v6` is separate from Concept B v5
  (`com.k3nnut.tvhome.ambient.preview.v5`).
- `HomePreviewActivity` subclasses the original `MainActivity`, preserving
  upstream app scanning, focus movement, app launching and dock management.
- **NO HOME intent filter** in its custom `src/homePreview` Android manifest.
  The original MainActivity remains explicitly disabled, and all system
  screensaver services remain disabled in this v6 package.
- No Internet, fine location, secure system settings, or accessibility
  privileges requested. Only read-only network/Wi-Fi status permissions
  are needed for the standard connection indicator.
- A normal Back key on the preview Home finishes it; the TV Home button
  returns to the original Google TV Home. Opening other apps from the dock
  uses the upstream launcher routing; any problems are tested on the real TCL.
- The package does not change the system's default launcher, power settings,
  screensaver component, saved v5 photos, or existing home idle settings.

## Install and test (Windows Git Bash)

1. Download the passing CI APK:
   [Home v6 workflow](https://github.com/K3nnutCode/cleanyourtv/actions/workflows/custom-home-v6.yml).
2. Save it to Windows Downloads. Install:
   `export MSYS_NO_PATHCONV=1`
   `adb install "$(cygpath -w "$HOME/Downloads/cleanyourtv-home-v6.apk")"`
3. Launch explicitly:
   `adb shell am start -W -n com.k3nnut.tvhome.home.preview.v6/com.gathin.tvhome.HomePreviewActivity`
4. Inspect four landscape app banners and a single continuous dark dock, the
   focus border moving with **Left/Right**, and the selected app name above.
5. Press **Down** for the original grid; press Up to return. Press OK on one
   dock tile (e.g. Stremio) to launch and then Back/Home to confirm return.
   Back from the launcher itself should return to stock Google TV.
6. Check HDMI Inputs, Wi-Fi and Settings pills. All other upstream launcher
   navigation is unchanged. **Do not set v6 as the default HOME app**.
7. Capture the TV screenshot without revealing personal media:
   `adb exec-out screencap -p > "$HOME/Downloads/cleanyourtv-home-v6.png"`
   Review dock scale and titles at actual TCL display resolution.

## Interpretation

A green CI result proves the Android package compiled and the manifest safety
assertions passed, **not** that the real TCL remote handled every input
correctly. Actual Home preview acceptance requires a real-device screenshot
and a launch/return/grid test. The default launcher must remain Google TV
through this phase.

Any future default-Home installation is a separate user approval with a
full rollback plan. Concept B native screensaver v5 stays independent.
