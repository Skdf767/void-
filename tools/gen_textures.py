#!/usr/bin/env python3
"""Procedurally generates the 16x16 textures for Void Ascension (original art, no Mojang assets).

Usage: python3 tools/gen_textures.py   (run from the project root; requires Pillow)
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "void_ascension", "textures")

PAL = {
    ".": (0, 0, 0, 0),
    "K": (11, 6, 20, 255),
    "D": (36, 16, 58, 255),
    "P": (90, 31, 140, 255),
    "L": (157, 78, 221, 255),
    "W": (224, 179, 255, 255),
    "C": (120, 240, 255, 255),
}

FRAGMENT = [
    "................",
    "..........KK....",
    ".........KWLK...",
    "........KWLPK...",
    ".......KWLPDK...",
    "......KWLPPDK.K.",
    ".....KLLPPDK.KCK",
    "....KWLPPDDK..K.",
    "....KLPPPDK.....",
    "...KLLPPDK......",
    "...KLPPDDK......",
    "..KLPPDDK.......",
    "..KPPDDK..KK....",
    "..KDDDK..KLK....",
    "...KKK....K.....",
    "................",
]


def save(img, *path):
    out = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    print("wrote", os.path.relpath(out))


def from_map(rows):
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, len(row))
        for x, ch in enumerate(row):
            img.putpixel((x, y), PAL[ch])
    return img


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def core(bound):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    r = 6.6
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d > r + 0.5:
                continue
            if d > r - 0.6:
                img.putpixel((x, y), PAL["K"])
                continue
            t = d / r
            # dark rim -> purple body
            col = lerp(PAL["P"], PAL["D"], min(1.0, t * 1.2))
            # swirl bands
            ang = math.atan2(dy, dx)
            swirl = math.sin(ang * 3 + d * 1.3)
            if swirl > 0.75 and t > 0.3:
                col = lerp(col, PAL["L"], 0.55)
            # top-left lighting highlight
            if dx + dy < -6 and t > 0.45:
                col = lerp(col, PAL["W"], 0.5)
            if bound:
                # glowing eye in the centre
                if d < 1.6:
                    col = PAL["C"] if d < 0.9 else lerp(PAL["W"], PAL["C"], 0.4)
                elif d < 2.8:
                    col = lerp(col, PAL["L"], 0.8)
            else:
                if d < 1.7:
                    col = PAL["K"]
            img.putpixel((x, y), col)
    # specular dot
    img.putpixel((5, 4), PAL["W"])
    if bound:
        for (x, y) in [(1, 2), (14, 3), (13, 13), (2, 12)]:
            img.putpixel((x, y), PAL["C"])
    return img


def ore():
    rnd = random.Random(1337)
    img = Image.new("RGBA", (16, 16))
    base = [(38, 38, 44), (48, 48, 54), (58, 58, 64), (70, 70, 76)]
    for y in range(16):
        streak = rnd.random() < 0.25
        for x in range(16):
            v = rnd.random()
            idx = 1 if v < 0.45 else (2 if v < 0.8 else (0 if v < 0.92 else 3))
            if streak and rnd.random() < 0.6:
                idx = 0
            img.putpixel((x, y), base[idx] + (255,))
    # crystal clusters
    clusters = [
        [(3, 3), (4, 3), (3, 4), (4, 4), (5, 2), (2, 5)],
        [(10, 5), (11, 5), (11, 6), (12, 6), (12, 4), (10, 6)],
        [(6, 10), (7, 10), (7, 11), (6, 11), (8, 9), (5, 12)],
        [(12, 12), (13, 12), (12, 13), (13, 13), (14, 11)],
    ]
    for cl in clusters:
        for i, (x, y) in enumerate(cl):
            img.putpixel((x, y), PAL["L"] if i in (0, 3) else PAL["P"])
        hx, hy = cl[0]
        img.putpixel((hx, hy), PAL["W"])
        # dark outline below/right for depth
        for (x, y) in cl:
            for (ox, oy) in ((1, 1),):
                nx, ny = x + ox, y + oy
                if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in cl:
                    img.putpixel((nx, ny), PAL["K"])
    return img


if __name__ == "__main__":
    save(from_map(FRAGMENT), "item", "void_fragment.png")
    save(core(False), "item", "void_core.png")
    save(core(True), "item", "void_core_bound.png")
    save(ore(), "block", "void_fragment_ore.png")
