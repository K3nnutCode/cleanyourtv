# CleanYourTV v8.3.1 + Concept B v6.3.1 handover

## Target setup

- HOME: `com.k3nnut.tvhome.home.candidate.v831/com.gathin.tvhome.HomeCandidateActivity`
- Native screensaver: `com.k3nnut.tvhome.ambient.preview.v631/com.gathin.tvhome.AmbientPhotoDream`
- Photo manager from Settings: `com.k3nnut.tvhome.ambient.preview.v631/com.gathin.tvhome.AmbientPhotoActivity`

Both builds are independent debug-signed test packages. They **must be installed** before promotion. Production signing with an owner-held key remains a separate task; never delete the current backups in anticipation of a signed release.

## Safe sequence

1. Check ADB connectivity, installed apps, current HOME and screensaver.
2. Use `adb exec-out run-as ... tar -C files -cf - ambient_photos` to back up **both** v6.1 and v6.3.1 private image libraries to the PC.
3. Validate both TAR backups, refuse filename collisions containing different image bytes, and preserve the v6.3.1 test-uploaded photos.
4. Use `adb push` to place the archive in a temporary file on the TV, then extract it under `run-as com.k3nnut.tvhome.ambient.preview.v631`. **Do not pipe a binary TAR through `adb shell -T`**: that previously resulted in a truncated image.
5. Verify every copied and existing photo using SHA-256.
6. Permit v6.3.1 `APP_AUTO_START`; select its DreamService; assign the HOME role and HOME activity to v8.3.1.
7. Verify the physical Home action via input key event, reboot, reconnect and verify HOME/screensaver/auto-start persist.
8. Manually confirm QR scan/upload and automatic native screensaver activation after idling.

The separate Git Bash handover and rollback scripts are supplied with the user-facing handover bundle. The handover stops on failed preflight or photo verification, and attempts to restore the old component selections if a post-switch check fails while ADB is reachable.

## Rollback commands

```bash
adb shell settings put secure screensaver_components \
  com.k3nnut.tvhome.ambient.preview.v61/com.gathin.tvhome.AmbientPhotoDream
adb shell cmd role add-role-holder --user 0 android.app.role.HOME \
  com.k3nnut.tvhome.home.candidate.v81
adb shell cmd package set-home-activity --user 0 \
  com.k3nnut.tvhome.home.candidate.v81/com.gathin.tvhome.HomeCandidateActivity
adb shell input keyevent 3
```

Do not uninstall or clear the data of v8.1, v6.1, v8.3.1, or v6.3.1 during handover. Photos are app-private and are deleted if their owning app is uninstalled.
