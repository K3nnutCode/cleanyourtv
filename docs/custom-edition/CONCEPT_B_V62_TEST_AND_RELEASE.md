# Concept B v6.2 — Visual refinement, test and release

## What changed
- Gallery: three-column photo thumbnails in a dark TV-friendly panel, focused card outlines, clear add/delete/close controls, and confirmed per-photo / all-photo deletion.
- Slideshow preview: removed the bottom instruction banner that covered prayer times. The focusable Manage Photos button remains in the top right.
- Information overlay: stronger gradient behind clock, weather and prayer times, plus text shadows and a clearer next-prayer highlight.
- The actual native DreamService remains non-interactive and never runs an upload server.

## Safety invariants
- v6.2 test package: `com.k3nnut.tvhome.ambient.preview.v62`.
- Active installed production-like v6.1 package: `com.k3nnut.tvhome.ambient.preview.v61`.
- CleanYourTV HOME v8.1 and its **Manage Screensaver Photos** shortcut still target **v6.1**.
- Installing v6.2 does not automatically change HOME, the selected screensaver, or any previous package's private photos.
- A debug test package must not be confused with a stable signed release.
- Do not uninstall v6.1 or clear its app data. Keep the verified PC photo archive.

## Test installation
Build: GitHub Actions `ambient-v62-visual-polish.yml`. Download the artifact,
extract the APK, and install via `adb install`. Open the **v6.2** preview:
`adb shell am start -W -n com.k3nnut.tvhome.ambient.preview.v62/com.gathin.tvhome.AmbientPhotoActivity`.

Upload one or more photos from the PC/phone using the on-TV code and URL, and verify:
1. Gallery opens with the remote OK button.
2. Three columns are readable; focus is visibly highlighted.
3. Deleting a photo asks for confirmation and updates the gallery; Back/cancel does not delete.
4. Delete all asks for confirmation and empties the collection.
5. Uploading again works.
6. Clock/weather/prayer information is legible on a bright photo without a bottom preview banner.
7. v8.1 remains the selected HOME and v6.1 remains the system screensaver.

The v6.2 preview starts with an empty library because Android isolates package private storage.

## Owner-signed stable release — manual only
The `ambient-v62-signed-release.yml` workflow builds
`com.k3nnut.tvhome.ambient.release` with versionCode 620 and versionName 6.2.0.
It requires four **GitHub Actions repository secrets**:
- `TV_RELEASE_KEYSTORE_BASE64` — Base64 of the owner's Android keystore file.
- `TV_RELEASE_STORE_PASSWORD` — Keystore password.
- `TV_RELEASE_KEY_ALIAS` — Signing key alias.
- `TV_RELEASE_KEY_PASSWORD` — Key password.

Never send signing keys or passwords in chat or commit them to Git.
Securely store an offline copy of the keystore; losing it prevents updates signed with
the same key. The workflow fails if signing credentials are missing and verifies
the APK signing certificate before publishing an artifact.

This **release package is separate**, so it will NOT overwrite the existing v6.1 photo library.
Only after user acceptance should photos be copied from the v6.1 backup and
the selected system screensaver switched to the release package. Updating the v8.1
launcher shortcut to the final release package requires a separately tested HOME build.
Keep the v8/v8.1 launcher and v5/v6.1 screensavers as rollback paths until then.
