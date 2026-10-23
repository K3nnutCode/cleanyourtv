# CleanYourTV Home v8 — TCL Android TV 11 candidate

## Verified baseline
On 2026-10-23 v7 passed the on-TV visual review and all five remote checks: Left/Right dock navigation, OK launches Stremio, Down opens the grid, Back returns to the dock, and Home returns to Google TV.

v8 retains the v7 appearance and navigation. It adds Android HOME/DEFAULT registration through HomeCandidateActivity in a separately installable app. Back from the grid returns to the dock; Back on the dock does not close the selected system HOME. The ordinary v7 preview still exits on Back.

Package: com.k3nnut.tvhome.home.candidate.v8
All photo DreamServices and accessibility services remain disabled in the v8 candidate. Concept B v5 stays installed and selected independently. v8 does not need network or sensitive permissions.

## Build
Run from launcher/:
    ./gradlew --no-daemon testDebugUnitTest assembleDebug -PhomeCandidate=true -PlauncherApplicationId=com.k3nnut.tvhome.home.candidate.v8
GitHub Actions: .github/workflows/custom-home-v8.yml
Artifact: cleanyourtv-home-v8-candidate-apk; rename the downloaded APK to cleanyourtv-home-v8-candidate.apk.

## Manual on-TV preflight (does NOT change default Home)
Keep Google TV, Home Photos, v7 and Concept B v5 installed. Before switching Home, record current HOME resolution:
    adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
Install and open the candidate in Git Bash:
    export MSYS_NO_PATHCONV=1
    adb install "$(cygpath -w "$HOME/Downloads/cleanyourtv-home-v8-candidate.apk")"
    adb shell am start -W -n com.k3nnut.tvhome.home.candidate.v8/com.gathin.tvhome.HomeCandidateActivity
Repeat dock, grid, Stremio, Back, Home, HDMI and native screensaver tests. At this stage Home must still go to the user's previously selected launcher.

## Default Home selection and rollback
Only after the candidate passes on-TV checks and stock-Home recovery is verified, deliberately select the v8 candidate as Home through the TV's Home app picker or Android's supported package command. Installing the APK alone must never modify default Home.
Re-test return from apps, reboot, settings, inputs and Concept B idle start. If anything fails, restore the original Home app first, then consider removing the candidate. Some TCL builds restrict changing Home.

## Signing
This is a debug-signed candidate, not a permanently signed production update. Decide a consistent release application ID and private release-signing key before treating it as a long-term default. Never publish a signing key or password to GitHub.
