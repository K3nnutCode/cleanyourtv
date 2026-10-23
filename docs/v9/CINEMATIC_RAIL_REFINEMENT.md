# HAVEN v9 — Cinematic Rail Refinement

**Status:** Browser UI/UX study, not Android APK, not installed and not selected as default HOME. Active baseline v8.3.1 (Home) and v6.3.1 (DreamService) stay untouched.

## Objective

Refine the approved hybrid design without redoing its architecture: retain photo-first image, horizontal weather/prayer/location strip *above* the large clock, a right-aligned vertical app rail, and quick tools docked below. The app rail should gain cinematic richness **without obscuring the family photograph** or causing remote-navigation ambiguity.

## UI changes

- App rail shrunk from approximately **423 × 128 dp-proxy cards** to **378 × 109 CSS px at 1600×900 mockup**; four visible. This is a design prototype, *not* production Android dp measurement.
- Each app has a clear text label and light logo; a right-side artwork excerpt, masked to a dark leading gradient. Never change the actual full-screen photo when app selection changes.
- Selected app has **one** cyan focus ring, slight lift and artwork brighten. Unfocused cards are deliberately quieter; no competing highlight on bottom quick tools.
- User can toggle between **Cinema** (artwork-forward) and **Quiet** (narrower, weaker artwork) cards without changing focus semantics.
- Three navigation states: **Ambient Home** (quiet controls, wallpaper prioritized), **App Focus** (one selected app), **Quick Navigation** (one highlighted utility).
- **Only demonstration assets.** Artwork and scene are extracted/composited from the approved HAVEN visual mockups. Weather, prayer, time, app entries and other data are illustrations, not live integrations.

## Focus / keyboard interaction contract for the offline prototype

| Input | Ambient | App Focus | Quick Navigation |
|---|---|---|---|
| Right | Open app rail | Stay within app rail | Next bottom item |
| Left | Open tools | Move to bottom tools | Previous bottom item |
| Up | Open app rail | Previous app (stops at first) | Return app rail |
| Down | Open tools | Next app (last goes to tools) | Next bottom item |
| Enter | Open app rail | Preview app activation | Preview tool activation |
| Escape | Stay/reset Ambient | Return Ambient | Return Ambient |
| 1 / 2 / 3 | Force 3 states (demo shortcut) | same | same |
| C / Q | Cinema / Quiet card treatments | same | same |

Buttons are **preview only**. Native Android must use the real TV remote focus system and enforce proper Back semantics.

## Usability and engineering gates

1. No more than one focus outline. A bright focused app and a bright focused bottom tool must never coexist.
2. Readable app labels at 720p/1080p and on real TV at sofa distance. Test both light and dark photos; no baked-in screenshot background for production.
3. Selected item remains in view, no clipping under overscan. Neighbor order deterministic on every D-pad direction; focus persists on mode switches.
4. Wallpaper is static on focus and may slideshow *only* at configured interval. Add reduced-motion support.
5. The first native implementation must be a separate preview APK without HOME eligibility or DreamService; no modifications to deployed stable packages.
6. Integrate real Android TV app banners and actual installed app list instead of brand placeholders. No fake continue-watching or playback history.

## Desktop prototype validation

Playwright/Chromium visual testing verified the three modes, a single active focus, app switching (Netflix → YouTube), quick navigation, Escape reset, artwork treatment toggle and 720p visual scaling. **This is not a physical TV test.**

## Selection question

Choose which treatment should become HAVEN's default:

- **Cinema:** larger visual artwork inside cards, better for entertainment-first homes.
- **Quiet:** less colorful, slightly narrower cards, better for family-photo-first homes.

The two can be retained as launcher settings later only if testing confirms the choice has user value.
