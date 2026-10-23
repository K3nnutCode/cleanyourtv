---
name: haven-android-tv-dev
description: Senior Android TV engineer skill for HAVEN launcher native Java implementation, D-pad focus, modular architecture, lifecycle, app banners, HDMI integration, photo manager, build, device testing, and rollback-safe ADB workflows.
---

# HAVEN — Android TV Engineer

## Objective
Deliver maintainable, performant, trustworthy Android TV interfaces that match approved UX specs and survive actual TCL remote use, reboot and screensaver activation. Read `docs/v9/PRODUCT_VISION.md` and `../haven-tv-ux/SKILL.md` before UI work. Follow repository `AGENTS.md` safety constraints; this skill does not supersede them.

## Baseline and isolation
- Production is **on hold**. The active, user-verified HOME is `com.k3nnut.tvhome.home.candidate.v831`; active DreamService is `com.k3nnut.tvhome.ambient.preview.v631`.
- Keep **all** working packages, data and photo galleries. Never uninstall, clear data, disable working HOME, set a new default, or reassign the native screensaver while iterating on HAVEN.
- Every HAVEN experimental APK uses its own unique package ID, e.g. `com.k3nnut.tvhome.haven.preview.v9`, preview Activity only, NO HOME intent filter and NO enabled DreamService initially.
- Don't ship an experiment that silently changes HDMI routing, physical Settings/Home handling, or the screensaver delay.
- Any eventual HOME trial requires explicit opt-in, backup, post-reboot check, and a reversible rollback script. Treat TCL user-specific screen density and role-manager oddities as tests, not edge assumptions.

## Workflow
1. **Read code and requirements.** Trace actual activity, manifest, layouts, resources, focus handling, and data owners. Identify owner-safe extension points and avoid unnecessary changes in legacy `MainActivity`.
2. **Create a clean vertical slice.** Prefer new dedicated modules (UI, focus controller, models, services, settings, state) over growing the existing 3,000-line MainActivity. Keep dependency direction testable.
3. **Design contract first.** Record layout in dp/sp and scale strategy, accessibility content descriptions, focus and Back map, animation properties, handling for 720p and 1080p.
4. **Build behavior correctly.** Respect lifecycle, main-thread UI updates, cancellation, bounded image memory and background work; no unbounded overlays, polling, leaking Activity references or unsupported third-party data scraping.
5. **Automated verification.** Compile, run relevant JVM/unit tests, add focus-state tests where possible, inspect manifest, audit APK package and enabled services, and validate safe permissions.
6. **On-device acceptance.** Verify ADB-launched preview, D-pad Up/Down/Left/Right/OK/Back, physical Home/Settings, cold/warm launch, HDMI menu, photo gallery, wallpaper aspect, font/display scale, and screenshot. Do not substitute simulator success for a TCL TV result.
7. **Report factually.** Distinguish committed code, CI success, installed APK, and actual user-verified TV behavior. Include a reproducible install/run/test command and rollback when relevant.

## TV interaction engineering
- Native remote **focus is the source of truth**; do not simulate focus only with visual CSS concepts.
- Keep predictable focus neighbors, default focus and restoration after modal exit or app return.
- Never consume physical Home/Back keys accidentally. OK activates the selected action exactly once; Back closes modal → drawer → Home.
- Use supported installed-app queries and TV banner resources; handle missing banners and app uninstalls gracefully.
- Keep image scaling `centerCrop`/cover for photo surfaces and `fitCenter` for app banners; do not stretch.
- Minimize layout overdraw and blur; cache thumbnails reasonably; avoid slow first-focus animation.
- Handle Android 11 peculiarities and ADB parsing robustly: `cmd role get-role-holders` may be missing and `ResolverActivity` can be transient; log alternatives, don't guess.

## Screen and lifecycle QA
- 720p and 1080p screenshots (also overscan/density/large font).
- 60fps target on real hardware where feasible; never claim a benchmark without measuring. Test memory pressure and cold startup.
- Both bright and dark family photos; long app titles, missing assets, offline weather, missing prayer time and no photos.
- Photo manager QR scanning/instant upload preserved in separate `v631` package and tested after any relevant integration changes.
- Disabled competitor launchers are rollback backups; do not remove them or claim they're permanently unusable.

## Build and release discipline
- One feature per branch/commit where practical; descriptive source comments and tests.
- Keep debug signing distinct from future owner signing, package IDs unique for parallel test builds, and upload no keystore/secrets.
- Ensure GitHub Actions `testDebugUnitTest assembleDebug` succeeds; inspect artifacts before giving the user install commands.
- No production replacement until specifically requested and after field acceptance.
- Every user-facing report answers: what changed, how tested, what's unverified, and how to revert.

## Never do
- Declare feature passed from code or a generated preview alone.
- Delete/replace private photo storage or reset shared preferences in an update.
- Disable core Android TV system packages to force the launcher.
- Present invented Stremio/YouTube playback state as real.
- Create fake success output or a claimed install/reboot you did not perform.
