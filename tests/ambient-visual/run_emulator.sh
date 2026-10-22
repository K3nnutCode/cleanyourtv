#!/usr/bin/env bash
# Runs as ONE process: android-emulator-runner splits inline YAML script lines
# into separate shell commands, which loses any variables defined earlier.
set -euo pipefail

APK="launcher/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.k3nnut.tvhome.ambient.preview"
PHOTO_DIR="files/ambient_photos"

test -s "$APK"
adb wait-for-device
adb install "$APK"
adb shell wm size 1920x1080

# Android TV 11 blocks arbitrary adb writes to /sdcard/Android/data.
# Copy a synthetic fixture into this DEBUG package's private directory via
# run-as. This cannot reach stock launcher files or alter the HOME role.
adb push artifacts/ambient-fixture.png /data/local/tmp/ambient-synthetic.png
adb shell run-as "$PACKAGE" mkdir -p "$PHOTO_DIR"
adb shell run-as "$PACKAGE" cp /data/local/tmp/ambient-synthetic.png "$PHOTO_DIR/ambient-synthetic.png"
adb shell rm /data/local/tmp/ambient-synthetic.png
adb shell am start -W -n "${PACKAGE}/com.gathin.tvhome.AmbientPhotoActivity"
sleep 15
adb exec-out screencap -p > artifacts/ambient-emulator-screenshot.png
adb logcat -d -t 350 > artifacts/ambient-emulator-logcat.txt || true
python3 tests/ambient-visual/visual_smoke.py check artifacts/ambient-emulator-screenshot.png
