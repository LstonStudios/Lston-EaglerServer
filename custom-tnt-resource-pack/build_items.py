#!/usr/bin/env python3
"""Generate the lston: item models/textures for swords, guns, abilities and passes.

Every custom item gets its own item model (assets/lston/items/<id>.json) that the plugins select with
ItemMeta#setItemModel, so no custom item restyles a vanilla item for other players.
"""

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent
NS = ROOT / "assets" / "lston"
LEGACY = ROOT / "assets" / "minecraft" / "textures" / "items"
ABILITY_LEGACY = ROOT.parent / "custom-ability-resource-pack" / "assets" / "minecraft" / "textures" / "items"

OUTLINE = (30, 26, 24, 255)

# Sword key -> legacy texture (the old art, one distinct design per sword).
SWORDS = {
    "lightning": "gold_sword", "explosion": "iron_sword", "pearl": "diamond_sword",
    "fall": "stone_sword", "bad": "wood_sword", "tp": "diamond_axe",
}

# Ability key -> old per-ability texture from the ability pack.
ABILITIES = {
    "pyroclastic": "magma_cream", "void": "nether_star", "reaper": "flint", "sonic": "prismarine_shard",
    "quantum": "chorus_fruit", "chitinous": "iron_horse_armor", "overclock": "blaze_powder",
    "aegis": "ghast_tear", "gale": "feather", "shadow": "spider_eye_fermented", "tailwind": "rabbit_foot",
}

GRAY, DARK, STEEL = (120, 124, 134, 255), (62, 64, 72, 255), (170, 174, 184, 255)
WOOD, WOOD_D = (146, 96, 52, 255), (98, 62, 32, 255)
GOLD = (255, 205, 40, 255)


class Canvas:
    def __init__(self):
        self.px = [[None] * 16 for _ in range(16)]

    def rect(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if 0 <= x < 16 and 0 <= y < 16:
                    self.px[y][x] = color

    def dot(self, x, y, color):
        self.rect(x, y, x, y, color)

    def image(self):
        # Dark outline around the shape, then scale 2x so it matches the other 32px icons.
        out = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
        for y in range(16):
            for x in range(16):
                if self.px[y][x] is not None:
                    out[y][x] = self.px[y][x]
                elif any(
                    0 <= y + dy < 16 and 0 <= x + dx < 16 and self.px[y + dy][x + dx] is not None
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                ):
                    out[y][x] = OUTLINE
        return [[out[y // 2][x // 2] for x in range(32)] for y in range(32)]


# Guns are drawn like the TNT icons: 32px, noise-shaded parts with a lit top edge, a coloured accent band,
# a dark outline and a muzzle spark. Each gun is a list of (x0, y0, x1, y1, material) rectangles.
# Theme = (body, accent, spark) colours.
GUN_THEMES = {
    "pistol": ((120, 124, 134), (230, 230, 235), (255, 240, 150)),
    "smg": ((84, 88, 98), (255, 205, 40), (255, 230, 90)),
    "shotgun": ((140, 100, 52), (255, 170, 0), (255, 210, 90)),
    "sniper": ((70, 90, 110), (60, 220, 230), (150, 255, 255)),
    "rocket": ((96, 112, 62), (230, 50, 40), (255, 160, 60)),
    "revolver": ((110, 40, 40), (255, 205, 40), (255, 220, 120)),
    "rifle": ((72, 104, 70), (150, 220, 120), (230, 255, 160)),
    "minigun": ((70, 72, 80), (230, 70, 40), (255, 190, 80)),
    "freeze": ((70, 150, 210), (225, 250, 255), (200, 255, 255)),
    "thunder": ((140, 80, 210), (255, 240, 80), (255, 250, 150)),
    "railgun": ((90, 50, 150), (240, 70, 215), (255, 170, 250)),
}

# materials: b body, a accent, k dark metal, w grip, g glow
GUN_PARTS = {
    "pistol": [(6, 9, 23, 14, "b"), (23, 10, 28, 13, "k"), (8, 11, 21, 11, "a"), (6, 14, 19, 16, "k"),
               (8, 16, 13, 26, "w"), (14, 16, 17, 16, "k"), (14, 16, 15, 19, "k"), (28, 11, 29, 12, "g")],
    "smg": [(4, 9, 22, 15, "b"), (22, 10, 29, 13, "k"), (5, 11, 20, 12, "a"), (10, 15, 13, 26, "k"),
            (15, 15, 18, 22, "w"), (3, 10, 4, 14, "k"), (29, 11, 30, 12, "g")],
    "shotgun": [(10, 9, 29, 12, "k"), (10, 13, 27, 15, "b"), (11, 10, 28, 10, "a"), (1, 11, 11, 18, "w"),
                (12, 15, 16, 18, "w"), (4, 18, 8, 21, "w"), (29, 10, 30, 11, "g")],
    "sniper": [(8, 12, 30, 14, "k"), (3, 11, 14, 16, "b"), (0, 12, 3, 20, "w"), (8, 6, 20, 8, "a"),
               (10, 9, 11, 11, "k"), (17, 9, 18, 11, "k"), (9, 7, 19, 7, "g"), (13, 16, 15, 22, "k"), (30, 12, 31, 13, "g")],
    "rocket": [(2, 8, 22, 15, "b"), (22, 7, 29, 16, "a"), (2, 8, 22, 9, "g"), (0, 10, 2, 13, "k"),
               (10, 15, 13, 24, "k"), (6, 15, 8, 20, "w"), (29, 10, 30, 13, "k")],
    "revolver": [(12, 10, 28, 12, "k"), (6, 8, 14, 16, "b"), (8, 10, 11, 14, "a"), (4, 15, 10, 26, "w"),
                 (11, 15, 16, 16, "k"), (28, 10, 29, 11, "g"), (3, 8, 6, 10, "k")],
    "rifle": [(8, 10, 28, 13, "b"), (28, 11, 31, 12, "k"), (0, 10, 8, 17, "w"), (14, 13, 17, 17, "k"),
              (19, 13, 22, 21, "k"), (9, 7, 14, 9, "k"), (10, 11, 26, 11, "a"), (30, 11, 31, 12, "g")],
    "minigun": [(12, 7, 29, 9, "k"), (12, 11, 29, 13, "k"), (12, 15, 29, 17, "k"), (3, 6, 12, 18, "b"),
                (5, 8, 10, 8, "a"), (6, 18, 9, 25, "w"), (29, 8, 30, 8, "g"), (29, 12, 30, 12, "g"), (29, 16, 30, 16, "g")],
    "freeze": [(5, 8, 22, 15, "b"), (22, 9, 28, 14, "a"), (6, 9, 21, 10, "a"), (7, 15, 10, 25, "k"),
               (28, 10, 30, 13, "g"), (12, 5, 15, 7, "a"), (16, 4, 18, 7, "a")],
    "thunder": [(5, 8, 22, 15, "b"), (22, 9, 29, 14, "k"), (7, 10, 20, 11, "a"), (7, 15, 10, 25, "k"),
                (13, 15, 16, 20, "k"), (29, 10, 30, 13, "g"), (11, 5, 13, 7, "g"), (13, 3, 15, 5, "g")],
    "railgun": [(3, 8, 25, 10, "k"), (3, 15, 25, 17, "k"), (4, 11, 26, 14, "b"), (5, 12, 25, 13, "a"),
                (1, 9, 5, 16, "k"), (10, 17, 13, 26, "k"), (25, 11, 31, 14, "g"), (8, 8, 9, 17, "a"), (16, 8, 17, 17, "a")],
}


def gun_icon(name):
    import build_pack as bp
    body, accent, spark = GUN_THEMES[name]
    ink = bp._mix(body, 0.28)
    palette = {
        "b": body, "a": accent, "k": bp._mix(body, 0.55), "w": (96, 62, 34), "g": spark,
    }
    px = [[None] * 32 for _ in range(32)]
    for x0, y0, x1, y1, mat in GUN_PARTS[name]:
        base = palette[mat]
        h = max(1, y1 - y0)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if mat == "g":
                    px[y][x] = base
                    continue
                shade = 1.22 - 0.5 * ((y - y0) / h)
                if y == y0:
                    shade = 1.4
                if x in (x0, x1):
                    shade *= 0.85
                shade *= 0.95 + 0.1 * bp._noise(x, y, hash(name) % 97)
                px[y][x] = bp._mix(base, shade)
    out = [[(0, 0, 0, 0)] * 32 for _ in range(32)]
    for y in range(32):
        for x in range(32):
            if px[y][x] is not None:
                out[y][x] = tuple(px[y][x]) + (255,)
            elif any(0 <= y + dy < 32 and 0 <= x + dx < 32 and px[y + dy][x + dx] is not None
                     for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y][x] = tuple(ink) + (255,)
    return out


def redstone_pass(c):
    c.rect(2, 4, 13, 11, (240, 232, 200, 255)); c.rect(2, 4, 13, 4, (255, 250, 225, 255))
    c.rect(2, 7, 13, 8, (200, 40, 30, 255)); c.rect(4, 5, 5, 5, (150, 150, 150, 255)); c.rect(4, 10, 11, 10, (190, 180, 150, 255))
    c.dot(11, 5, (255, 90, 60, 255))


def pass_button(c):
    stone, light, shade = (143, 143, 148, 255), (190, 190, 196, 255), (96, 96, 102, 255)
    c.rect(2, 4, 13, 11, stone)
    c.rect(2, 4, 13, 4, light); c.rect(2, 4, 2, 11, light)
    c.rect(13, 5, 13, 11, shade); c.rect(3, 11, 13, 11, shade)
    # thin slit running left to right, outlined
    c.rect(3, 6, 12, 8, (58, 58, 66, 255))
    c.rect(4, 7, 11, 7, (8, 8, 12, 255))


def ability_icon(key):
    import ability_art
    return ability_art.ICONS[key]()


def write_png(path, image):
    import struct
    import zlib
    raw = b"".join(b"\0" + b"".join(bytes(p) for p in row) for row in image)

    def chunk(name, data):
        body = name + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">2I5B", len(image[0]), len(image), 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def register(name):
    """Write the model and item definition that point at lston:item/<name>."""
    for folder, data in (
        ("models/item", {"parent": "minecraft:item/generated", "textures": {"layer0": f"lston:item/{name}"}}),
        ("items", {"model": {"type": "minecraft:model", "model": f"lston:item/{name}"}}),
    ):
        path = NS / folder / f"{name}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=2) + "\n")


def copy_texture(source, name):
    target = NS / "textures" / "item" / f"{name}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    register(name)


def build():
    for key, legacy in SWORDS.items():
        copy_texture(LEGACY / f"{legacy}.png", f"sword_{key}")
    for key in GUN_THEMES:
        write_png(NS / "textures" / "item" / f"gun_{key}.png", gun_icon(key))
        register(f"gun_{key}")
    for key in ABILITIES:
        write_png(NS / "textures" / "item" / f"ability_{key}.png", ability_icon(key))
        register(f"ability_{key}")
    for name, draw in (("redstone_pass", redstone_pass), ("pass_button", pass_button)):
        canvas = Canvas()
        draw(canvas)
        write_png(NS / "textures" / "item" / f"{name}.png", canvas.image())
        register(name)


if __name__ == "__main__":
    build()
