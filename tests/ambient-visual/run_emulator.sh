#!/usr/bin/env bash
# Runs as ONE process: android-emulator-runner splits inline YAML script lines
# into separate shell commands, which loses any variables defined earlier.
set -euo pipefail

APK="launcher/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.k3nnut.tvhome.ambient.preview"
PHOTO_DIR="/sdcard/Android/data/${PACKAGE}/files/ambient_photos"

test -s "$APK"
adb wait-for-device
adb install "$APK"
adb shell wm size 1920x1080

# Explicitly imported synthetic test fixture, never user photography.
adb shell mkdir -p "$PHOTO_DIR"
adb push artifacts/ambient-fixture.png "$PHOTO_DIR/ambient-synthetic.png"
adb shell am start -W -n "${PACKAGE}/com.gathin.tvhome.AmbientPhotoActivity"
sleep 15
adb exec-out screencap -p > artifacts/ambient-emulator-screenshot.png
adb logcat -d -t 350 > artifacts/ambient-emulator-logcat.txt || true
python3 tests/ambient-visual/visual_smoke.py check artifacts/ambient-emulator-screenshot.png
