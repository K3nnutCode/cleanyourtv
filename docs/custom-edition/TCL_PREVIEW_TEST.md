# Controlled TCL check — Ambient preview only

**Do not run until the Android TV emulator smoke job has passed and owner chooses to install the standalone preview.**
Last updated: 22 Oct 2026. This is NOT the final system screensaver, not a replacement Home, and not a debloat step.

## What stays unchanged

- The freshly reset TV's original Google TV Home/default launcher remains selected.
- No `pm disable-user`, `set-home-activity`, CleanYourTV `build-and-install.sh`, system-screen-saver setting, Accessibility Services or factory reset.
- No uploads of private family photos, network details, or screen captures to the **public** GitHub fork.
- The preview is a separately installable Android package, `com.k3nnut.tvhome.ambient.preview`. Its Home claimant and upstream `ClockDream`/remote key helpers are explicitly **disabled** in the preview build.
- The only network permission requested by the preview is `INTERNET` (for fixed-city weather); photo selection is local.

## After emulator proof and explicit approval

1. Complete the TCL's initial setup after factory reset. In Settings, enable Developer Options/debugging temporarily, and discover its **current** LAN address; the old address may no longer work.
2. Download the `ambient-photo-only-preview-apk` artifact from the **successful latest** [Custom Ambient Preview workflow](https://github.com/K3nnutCode/cleanyourtv/actions/workflows/custom-ambient-preview.yml) after it includes all approved branch commits. The zip contains `app-debug.apk`. Do not select a stale artifact or one built for `com.gathin.tvhome`.
3. On Windows Git Bash, explicitly substitute your current IP and exact APK path:
   ```bash
   adb connect <CURRENT_TV_IP>:5555
   adb shell cmd shortcut get-default-launcher
   adb install "$HOME/Downloads/app-debug.apk"
   adb shell am start -W -n com.k3nnut.tvhome.ambient.preview/com.gathin.tvhome.AmbientPhotoActivity
   ```
   If debugging is unavailable, **stop** rather than using launcher takeover or disabling OEM packages. If installation fails due to a different test signature, do **not** uninstall the default Google Home or CleanYourTV; an uninstall of **only the Ambient preview** would remove its imported photos and must be an informed choice.
4. Use `Choose local photos` to import a few pictures from a USB drive or file picker (JPG/PNG/WebP, <=10MiB each). If there is no compatible Documents UI, report the picker message and stop; the ADB-import alternative has not been validated yet.
5. Confirm photo fill/crop, one-second fade after ~60 seconds, clock/date at top-left, actual city weather at top-right, five prayer times at bottom. If the day is not in the verified October–December 2026 coverage, the prayer row should visibly say `Prayer times unavailable` and display **no invented times**.
6. Check `Back` to exit and the real physical `Home` key to return to the original Google TV launcher. Then run:
   ```bash
   adb shell cmd shortcut get-default-launcher
   adb shell dumpsys window | grep mCurrentFocus
   ```
   Default launcher must still be `com.google.android.apps.tv.launcherx` or the TV's actual OEM Home. No component of `com.k3nnut.tvhome.ambient.preview` should be a selectable HOME target.
7. If you want a private screenshot for review (not a public repo attachment):
   ```bash
   adb exec-out screencap -p > "$HOME/Downloads/ambient-concept-b-private.png"
   ```
8. Disable network/USB debugging after the session; revoke debugging authorizations if no longer needed. Do not leave port 5555 open.

## Acceptance gate

Do not change the default launcher or disable Google/TCL packages based only on this preview. System `DreamService` integration is a different feature requiring TCL power/wake testing and **separate approval**.
