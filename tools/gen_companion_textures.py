#!/usr/bin/env python3
"""
Generates production textures for the Void Companions (Abyssal Wisp, Rift Stalker, Null Oracle, and Rift Bolt).
All textures are 100% original pixel art adhering to VOID: ASCENSION's dark supernatural aesthetic.
"""
import os
import math
from PIL import Image

TEXTURE_DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "void_ascension", "textures", "entity", "companion")
os.makedirs(TEXTURE_DIR, exist_ok=True)

def create_abyssal_wisp():
    # 32x32 texture map for Abyssal Wisp
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    
    # Core (texOffs 0, 0: box 6x6x6)
    # Box layout in standard box mapping:
    # 0..24 x 0..12
    for x in range(24):
        for y in range(12):
            dx = abs(x - 12)
            dy = abs(y - 6)
            # Deep void purple with lighter center glow
            base_r = int(25 + 50 * math.sin((x+y)*0.5)**2)
            base_g = int(8 + 20 * math.sin((x*y)*0.3)**2)
            base_b = int(45 + 90 * math.cos((x-y)*0.4)**2)
            alpha = 245
            img.putpixel((x, y), (base_r, base_g, base_b, alpha))
            
    # Eye / Core glow on front face
    for ex in range(8, 12):
        for ey in range(6, 10):
            img.putpixel((ex, ey), (160, 70, 240, 255))
    img.putpixel((9, 7), (230, 180, 255, 255))
    img.putpixel((10, 8), (120, 240, 255, 255))

    # Tendril textures (texOffs 0, 12 and 6, 12: 2x8x1 and 1x8x2)
    for x in range(16):
        for y in range(12, 28):
            t = (y - 12) / 16.0
            r = int(120 - 80 * t)
            g = int(40 - 25 * t)
            b = int(220 - 100 * t)
            img.putpixel((x, y), (r, g, b, 230))
            
    out_path = os.path.join(TEXTURE_DIR, "abyssal_wisp.png")
    img.save(out_path)
    print("Wrote", out_path)

def create_rift_stalker():
    # 32x32 texture map for Rift Stalker
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    
    # Core (texOffs 0, 0: box 5x7x5)
    for x in range(20):
        for y in range(12):
            r = int(50 + 70 * math.cos(x*0.4)**2)
            g = int(10 + 15 * math.sin(y*0.3)**2)
            b = int(20 + 30 * math.sin((x+y)*0.5)**2)
            img.putpixel((x, y), (r, g, b, 255))
            
    # Crest (texOffs 20, 0: box 3x3x3)
    for x in range(20, 30):
        for y in range(8):
            img.putpixel((x, y), (180, 25, 40, 255))
    img.putpixel((24, 4), (255, 80, 80, 255))

    # Wings (texOffs 0, 12 and 12, 12: blade wings)
    for x in range(24):
        for y in range(12, 26):
            grad = (y - 12) / 14.0
            r = int(220 - 120 * grad)
            g = int(30 - 20 * grad)
            b = int(50 - 30 * grad)
            img.putpixel((x, y), (r, g, b, 240))
            
    out_path = os.path.join(TEXTURE_DIR, "rift_stalker.png")
    img.save(out_path)
    print("Wrote", out_path)

def create_null_oracle():
    # 48x48 texture map for Null Oracle
    img = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
    
    # Prism (texOffs 0, 0: box 6x6x6)
    for x in range(24):
        for y in range(12):
            r = int(15 + 20 * math.sin(x*0.5)**2)
            g = int(35 + 50 * math.cos(y*0.5)**2)
            b = int(60 + 80 * math.cos((x+y)*0.4)**2)
            img.putpixel((x, y), (r, g, b, 245))
            
    # Prism runes/eyes
    for (px, py) in [(8, 6), (10, 6), (9, 7), (14, 5), (15, 6)]:
        img.putpixel((px, py), (0, 240, 255, 255))

    # Inner Ring (texOffs 0, 12: 10x1x10)
    for x in range(30):
        for y in range(12, 23):
            r = int(10 + 15 * math.sin(x*0.2)**2)
            g = int(120 + 80 * math.cos(y*0.4)**2)
            b = int(180 + 75 * math.sin(x*0.3)**2)
            img.putpixel((x, y), (r, g, b, 235))

    # Outer Ring (texOffs 0, 23: 14x14x1)
    for x in range(35):
        for y in range(23, 40):
            r = int(8 + 10 * math.sin(x*0.3)**2)
            g = int(80 + 100 * math.cos(y*0.3)**2)
            b = int(150 + 105 * math.sin((x+y)*0.4)**2)
            img.putpixel((x, y), (r, g, b, 220))

    out_path = os.path.join(TEXTURE_DIR, "null_oracle.png")
    img.save(out_path)
    print("Wrote", out_path)

def create_rift_bolt():
    # 16x16 projectile texture
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    coords = [
        (8, 2), (7, 3), (8, 3), (9, 3),
        (6, 4), (7, 4), (8, 4), (9, 4), (10, 4),
        (7, 5), (8, 5), (9, 5),
        (7, 6), (8, 6), (9, 6),
        (6, 7), (7, 7), (8, 7), (9, 7), (10, 7),
        (7, 8), (8, 8), (9, 8),
        (8, 9), (8, 10), (7, 11), (8, 11), (8, 12), (8, 13)
    ]
    for (x, y) in coords:
        dist_center = abs(x - 8)
        if dist_center == 0:
            img.putpixel((x, y), (255, 180, 200, 255))
        elif dist_center == 1:
            img.putpixel((x, y), (220, 50, 80, 255))
        else:
            img.putpixel((x, y), (120, 20, 40, 255))
            
    out_path = os.path.join(TEXTURE_DIR, "rift_bolt.png")
    img.save(out_path)
    print("Wrote", out_path)

if __name__ == "__main__":
    create_abyssal_wisp()
    create_rift_stalker()
    create_null_oracle()
    create_rift_bolt()
