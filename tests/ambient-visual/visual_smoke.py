#!/usr/bin/env python3
"""Synthetic-image generator and pixel sanity checks for Android TV Ambient preview CI.

No personal photography: all images are generated from simple shapes.
The checker verifies the preview is not blank and that its photo was displayed.
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw


def fixture(path: Path):
    # 1280x720 with conspicuous vertical center bands that a solid-color
    # screensaver or a default clock cannot accidentally resemble.
    image = Image.new("RGB", (1280, 720))
    draw = ImageDraw.Draw(image)
    for y in range(720):
        draw.line((0, y, 1279, y), fill=(
            28 + y // 16, 85 + y // 12, min(230, 140 + y // 13)))
    draw.ellipse((475, 210, 805, 540), fill=(234, 168, 102))
    draw.rectangle((0, 620, 1280, 719), fill=(38, 83, 86))
    image.save(path)


def check(path: Path):
    image = Image.open(path).convert("RGB")
    w, h = image.size
    if w < 1200 or h < 650:
        raise SystemExit(f"Unexpected tiny emulator screenshot: {w}x{h}")
    def pixel(x, y):
        return image.getpixel((int(w * x), int(h * y)))
    # The middle must contain the bright synthetic sun (rather than black).
    middle = pixel(.5, .48)
    if not (middle[0] > 120 and middle[1] > 85 and middle[2] < 185):
        raise SystemExit(f"Expected locally imported synthetic photo at screen center, got {middle}")
    # Three distinguishable screen zones prevent an all-black/blank false pass.
    top = pixel(.5, .07)
    lower = pixel(.5, .83)
    if top == middle or lower == middle:
        raise SystemExit("Expected distinct synthetic photo regions")
    print(f"PASS: {w}x{h} screenshot contains imported test photo; colors center={middle}, top={top}, bottom={lower}")
    print("NOTE: automated pixel checks cannot approve exact font placement, prayer labels or TV visual polish.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["fixture", "check"])
    parser.add_argument("path", type=Path)
    args = parser.parse_args()
    if args.mode == "fixture":
        args.path.parent.mkdir(parents=True, exist_ok=True)
        fixture(args.path)
    else:
        check(args.path)
