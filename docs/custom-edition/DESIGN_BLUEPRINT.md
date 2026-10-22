# Custom Edition — Visual Blueprint (v0.1)

**Concept B and the original CleanYourTV dock are approved visual directions. This is a design blueprint, not proof of a finished working launcher.** All shown content in mockups is illustrative; photo art is a deliberately synthetic **placeholder**, never an image to ship on the TV. Clock/weather/prayer figures are mock labels, **not actual date-specific weather or official prayer times**.

- [Home mockup — 1920×1080 SVG](mockups/home-mode.svg)
- [Ambient concept A: consolidated bottom information — 1920×1080](mockups/ambient-mode.svg)
- [Ambient concept B: split-corner clock/weather + slim prayer footer — 1920×1080](mockups/ambient-mode-split.svg) (**recommended for the next review**)
- [Product specification](PRODUCT_SPEC.md)

## Overall visual intent

**Home:** faithful to upstream CleanYourTV: near-black, understated, one floating dock, one moving focus highlight, selected app name over a soft ambient glow, tiny system utilities. **No family photo here**: intentional separation from ambient view.

**Ambient:** full-bleed **local photos chosen by the owner**; time/date, weather and complete Brunei-Muara prayer line carefully separated into visual zones so **the center of the photo remains unobstructed**. Concept A concentrates information along the bottom; concept B places clock/date top-left, weather top-right and a slim prayer footer. Both use subtle local text gradients; **the image remains dominant**. No app dock, tiles, player, ads, captions or logo. On a bright photo, only the underlying text area gets enough shade for contrast.

## Home: reference screen 1920×1080

| Region | Suggested bounds | Behavior |
|---|---|---|
| Clock/greeting | x=100–440, y=72–250 | Minimal date + time; compact not a dashboard |
| Top controls | x=1360–1830, y=72–145 | Inputs, network, Settings, with TV remote navigation |
| Negative space | center/middle | Deliberate black; no media data or information cards |
| Focused app title | x≈115–700, y≈555–650 | Large light title above the dock, like original CleanYourTV, with restrained artwork-color glow |
| Dock | x≈116–1803, y≈696–956 | One wide charcoal rounded band, four **landscape TV app banners**, one floating glass focus outline; app-provided artwork at runtime |
| App grid | below y≈1030, slides up on Down | Installed apps in the original grid. **No All Apps button in the dock.** |

**Interaction states:** exactly the original CleanYourTV dock behavior is the design reference: four roughly 1.8:1 landscape app banners inside one long near-black rounded shelf; individual dock items have no persistent labels under them; one translucent glass highlight glides between banners, selected tile lifts slightly, and the large app name appears **above** the shelf. Down reveals the installed-app grid; Back returns to dock, and Inputs/Settings remote buttons work. Use installed app TV banners at runtime, not hardcoded example logo drawings or square mobile icons. No unnecessary loading animations.

## Ambient: reference screen 1920×1080

Two composition options are saved rather than silently replacing the previous concept. **Concept B (split corners) was explicitly selected by the owner on 22 October 2026**. It keeps a wide unobstructed middle region for people's faces, unlike a large permanent glass dashboard.

| Region | Suggested bounds | Behavior |
|---|---|---|
| Photo | entire 1920×1080 | One user-supplied image, aspect-preserving center crop |
| Gradient mask | top y≈0–380 and bottom y≈705–1080 | Only behind readable information; never dim the whole picture |
| Time/date | x≈105–700, y≈115–350 | Clock at top-left; compact date and location, no large surrounding card |
| Weather | x≈1450–1810, y≈145–300 | Weather at top-right: temperature, condition, area; no GPS needed |
| Prayer line | x≈110–1810, y≈865–1050 | Five daily prayer times in a single low footer; next-prayer treatment only one restrained small translucent highlight |
| Clear photo center | x≈100–1820, y≈365–705 | No titles, dock, marketing, recommendations or large translucent panel |

**Prayer line:** show all five using verified Brunei-Muara PDI values available for that date. Highlight the next prayer and label it `NEXT`. No sample times may be shipped. If timetable absent/outside valid range, show `Prayer timetable unavailable` rather than historical values. Allow later a compact `next only` user preference, but the full five-time line is our *design review default*.

**Position variation:** on very bright pictures, add only a minimal gradient/shadow around typography; a slight periodic offset can reduce image retention. No diffuse blurred background or strong frosted cards.

## Layout and type

- TV 16:9, initial target 1920×1080. Responsive scaling for other display sizes with safe margins ~90–110px.
- Design language: dark neutral #07090D, charcoal panels #20252C, near-white #EEF0F3, warm-gray tertiary #A7ADB5. Use existing Outfit font licensing or existing upstream typefaces; do not add unlicensed fonts.
- One light focus treatment, short motion, no simultaneous competing animations.
- Avoid saturated accent colors except native installed app art and a subtle focus tint.
- Accessibility: 10-foot reading distance; contrast validated using real TCL screenshots.

## Critical implementation observation

Current upstream `ClockDream.java` renders only `ClockScene` into `dream_clock.xml`: clock/date on black with ambient glow, **no photos/network/weather/prayers**. `ClockActivity.java` starts a similar clock after a Home-specific idle delay. `WallpaperPlayer.java` handles local wallpaper rotation only on Home. These are distinct systems: the ambient proposal **is not available by changing a theme JSON alone**. Follow the staged implementation/testing in PRODUCT_SPEC.md.

## Review prompts

1. Does the **original CleanYourTV wide-banner dock** and completely dark Home look right now? Its logos in the SVG are only placeholders; production will use installed app TV banners.
2. **Decision complete:** owner chose [Concept B](mockups/ambient-mode-split.svg). It keeps the center of their photos free and puts clock and weather in the corners, prayers in the footer.
3. Should the Ambient timetable show five prayer times by default, or next prayer only? The drawing selects five.
4. **Composition approved; proceed with a separate reversible photo-only preview prototype on this development branch.** No system-package changes, TV installation or Home takeover without explicit approval.
