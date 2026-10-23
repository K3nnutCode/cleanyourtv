# HAVEN — Original TV Home (working codename)

**Status:** Early interaction prototype. **Not** a production release, launcher replacement, or screensaver migration. The working CleanYourTV Home v8.3.1 and Concept B v6.3.1 continue untouched.

## Why we are building this

The fork proved the essentials on the actual TCL Android TV 11: physical D-pad focus, OK/Back/Home semantics, a four-banner dock, stable default HOME after reboot, photo screensaver and secure-enough home-network QR pairing with photo upload. We should turn this experience into a coherent, original UI and architecture rather than only styling its upstream.

## Product position

A private, photo-first living room interface built for an actual remote. No ads, no attention-stealing autoplay, no required account, core functionality without internet. Personal memories are the backdrop, apps are one gesture away, useful context appears only when needed.

Working codename is **HAVEN**; keep branding easy to change.

## Proposed interaction language

1. **Ambient canvas:** family or landscape photos, edge-to-edge cover (never stretch); quiet tint and scrim provide contrast while keeping the image visible. Wallpaper does not switch when app focus changes.
2. **Floating essentials:** large legible clock and restrained date header. Weather and next-prayer information are optional, sourced from existing services and shown only when valid. No made-up data.
3. **Focus rail:** four large, TV-native app banners along the lower third; focus lifts a tile slightly with a clear luminous outline and gentle spring. Never replace TV banner assets with phone icons. App title and optional metadata live outside the tile.
4. **Context stage:** one concise area that responds to focus. Only show actual Continue Watching and YouTube history from permitted/reliable APIs or integrations; don't pretend content exists when it doesn't.
5. **Quick shelf:** Inputs, Photos, Apps, and Settings remain one deliberate D-pad gesture from the rail, no crowded header row.
6. **Full library drawer:** all installed launchable apps; pin, unpin, reorder with remote; always keep physical Back exits predictable.
7. **Photo Manager:** preserve isolated Concept B QR uploader, lifetime/session code, offline network pairing and independently updateable DreamService.

## Signature design direction

- Dark charcoal overlay (#0A1017), warm white (#F4F6F5), pale aqua focus (#9FEAD9).
- Natural photo is the main color source; use accent only to communicate focus, not to tint the photo.
- Strong typographic hierarchy, generous 10-foot viewing distances and TV-safe padding.
- Calm micro-motion (120–220ms typical) and support reduced motion.
- Explicit focus, a consistent Back ladder: modal → library → Home → remain on Home.
- No navigation-only UI that depends on hover, touch, or phone pairing.
- Landscape 16:9 720p/1080p/4K scalable; handle TV display-density discrepancies.

## What improves on v8.3.1

| Area | Inherited strength | Original improvement |
| --- | --- | --- |
| App launch | Tested four-banner dock | Distinct Focus Rail and separate contextual stage |
| Background | Slideshow and artwork support | Background remains the user's own photos |
| Navigation | D-pad, OK, Back validated | Explicit focus map, accessibility and 10-foot ergonomics |
| Settings | Working shortcut to photo manager | Uniform design system, remote-editable layouts |
| Screensaver | Concept B v6.3.1 with QR upload | Preserve independent, photo-safe update path |
| Architecture | Proven Android package behavior | Break up 3,000+-line MainActivity into view/controller modules |
| Testing | GitHub Actions APK builds | Screenshot comparisons, automated D-pad flows and safe rollback |

## Development phases

**0. Baseline:** freeze tested v8.3.1 and v6.3.1. Don't disable/remove current package IDs, alter HOME role, clear photo data or share signing keys.

**1. Interaction concept:** offline single-file HTML prototype for desktop keyboard (←→↑↓, Enter, Esc), mirroring D-pad semantics. Validate layout, motion and information hierarchy before Android port.

**2. Android proof of concept:** new isolated package (e.g. `com.k3nnut.tvhome.haven.preview.v9`), preview Activity only, not HOME-eligible; create native screen with focus rail and quick shelf.

**3. Functional parity:** maintain app banner launch, installed-app grid, HDMI inputs, Settings, physical remote, and photo manager; integrate live optional data only when sourced.

**4. Quality gate:** ADB screenshot + D-pad test matrix, screen scaling, cold/warm launches, reboot, offline behavior, memory and animation jank checks.

**5. Optional default HOME trial:** separate v9 HOME candidate only after UI is proven, with exact tested rollback script and backup; do not touch working v8.3.1 until explicitly promoted.

## Non-goals for now

- Production keystore, app update checker, or automatic releases.
- Copying the upstream project's layout or imitating Apple TV pixel-for-pixel.
- Selling recommendations, advertisements, telemetry, forced login.
- Assuming third-party Stremio/YouTube playback state is accessible without actual approved integration.

## Acceptance criteria for Phase 1

- Visibly original, recognizable visual identity.
- Remote-first navigation demonstrated in prototype.
- Photo-first composition, legible at ten feet.
- No network requirements for the mock.
- Separate code, branch, and test package from deployed apps.
