# Custom Edition — Product Specification (Design v0.1)

**Status:** PROPOSED / FOR OWNER REVIEW — no code or APK approved.
**Base:** [Gathin23/CleanYourTV](https://github.com/Gathin23/cleanyourtv), fork [K3nnutCode/cleanyourtv](https://github.com/K3nnutCode/cleanyourtv).
**Target:** TCL 4K-55V6B, Android/Google TV 11 (owner performs on-device checks).
**Design branch:** `feature/custom-home-ambient`.
**Related:** [DESIGN_BLUEPRINT.md](DESIGN_BLUEPRINT.md), mockups in `mockups/`.

## Vision

Keep the two things the owner valued in CleanYourTV: **a minimal, elegant TV launcher without sponsored Home content**, and a way to **reduce unwanted TV services selectively and reversibly**.

Make exactly two clear visual modes:
1. **Home Mode:** original CleanYourTV-inspired dark launcher for apps/navigation. No streaming recommendations, playback dashboard, weather panel, family pictures behind the dock, or full prayer timetable as permanent overlays.
2. **Ambient Screensaver Mode:** **ONLY the owner's locally imported photos**, plus an elegant clock/date, weather and Brunei-Muara prayer times. No app icons, movie posters, Google feed, remote-control navigation UI, random bundled images or advertisements. User pictures are the content, widgets are secondary.

**Important:** A new launcher hides Google TV's ad-filled Home when chosen as default; it does not, by itself, eliminate all advertising inside apps or all background telemetry. System cleanup is a distinct opt-in process.

## Mode A — Home

- Very dark neutral/near-black screen and generous empty space. No photo slideshow or recommendation feed on Home by default.
- Top left: minimal date and time; original CleanYourTV's greeting may remain if aesthetically consistent.
- Top right: compact Inputs, connection and Settings controls; reuse actual app routing and the native TV input framework.
- Lower middle: preserve **CleanYourTV's original four-wide-TV-banner dock** by default, inside one continuous rounded near-black band. Use real installed app banners (not hardcoded icon substitutes). User can configure app selection/order; a single translucent glass highlight follows focus, and selected tile rises slightly. The selected app's name appears large **above** the dock, with restrained artwork-derived ambient glow; **do not put persistent labels underneath the four dock banners**.
- Press Down: original CleanYourTV app grid/library; Back restores prior focus. Press Up as appropriate to top actions. **All Apps is not a fifth dock tile.** D-pad and real remote have precedence over animations.
- Optional quick link to Ambient Mode in Settings or long-press menu; never another permanent large tile unless requested.
- No intrusive permissions or forced app promos. Weather/prayers absent from Home by design; only in Ambient Mode.
- All functionality based on the upstream reference implementation where possible. Avoid rewriting its launcher layout, source switching and focus engine before a baseline comparison.

## Mode B — Ambient Screensaver

**Only allowable on-screen content:**
- **Background:** photos the owner explicitly imports/selects from local storage; no bundled demo/stock wallpapers on the TV, video, app thumbnails, fetched media or external photo feed.
- **Clock/date:** large clear time plus smaller localized Gregorian date. Timezone `Asia/Brunei`; respect device 12h/24h format.
- **Weather:** subtle temperature, condition and location from Open-Meteo (subject to explicit enablement), sensible limited updates (~30–60 min), and cache age indication. Missing Internet/API => an honest unavailable state or a visibly dated cached value; never fake a live reading.
- **Prayer times:** five daily Brunei-Muara times (Subuh, Zohor, Asar, Maghrib, Isyak), with **next prayer** visually singled out using restraint. Use verified, date-specific local PDI timetable (never invented times or naïve solar estimates presented as official). Cover future dates only after verifying current source; if absent, show `Prayer times unavailable`, not stale numbers.

**Photo handling:**
- User chooses/imports photos; default **60 seconds per image**, sequential or shuffle configurable; first screen stays black with clear setup guidance if library is empty. A graphic placeholder is permitted **in design documents only**, never shipped into production slideshow.
- Fit: aspect-preserving `CENTER_CROP`, no stretching, no blurred duplicate filling. Warn users that portrait photos can crop substantially. Option to later select FIT mode only by explicit request.
- Smooth ~0.8–1.1s fade when practical, predecode off UI thread; avoid video wallpapers, high-frequency shaders and performance-heavy blur.
- Limit to reasonable photo size, supported formats and memory budget; honor local permissions. Existing upstream `WallpaperRotation` supports JPG/JPEG/PNG/WebP, <=10MB, up to 100 images per folder; this is an **implementation reference**, not yet the finalized ambient library contract.
- Photos remain on the TV: no analytics, uploads or cloud sync. Don't commit user's photos, screenshot captures, ADB addresses, network names or secrets to this public GitHub fork.
- Overlays stay legible against bright/dark images with **small, localized gradient only behind text**. The image itself is never darkened as a whole if we can avoid it.
- Avoid static-image retention over long periods: clock/prayer panel may shift a few pixels periodically; use Android power-saving conventions and respect TCL's system screen-off time.
- No blocking overlays, interruptions or adverts; remote key should wake/dismiss reliably. Don't swallow the key and accidentally click a Home app tile.
- Weather and prayer are independent: screensaver still works if one data source is unavailable.

## Screensaver activation strategy (not implemented)

Upstream implements two different paths:
1. `ClockActivity` invoked by **Home idle** after `home_idle_clock_minutes`; user input returns to Home.
2. `ClockDream` registered as Android's `DreamService`; the OS may start it according to screensaver settings, and dismissal generally returns to prior context.

Upstream `ClockScene` and `dream_clock.xml` show **only clock/date with glow on black; no photos or network**. `WallpaperPlayer` currently paints slideshow photos **only while Home is visible**. Therefore the proposed photo + data screensaver requires new architecture and careful lifecycle/permission/cache testing.

**Proposed conservative rollout:**
- First prototype the photo screensaver **inside the independent preview** via a voluntary `Preview ambient mode` action and Home-only inactivity.
- Implement a real DreamService integration only after checking Android TV 11's screensaver selection, TV power settings and D-pad wake behavior on the owner's device. Never force-select the system screensaver, override sleep settings or change the default Home during early design work.
- When the TV is watching video/input, respect user-configured system behavior; don't introduce a custom timer that interrupts other apps.

## Setup and controls

- Onboarding: optional local photo picker/import, time/date format, prayer area (Brunei-Muara), weather opt-in (location **city/area-level only**, no exact GPS needed), photo order / interval, ambient inactivity delay, toggle each overlay visibility. Defaults remain useful without Internet.
- No photos: black ambient background + unobtrusive `Add your photos in Ambient settings`, until user adds one. Never select random system/stock images.
- No inaccurate connection badges, HDMI statuses, prayer times or weather reports.
- Exiting Ambient Mode: Back/Home/any navigation key follows a validated predictable rule; no double-trigger into app launches. Verify real TCL remote.
- Accessibility: TV 10-foot legibility, strong contrast, large enough focus target, no low-contrast labels; support reduced motion.
- Configurable, not hardcoded visual states whenever practical.

## Safe TV policy after factory reset

1. Owner has freshly reset TCL. **Leave original Google TV Home as default** during design and prototype development.
2. Development changes ONLY to this fork's `feature/custom-home-ambient` branch; `main` stays upstream-aligned.
3. No ADB operations, remote settings edits, Home hijack, default launcher changes, system package disable/uninstall or factory reset without explicit approval and verified rollback.
4. Later, do a **read-only inventory and backup first**, then propose an itemized opt-in debloat plan specific to the TCL's actual installed services; disable only approved nonessential packages with restore script and reboot validation. Never blind-run the upstream prompt's full automation or disable protected system/TV/input/Play services.
5. Existing `K3nnutCode/tv-detox` remains untouched and serves only as a reference for prior experiments.

## Design review gates (before coding)

- Owner approves **both** screen mockups and hierarchy; screenshots in `mockups/` are illustrative, with fake placeholder labels, not actual TV captures.
- Agree how many app tiles, whether app names always show, top controls and ambient widget positioning.
- Agree photos stay *only* in Ambient Mode, not Home; status at v0.1: **proposed**, owner approval pending.
- Agree local-only photo import, 60-second rotation, correct date-source behavior and weather failure state.

## Phase plan

1. **Design only (current):** specification, mockups, owner screenshot-level review. No builds/install.
2. **Reference baseline:** inspect upstream tests/CI and TCL compatibility without altering default Home.
3. **Implement Home customization as a theme/config change wherever possible.**
4. **Implement Ambient photo + info screen:** offline slideshow first, then trustworthy prayer data, weather; test power/lifecycle and latency.
5. **Independent preview build + optional real-TV validation**, safe rollback, original Home preserved.
6. **Debloat review** only after owner approval, with verified restore record and fail-safe.
7. **Consider setting custom Home default** only after separate explicit owner approval.

## Acceptance checklist

- [ ] Home uses CleanYourTV's **four landscape TV banners in one dark dock band**, selected app title above the dock, a single focus glass, compact clock/date and small top controls; no stacked square icon buttons or fake brand art.
- [ ] D-pad focus/app library/Inputs/Settings work and are readable.
- [ ] Ambient shows only explicitly imported local photos, time/date, weather and prayer times.
- [ ] No photo stretching, unintended stock images, app artwork, recommendations or ads inside either custom mode.
- [ ] Missing-weather and out-of-coverage-prayer states are accurate.
- [ ] Screensaver wake and power behavior tested on real TCL without disrupting watched content.
- [ ] Repo main and TV factory launcher have not been modified.
- [ ] Debloat, if eventually desired, is backed up, reviewed, reversible and owner-approved.
