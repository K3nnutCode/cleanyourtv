# Home v7 — TCL visual refinements (opt-in preview)

**Based on TCL real-device v6 screenshot and verified remote navigation.**
Builds on `feature/custom-home-ambient` only; `main` remains unchanged.

## Changes compared with v6

1. **Reduced colored ambient glow** around the selected dock tile. Original
   CleanYourTV draws a full-screen tinted RadialGradient. The Home preview
   uses a much softer alpha mask (0x24 and 0x0D versus upstream 0x5C and
   0x26). This preserves artwork-derived hue and moving glass focus, while
   keeping the background predominantly near-black.
2. **Clean dock-only landing view**. `HomePreviewActivity` now clips only the
   `RecyclerView` drawing at the bottom of the measured first dock row
   when dock focus is active. This hides the cropped app-grid banners
   previously showing below the dock. Press Down removes the clip
   immediately, keeping the existing RecyclerView scrolling, grid focus,
   app launch and Back navigation unchanged.
3. **Fourth dock slot prefers Stremio**. The official Android TV package is
   `com.stremio.one`; only `HomePreviewActivity` without a custom JSON
   theme swaps the fourth preset from Hotstar to Stremio. The original
   `fill_dock` behavior still selects another installed app if Stremio
   is missing. The first three slots remain Netflix, Prime Video, YouTube.

## TCL test plan

- Install the v7 non-HOME APK (separate from v6 and Concept B v5).
- Launch `com.k3nnut.tvhome.home.preview.v7/com.gathin.tvhome.HomePreviewActivity`.
- Check four banners in single near-black dock. There should be *no visible
  cropped grid tile row* along the screen's bottom while dock is selected.
- Observe YouTube glow: restrained compared with v6.
- Confirm Stremio is fourth if installed, otherwise determine the TV's
  actual package name via read-only `adb shell pm list packages | grep -i stremio`.
- Press Down, confirm grid is fully visible and functional.
- Press Back, confirm dock returns and the grid is hidden below.
- Press Left/Right through four apps, confirming focus glass remains correct.
- Press OK on Stremio, navigate Back to Home preview, then Home to Google TV.
- The native system DreamService selection and 2-minute screen_off_timeout
  stay unchanged. No `pm disable`, no launcher-default edits, no system
  setting writes, no additional permissions.

A CI green build only verifies compilation, tests, and the non-HOME APK
manifest. Final behavior requires the physical TCL check.
