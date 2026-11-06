#!/usr/bin/env python3
"""Draw the TNT-Zombie Overlord boss bar: red/black/gray bar sprites plus a winged frame glyph.

The boss uses the WHITE boss bar colour (no vanilla boss uses it), so only this boss is restyled.
The frame is a 192x24 bitmap-font glyph drawn over the bar as the bar title, with a TNT block in the middle.
"""

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent
SPRITES = ROOT / "assets" / "minecraft" / "textures" / "gui" / "sprites" / "boss_bar"
FONT_TEXTURE = ROOT / "assets" / "lston" / "textures" / "font" / "tnt_boss_frame.png"
PREVIEW = ROOT / "site" / "bossbar.png"

BLACK = (14, 14, 16, 255)
DARK = (40, 40, 46, 255)
GRAY = (104, 104, 112, 255)
LIGHT = (176, 176, 184, 255)
RED = (200, 24, 24, 255)
DRED = (122, 10, 12, 255)
BRIGHT = (244, 64, 52, 255)

BAR_WIDTH, BAR_HEIGHT = 182, 5
FRAME_WIDTH, FRAME_HEIGHT = 192, 24
BAR_LEFT, BAR_TOP = 5, 10


def background():
    image = Image.new("RGBA", (BAR_WIDTH, BAR_HEIGHT), DARK)
    pixels = image.load()
    for x in range(BAR_WIDTH):
        pixels[x, 0] = BLACK
        pixels[x, BAR_HEIGHT - 1] = BLACK
        for y in range(1, BAR_HEIGHT - 1):
            if (x + y) % 8 < 3:
                pixels[x, y] = (54, 54, 60, 255)
    return image


def progress():
    image = Image.new("RGBA", (BAR_WIDTH, BAR_HEIGHT), RED)
    pixels = image.load()
    rows = [BRIGHT, RED, RED, DRED, BLACK]
    for x in range(BAR_WIDTH):
        for y in range(BAR_HEIGHT):
            color = rows[y]
            if y in (1, 2) and (x + y) % 8 < 2:
                color = DRED
            pixels[x, y] = color
    return image


def shade(base, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in base[:3]) + (255,)


WING_WIDTH = 54


def _segment_distance(point, a, b):
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((point[0] - ax) * dx + (point[1] - ay) * dy) / (dx * dx + dy * dy)))
    return ((point[0] - ax - t * dx) ** 2 + (point[1] - ay - t * dy) ** 2) ** 0.5


def upper_wing():
    """Bat-style wing for the upper-left corner: bone fingers, ribbed membrane, scalloped black edge."""
    joint = (WING_WIDTH - 2, 8)
    tips = [(48, 0), (36, 0), (24, 0), (13, 1), (4, 2), (0, 6)]
    outline = [joint]
    for index, tip in enumerate(tips):
        outline.append(tip)
        if index < len(tips) - 1:
            nxt = tips[index + 1]
            mid = ((tip[0] + nxt[0]) / 2, (tip[1] + nxt[1]) / 2)
            outline.append((mid[0] + (joint[0] - mid[0]) * 0.3, mid[1] + (joint[1] - mid[1]) * 0.3 + 1))
    outline.append((0, 9))
    mask = Image.new("L", (WING_WIDTH, 12), 0)
    ImageDraw.Draw(mask).polygon(outline, fill=255)
    image = Image.new("RGBA", (WING_WIDTH, 12), (0, 0, 0, 0))
    pixels = image.load()
    mask_pixels = mask.load()
    for y in range(12):
        for x in range(WING_WIDTH):
            if not mask_pixels[x, y]:
                continue
            distance = min(_segment_distance((x, y), joint, tip) for tip in tips)
            if distance < 0.55:
                color = LIGHT if (x + y) % 5 == 0 else GRAY
            elif distance < 1.05:
                color = DRED
            else:
                color = BRIGHT if (x * 3 + y * 5) % 9 == 0 else RED
            pixels[x, y] = color
    # Black outline: any wing pixel touching empty space
    for y in range(12):
        for x in range(WING_WIDTH):
            if mask_pixels[x, y] and any(
                    not (0 <= x + dx < WING_WIDTH and 0 <= y + dy < 12) or not mask_pixels[x + dx, y + dy]
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                pixels[x, y] = BLACK
    for tip in tips:
        tx, ty = min(tip[0] + 1, WING_WIDTH - 1), min(tip[1] + 1, 11)
        pixels[tx, ty] = BRIGHT
    draw = ImageDraw.Draw(image)
    draw.rectangle([joint[0] - 3, 6, joint[0] + 1, 10], fill=GRAY, outline=BLACK)
    draw.point((joint[0] - 1, 8), fill=BRIGHT)
    draw.point((joint[0] - 2, 7), fill=LIGHT)
    return image


def flip_vertical_about_bar(image):
    """Mirror the upper wing into the lower corner so rows 7..17 mirror around the bar centre line."""
    out = Image.new("RGBA", (image.width, FRAME_HEIGHT), (0, 0, 0, 0))
    flipped = image.transpose(Image.FLIP_TOP_BOTTOM)
    out.paste(flipped, (0, FRAME_HEIGHT - image.height))
    return out


def wings(frame_image):
    top = upper_wing()
    left = Image.new("RGBA", (WING_WIDTH, FRAME_HEIGHT), (0, 0, 0, 0))
    left.alpha_composite(top, (0, 0))
    left.alpha_composite(flip_vertical_about_bar(top))
    frame_image.alpha_composite(left, (0, 0))
    frame_image.alpha_composite(left.transpose(Image.FLIP_LEFT_RIGHT), (FRAME_WIDTH - WING_WIDTH, 0))


def rails(draw):
    for y, color in ((7, GRAY), (8, BLACK), (9, DARK), (15, DARK), (16, BLACK), (17, GRAY)):
        draw.line([(BAR_LEFT, y), (FRAME_WIDTH - 1 - BAR_LEFT, y)], fill=color)
    for x in range(BAR_LEFT, FRAME_WIDTH - BAR_LEFT):
        stripe = (x // 3) % 2 == 0
        draw.point((x, 9), fill=DRED if stripe else BLACK)
        draw.point((x, 15), fill=DRED if stripe else BLACK)
        if x % 12 == 6:
            draw.point((x, 7), fill=LIGHT)
            draw.point((x, 17), fill=LIGHT)
    for centre in (18, 174):
        for y in (6, 18):
            draw.rectangle([centre - 2, y - 1, centre + 2, y + 1], fill=GRAY, outline=BLACK)
            draw.point((centre, y), fill=BRIGHT)


def end_caps(draw):
    for left in (0, FRAME_WIDTH - 5):
        draw.rectangle([left, 9, left + 4, 15], fill=GRAY, outline=BLACK)
        draw.line([(left + 1, 10), (left + 3, 10)], fill=LIGHT)
        draw.rectangle([left + 1, 12, left + 3, 13], fill=RED)
        draw.point((left + 2, 12), fill=BRIGHT)


LETTERS = {
    "T": ["11111", "00100", "00100", "00100", "00100"],
    "N": ["10001", "11001", "10101", "10011", "10001"],
}


def emblem(draw):
    cx = FRAME_WIDTH // 2
    left, right = cx - 12, cx + 11
    # Clamps joining the TNT block to the rails, with spikes
    for side in (-1, 1):
        edge = left - 1 if side < 0 else right + 1
        far = edge + side * 7
        draw.rectangle([min(edge, far), 8, max(edge, far), 16], fill=DARK, outline=BLACK)
        draw.line([(edge + side * 1, 9), (far - side * 1, 9)], fill=GRAY)
        draw.line([(edge + side * 1, 15), (far - side * 1, 15)], fill=GRAY)
        spike = far + side * 3
        draw.polygon([(far, 8), (spike, 5), (far, 11)], fill=GRAY, outline=BLACK)
        draw.polygon([(far, 16), (spike, 19), (far, 13)], fill=GRAY, outline=BLACK)
        draw.point((far - side * 3, 12), fill=BRIGHT)
    # Horns
    draw.polygon([(left + 1, 6), (left - 5, 1), (left - 2, 0), (left + 6, 4)], fill=GRAY, outline=BLACK)
    draw.polygon([(right - 1, 6), (right + 5, 1), (right + 2, 0), (right - 6, 4)], fill=GRAY, outline=BLACK)
    draw.line([(left - 3, 1), (left + 3, 4)], fill=LIGHT)
    draw.line([(right + 3, 1), (right - 3, 4)], fill=LIGHT)
    # TNT block body with shaded faces
    draw.rectangle([left, 5, right, 21], fill=BLACK)
    draw.rectangle([left + 1, 6, right - 1, 20], fill=RED)
    draw.rectangle([left + 1, 6, right - 1, 7], fill=BRIGHT)
    draw.rectangle([left + 1, 8, right - 1, 8], fill=DRED)
    draw.rectangle([left + 1, 18, right - 1, 20], fill=DRED)
    for x in range(left + 1, right):
        if (x - left) % 6 == 3:
            draw.point((x, 6), fill=LIGHT)
    draw.rectangle([left + 1, 6, left + 2, 20], fill=shade(RED, 1.25))
    draw.rectangle([right - 2, 6, right - 1, 20], fill=shade(DRED, 0.8))
    # Gray label band with black TNT letters and bolts
    draw.rectangle([left + 1, 9, right - 1, 17], fill=BLACK)
    draw.rectangle([left + 2, 10, right - 2, 16], fill=LIGHT)
    draw.line([(left + 2, 16), (right - 2, 16)], fill=GRAY)
    x = cx - 10
    for letter in "TNT":
        for dy, line in enumerate(LETTERS[letter]):
            for dx, bit in enumerate(line):
                if bit == "1":
                    draw.point((x + dx, 11 + dy), fill=BLACK)
        x += 7
    for bx, by in ((left + 3, 13), (right - 3, 13)):
        draw.point((bx, by), fill=DARK)
    for bx, by in ((left + 2, 6), (right - 2, 6), (left + 2, 20), (right - 2, 20)):
        draw.point((bx, by), fill=GRAY)
    # Stitch marks and crack (zombie-patched block)
    for sx in (left + 6, right - 6):
        draw.line([(sx, 19), (sx + 2, 21)], fill=BLACK)
        draw.line([(sx + 2, 19), (sx, 21)], fill=BLACK)
    draw.line([(cx - 3, 19), (cx - 1, 21), (cx, 19), (cx + 2, 21)], fill=BLACK)
    # Fuse: gray rope with a spark
    for y, x in ((3, cx), (2, cx), (1, cx + 1), (0, cx + 2)):
        draw.point((x, y), fill=GRAY)
    draw.point((cx - 1, 3), fill=LIGHT)
    draw.point((cx + 3, 0), fill=BRIGHT)
    draw.point((cx + 4, 1), fill=RED)
    draw.point((cx + 2, 1), fill=LIGHT)


def frame():
    image = Image.new("RGBA", (FRAME_WIDTH, FRAME_HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    rails(draw)
    wings(image)
    layer = Image.new("RGBA", (FRAME_WIDTH, FRAME_HEIGHT), (0, 0, 0, 0))
    layer_draw = ImageDraw.Draw(layer)
    end_caps(layer_draw)
    emblem(layer_draw)
    image.alpha_composite(layer)
    return image


def preview(frame_image, fraction=0.7):
    canvas = Image.new("RGBA", (FRAME_WIDTH, FRAME_HEIGHT), (0, 0, 0, 0))
    bar = background()
    fill = progress().crop((0, 0, int(BAR_WIDTH * fraction), BAR_HEIGHT))
    bar.paste(fill, (0, 0))
    canvas.paste(bar, (BAR_LEFT, BAR_TOP))
    canvas.alpha_composite(frame_image)
    return canvas.resize((FRAME_WIDTH * 4, FRAME_HEIGHT * 4), Image.NEAREST)


def build():
    SPRITES.mkdir(parents=True, exist_ok=True)
    FONT_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    for color in ("white", "red"):
        background().save(SPRITES / f"{color}_background.png")
        progress().save(SPRITES / f"{color}_progress.png")
    frame_image = frame()
    frame_image.save(FONT_TEXTURE)
    preview(frame_image).save(PREVIEW)


if __name__ == "__main__":
    build()
    print(f"Wrote boss bar art to {SPRITES} and {FONT_TEXTURE}")
