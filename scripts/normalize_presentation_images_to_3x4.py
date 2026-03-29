#!/usr/bin/env python3
"""Scale and center-crop presentation slide PNGs to exact 3:4 portrait (width:height)."""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image, ImageOps
except ImportError:
    print("Install Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

# 3:4 portrait (width x height)
TARGET_W, TARGET_H = 768, 1024

ROOT = Path(__file__).resolve().parents[1]
IMG_DIR = ROOT / "docs" / "presentations" / "images"


def main() -> None:
    if not IMG_DIR.is_dir():
        print(f"Missing directory: {IMG_DIR}", file=sys.stderr)
        sys.exit(1)
    paths = sorted(IMG_DIR.glob("*.png"))
    if not paths:
        print(f"No PNG files in {IMG_DIR}", file=sys.stderr)
        sys.exit(1)
    for path in paths:
        with Image.open(path) as img:
            if img.size == (TARGET_W, TARGET_H):
                print(f"{path.name} (already {TARGET_W}x{TARGET_H})")
                continue
            rgba = img.convert("RGBA")
            fitted = ImageOps.fit(
                rgba,
                (TARGET_W, TARGET_H),
                method=Image.Resampling.LANCZOS,
                centering=(0.5, 0.5),
            )
        fitted.save(path, format="PNG", optimize=False)
        print(f"{path.name} -> {TARGET_W}x{TARGET_H}")
    print(f"Normalized {len(paths)} images to {TARGET_W}x{TARGET_H} (3:4 portrait).")


if __name__ == "__main__":
    main()
