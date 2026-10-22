# Custom Edition — Visual Blueprint (v0.1)

**This is a design proposal, not the currently implemented launcher.** All shown content in mockups is illustrative; photo art is a deliberately synthetic **placeholder**, never an image to ship on the TV. Clock/weather/prayer figures are mock labels, **not actual date-specific weather or official prayer times**.

- [Home mockup — 1920×1080 SVG](mockups/home-mode.svg)
- [Ambient mockup — 1920×1080 SVG](mockups/ambient-mode.svg)
- [Product specification](PRODUCT_SPEC.md)

## Overall visual intent

**Home:** faithful to upstream CleanYourTV: near-black, understated, one floating dock, one moving focus highlight, selected app name over a soft ambient glow, tiny system utilities. **No family photo here**: intentional separation from ambient view.

**Ambient:** full-bleed **local photos chosen by the owner**; time, weather and complete Brunei-Muara prayer line in a darkened **lower-zone legibility gradient**, with **the image still the dominant content**. No app dock, tiles, player, ads, captions or logo. On a bright photo, only the underlying text area gets enough shade for contrast.

## Home: reference screen 1920×1080

| Region | Suggested bounds | Behavior |
|---|---|---|
| Clock/greeting | x=100–440, y=72–250 | Minimal date + time; compact not a dashboard |
| Top controls | x=1360–1830, y=72–145 | Inputs, network, Settings, with TV remote navigation |
| Negative space | center/middle | Deliberate black; no media data or information cards |
| Focused app title | x≈395, y≈610–695 | One expressive label, subtle colored glow |
| Dock | x≈365–1555, y≈742–938 | 4 default favourites; dark charcoal band; 4 equal tiles |
| App drawer hint | center bottom | Quiet `↓ ALL APPS`; arrow navigation preserved |

**Interaction states:** focus slides across one band smoothly, focused app tile lifts slightly; Down opens installed apps/library without a major scene transition; Back returns to dock, and Input/Settings remote buttons work. No unnecessary loading animations.

## Ambient: reference screen 1920×1080

| Region | Suggested bounds | Behavior |
|---|---|---|
| Photo | entire 1920×1080 | One user-supplied image, aspect-preserving center crop |
| Gradient mask | y≈650–1080, local around info | Low-opacity dark tint only where text must be legible |
| Time/date | x≈110, y≈750–955 | Time in large thin type, small date below |
| Weather | x≈1435–1810, y≈775–910 | Small condition icon, temperature, location |
| Prayer line | x≈110–1810, y≈963–1055 | All five daily prayers, even spacing, restrained next-prayer underline |

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

1. Is Home's four-app dock and completely dark background the right level of simplicity?
2. Do you prefer Ambient data at the **bottom** as drawn, or time at top-left/weather top-right/prayers bottom?
3. Should the Ambient timetable show five prayer times by default, or next prayer only? The drawing selects five.
4. Approve these two compositions **before any launcher code changes or TV debloating**.
