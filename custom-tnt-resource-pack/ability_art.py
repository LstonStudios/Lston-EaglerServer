"""Detailed 32px ability icons.

Each icon is a stack of layers drawn with PIL at 4x and reduced to a hard-edged 32px mask. Shaded layers get a
lit top/left edge, a darker bottom/right edge, a vertical gradient and a little noise (like the TNT icons);
flat layers are drawn as-is for fine detail. A dark outline is added around the finished shape.
"""

import math
from PIL import Image, ImageDraw

S = 4  # supersample
N = 32
INK = (24, 18, 34)


def _noise(x, y, seed=0):
    v = math.sin(x * 12.9898 + y * 78.233 + seed * 37.719) * 43758.5453
    return v - math.floor(v)


class Icon:
    def __init__(self):
        self.px = [[None] * N for _ in range(N)]

    def _mask(self, draw):
        im = Image.new("L", (N * S, N * S), 0)
        d = ImageDraw.Draw(im)
        draw(d, lambda v: [c * S for c in v] if isinstance(v, (list, tuple)) else v * S)
        small = im.resize((N, N), Image.BOX)
        return [[small.getpixel((x, y)) >= 128 for x in range(N)] for y in range(N)]

    def layer(self, draw, color, shaded=True, seed=1):
        m = self._mask(draw)
        ys = [y for y in range(N) for x in range(N) if m[y][x]]
        if not ys:
            return self
        y0, y1 = min(ys), max(ys)
        h = max(1, y1 - y0)

        def on(x, y):
            return 0 <= x < N and 0 <= y < N and m[y][x]

        for y in range(N):
            for x in range(N):
                if not m[y][x]:
                    continue
                if not shaded:
                    self.px[y][x] = tuple(color)
                    continue
                k = 1.2 - 0.45 * (y - y0) / h
                if not on(x, y - 1):
                    k *= 1.32
                if not on(x - 1, y):
                    k *= 1.12
                if not on(x, y + 1) or not on(x + 1, y):
                    k *= 0.78
                k *= 0.96 + 0.08 * _noise(x, y, seed)
                self.px[y][x] = tuple(max(0, min(255, int(c * k))) for c in color)
        return self

    def image(self):
        out = [[(0, 0, 0, 0)] * N for _ in range(N)]
        for y in range(N):
            for x in range(N):
                if self.px[y][x] is not None:
                    out[y][x] = self.px[y][x] + (255,)
                elif any(
                    0 <= y + dy < N and 0 <= x + dx < N and self.px[y + dy][x + dx] is not None
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                ):
                    out[y][x] = INK + (255,)
        return out


def _star(d, p, cx, cy, r, color):
    d.polygon(p([cx, cy - r, cx + r * .3, cy - r * .3, cx + r, cy, cx + r * .3, cy + r * .3, cx, cy + r,
                 cx - r * .3, cy + r * .3, cx - r, cy, cx - r * .3, cy - r * .3]), fill=255)


def pyroclastic():
    """Pyroclastic Smash: a huge molten fist smashing down, with an eruption of fire and rock."""
    i = Icon()
    # eruption behind/around the fist
    i.layer(lambda d, p: d.polygon(p([16, 0, 21, 7, 26, 4, 25, 13, 31, 14, 26, 20, 28, 27, 4, 27, 6, 20, 1, 14, 7, 13, 6, 4, 11, 7]), fill=255), (225, 65, 20), seed=2)
    i.layer(lambda d, p: d.polygon(p([16, 5, 19, 11, 23, 9, 22, 16, 27, 18, 23, 23, 24, 27, 8, 27, 9, 23, 5, 18, 10, 16, 9, 9, 13, 11]), fill=255), (255, 150, 30), seed=3)
    # fist: palm block + four knuckles + thumb
    i.layer(lambda d, p: d.rectangle(p([9, 11, 23, 23]), fill=255), (205, 120, 70), seed=4)
    for x in (9, 12.5, 16, 19.5):
        i.layer(lambda d, p, x=x: d.rectangle(p([x, 9, x + 3.5, 13]), fill=255), (225, 140, 85), seed=int(x))
    i.layer(lambda d, p: d.line(p([12.5, 11, 12.5, 17, 16, 11, 16, 17, 19.5, 11, 19.5, 17]), fill=255, width=S // 2), (140, 70, 40), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([9, 17, 23, 18]), fill=255), (140, 70, 40), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([5, 15, 10, 21]), fill=255), (215, 130, 78), seed=9)
    # cracked ground
    i.layer(lambda d, p: d.polygon(p([0, 26, 32, 26, 32, 32, 0, 32]), fill=255), (84, 62, 60), seed=10)
    i.layer(lambda d, p: d.line(p([16, 26, 12, 29, 8, 28, 4, 31]), fill=255, width=S), (255, 150, 30), shaded=False)
    i.layer(lambda d, p: d.line(p([16, 26, 20, 29, 25, 28, 29, 31]), fill=255, width=S), (255, 150, 30), shaded=False)
    i.layer(lambda d, p: d.line(p([16, 26, 16, 31]), fill=255, width=S), (255, 215, 90), shaded=False)
    for x, y in ((3, 8), (28, 9), (14, 2), (2, 22)):
        i.layer(lambda d, p, x=x, y=y: d.rectangle(p([x, y, x + 2, y + 2]), fill=255), (92, 66, 62), seed=x)
    return i.image()


def void():
    """Void Tether: two void orbs joined by a chain."""
    i = Icon()
    pts = [(7 + (25 - 7) * t / 5, 25 + (7 - 25) * t / 5) for t in range(6)]
    for k, (x, y) in enumerate(pts[1:-1]):
        horiz = k % 2 == 0
        box = [x - 3.4, y - 2.2, x + 3.4, y + 2.2] if horiz else [x - 2.2, y - 3.4, x + 2.2, y + 3.4]
        i.layer(lambda d, p, b=box: d.ellipse(p(b), fill=255), (215, 195, 255), seed=k)
        inner = [x - 1.9, y - 0.8, x + 1.9, y + 0.8] if horiz else [x - 0.8, y - 1.9, x + 0.8, y + 1.9]
        i.layer(lambda d, p, b=inner: d.ellipse(p(b), fill=255), (30, 14, 62), shaded=False)
    for cx, cy in ((7, 25), (25, 7)):
        i.layer(lambda d, p, cx=cx, cy=cy: d.ellipse(p([cx - 6, cy - 6, cx + 6, cy + 6]), fill=255), (100, 55, 195), seed=cx)
        i.layer(lambda d, p, cx=cx, cy=cy: d.ellipse(p([cx - 4.2, cy - 4.2, cx + 4.2, cy + 4.2]), fill=255), (40, 18, 90), shaded=False)
        i.layer(lambda d, p, cx=cx, cy=cy: d.ellipse(p([cx - 2.5, cy - 2.5, cx + 2.5, cy + 2.5]), fill=255), (8, 3, 24), shaded=False)
        i.layer(lambda d, p, cx=cx, cy=cy: d.arc(p([cx - 5, cy - 5, cx + 5, cy + 5]), 200, 310, fill=255, width=S), (185, 140, 255), shaded=False)
    return i.image()


def reaper():
    """Reaper's Harvest: a big scythe with a glowing soul."""
    i = Icon()
    i.layer(lambda d, p: d.polygon(p([5, 30, 8, 31, 26, 7, 23, 5]), fill=255), (126, 80, 42), seed=6)
    i.layer(lambda d, p: d.polygon(p([6, 29, 7.5, 30, 26, 6.5, 25, 5.5]), fill=255), (176, 122, 70), shaded=False)
    # blade
    i.layer(lambda d, p: d.polygon(p([25, 5, 18, 0, 8, 1, 2, 7, 1, 15, 6, 9, 12, 6, 19, 7, 25, 10]), fill=255), (120, 134, 160), seed=7)
    i.layer(lambda d, p: d.polygon(p([23, 6.5, 17, 2.5, 9, 3, 4.5, 8, 3.5, 12, 7, 8.5, 12, 7.5, 18, 8.5, 23, 9.5]), fill=255), (214, 224, 242), shaded=False)
    i.layer(lambda d, p: d.polygon(p([21, 6, 16, 3.5, 10, 4, 7, 6.5, 12, 5.5, 17, 6.5]), fill=255), (255, 255, 255), shaded=False)
    # binding where blade meets handle
    i.layer(lambda d, p: d.rectangle(p([22, 4, 27, 8]), fill=255), (70, 48, 130), seed=8)
    # soul flame
    i.layer(lambda d, p: d.polygon(p([22, 17, 25, 21, 27, 25, 24, 29, 20, 29, 17, 25, 19, 21]), fill=255), (70, 225, 165), seed=9)
    i.layer(lambda d, p: d.polygon(p([22, 21, 24, 24, 23, 28, 20, 28, 19, 24]), fill=255), (190, 255, 225), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([20.5, 25, 21.5, 26.2]), fill=255), (20, 60, 50), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([22.6, 25, 23.6, 26.2]), fill=255), (20, 60, 50), shaded=False)
    return i.image()


def sonic():
    """Sonic Boom: a booming blast with shockwave rings."""
    i = Icon()
    for r, col in ((15, (60, 150, 220)), (11.5, (110, 205, 250))):
        i.layer(lambda d, p, r=r: (d.ellipse(p([16 - r, 16 - r, 16 + r, 16 + r]), fill=255),
                                   d.ellipse(p([16 - r + 2.2, 16 - r + 2.2, 16 + r - 2.2, 16 + r - 2.2]), fill=0)), col, seed=int(r))
    pts = []
    for k in range(16):
        a = k * math.pi / 8
        r = 9.5 if k % 2 == 0 else 5
        pts += [16 + r * math.cos(a), 16 + r * math.sin(a)]
    i.layer(lambda d, p: d.polygon(p(pts), fill=255), (255, 214, 70), seed=11)
    pts2 = []
    for k in range(16):
        a = k * math.pi / 8 + math.pi / 16
        r = 6 if k % 2 == 0 else 3
        pts2 += [16 + r * math.cos(a), 16 + r * math.sin(a)]
    i.layer(lambda d, p: d.polygon(p(pts2), fill=255), (255, 246, 190), shaded=False)
    i.layer(lambda d, p: d.ellipse(p([13.5, 13.5, 18.5, 18.5]), fill=255), (255, 255, 255), shaded=False)
    return i.image()


def quantum():
    """Quantum Shift: an atom - glowing core with orbit rings and electrons."""
    i = Icon()
    def ring(angle, col, seed):
        im = Image.new("L", (N * S, N * S), 0)
        d = ImageDraw.Draw(im)
        d.ellipse([c * S for c in (1, 9, 31, 23)], outline=255, width=S * 2)
        im = im.rotate(angle, resample=Image.BICUBIC, center=(16 * S, 16 * S))
        small = im.resize((N, N), Image.BOX)
        m = [[small.getpixel((x, y)) >= 110 for x in range(N)] for y in range(N)]
        def draw(dd, p, m=m):
            for y in range(N):
                for x in range(N):
                    if m[y][x]:
                        dd.rectangle([x * S, y * S, x * S + S - 1, y * S + S - 1], fill=255)
        i.layer(draw, col, seed=seed)
    ring(0, (120, 70, 210), 11)
    ring(60, (150, 95, 235), 12)
    ring(120, (100, 55, 185), 13)
    i.layer(lambda d, p: d.ellipse(p([9.5, 9.5, 22.5, 22.5]), fill=255), (80, 35, 150), seed=14)
    i.layer(lambda d, p: d.ellipse(p([11.5, 11.5, 20.5, 20.5]), fill=255), (150, 90, 235), seed=15)
    i.layer(lambda d, p: d.ellipse(p([13.5, 13, 18, 17.5]), fill=255), (235, 215, 255), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([14.2, 13.6, 15.6, 15]), fill=255), (255, 255, 255), shaded=False)
    for x, y in ((29, 12), (5, 25), (11, 3)):
        i.layer(lambda d, p, x=x, y=y: d.ellipse(p([x - 2, y - 2, x + 2, y + 2]), fill=255), (215, 170, 255), seed=x)
        i.layer(lambda d, p, x=x, y=y: d.rectangle(p([x - 0.8, y - 0.8, x + 0.2, y + 0.2]), fill=255), (255, 255, 255), shaded=False)
    return i.image()


def chitinous():
    """Chitinous Shield: a beetle's shell used as a shield."""
    i = Icon()
    for y in (12, 18, 24):
        for sx in (1, -1):
            i.layer(lambda d, p, y=y, sx=sx: d.line(p([16 + sx * 10, y, 16 + sx * 14.5, y + 2.5]), fill=255, width=S), (40, 70, 60), shaded=False)
    i.layer(lambda d, p: d.line(p([13, 5, 10, 1]), fill=255, width=S // 2), (60, 100, 85), shaded=False)
    i.layer(lambda d, p: d.line(p([19, 5, 22, 1]), fill=255, width=S // 2), (60, 100, 85), shaded=False)
    i.layer(lambda d, p: d.ellipse(p([12, 3, 20, 10]), fill=255), (50, 90, 78), seed=12)
    i.layer(lambda d, p: d.ellipse(p([5, 7, 27, 31]), fill=255), (60, 140, 115), seed=13)
    i.layer(lambda d, p: d.ellipse(p([8, 10, 24, 28]), fill=255), (95, 190, 155), seed=14)
    i.layer(lambda d, p: d.line(p([16, 8, 16, 30]), fill=255, width=S), (35, 85, 70), shaded=False)
    for y in (15, 21):
        i.layer(lambda d, p, y=y: d.arc(p([7, y - 7, 25, y + 7]), 20, 160, fill=255, width=S // 2), (40, 100, 82), shaded=False)
    i.layer(lambda d, p: d.arc(p([9, 12, 17, 22]), 200, 280, fill=255, width=S), (225, 252, 240), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([11, 12, 12.5, 14]), fill=255), (255, 255, 255), shaded=False)
    return i.image()


def overclock():
    """Overclock: a gauge pushed into the red, with a lightning bolt."""
    i = Icon()
    i.layer(lambda d, p: d.ellipse(p([2, 2, 30, 30]), fill=255), (150, 100, 45), seed=14)
    i.layer(lambda d, p: d.ellipse(p([5, 5, 27, 27]), fill=255), (36, 28, 34), shaded=False)
    i.layer(lambda d, p: d.arc(p([6, 6, 26, 26]), 280, 20, fill=255, width=S * 2), (235, 55, 40), shaded=False)
    i.layer(lambda d, p: d.arc(p([6, 6, 26, 26]), 200, 280, fill=255, width=S * 2), (250, 200, 50), shaded=False)
    i.layer(lambda d, p: d.arc(p([6, 6, 26, 26]), 130, 200, fill=255, width=S * 2), (110, 220, 110), shaded=False)
    i.layer(lambda d, p: d.ellipse(p([9, 9, 23, 23]), fill=255), (36, 28, 34), shaded=False)
    for k in range(9):
        a = math.radians(140 + k * 32)
        i.layer(lambda d, p, a=a: d.line(p([16 + 8.5 * math.cos(a), 16 + 8.5 * math.sin(a), 16 + 10.5 * math.cos(a), 16 + 10.5 * math.sin(a)]),
                                         fill=255, width=S // 2), (235, 225, 210), shaded=False)
    i.layer(lambda d, p: d.polygon(p([19, 5, 10, 17, 15, 17, 13, 28, 23, 14, 17, 14]), fill=255), (255, 195, 30), seed=16)
    i.layer(lambda d, p: d.polygon(p([18.5, 8, 13, 16, 16.5, 16, 15, 23, 20.5, 15, 16.5, 15]), fill=255), (255, 245, 160), shaded=False)
    return i.image()


def aegis():
    """Aegis Burst: a shield exploding outward in a burst of light."""
    i = Icon()
    pts = []
    for k in range(24):
        a = k * math.pi / 12
        r = 15.5 if k % 2 == 0 else 10
        pts += [16 + r * math.cos(a), 16 + r * math.sin(a)]
    i.layer(lambda d, p: d.polygon(p(pts), fill=255), (255, 226, 110), seed=17)
    i.layer(lambda d, p: d.ellipse(p([5, 5, 27, 27]), fill=255), (255, 246, 190), shaded=False)
    i.layer(lambda d, p: d.polygon(p([16, 5, 24, 7, 24, 16, 21, 22, 16, 28, 11, 22, 8, 16, 8, 7]), fill=255), (205, 140, 25), seed=18)
    i.layer(lambda d, p: d.polygon(p([16, 8, 21.5, 9.5, 21.5, 16, 19.5, 20.5, 16, 24.5, 12.5, 20.5, 10.5, 16, 10.5, 9.5]), fill=255), (255, 210, 70), seed=19)
    i.layer(lambda d, p: d.rectangle(p([14.7, 10, 17.3, 22]), fill=255), (255, 252, 225), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([11.5, 13, 20.5, 15.6]), fill=255), (255, 252, 225), shaded=False)
    return i.image()


def gale():
    """Gale Force: a tornado that launches you upward."""
    i = Icon()
    bands = [(3, 6, 29, 11), (6, 11, 26, 16), (9, 16, 23, 21), (12, 21, 20, 26), (14, 25, 18, 30)]
    for n, (x0, y0, x1, y1) in enumerate(bands):
        col = (120, 178, 228) if n % 2 == 0 else (160, 210, 245)
        i.layer(lambda d, p, b=(x0, y0, x1, y1): d.ellipse(p([b[0], b[1], b[2], b[3] + 2.5]), fill=255), col, seed=20 + n)
    for n, (x0, y0, x1, y1) in enumerate(bands[:4]):
        i.layer(lambda d, p, b=(x0, y0, x1, y1): d.arc(p([b[0] + 2, b[1] + 1, b[2] - 2, b[3] + 2]), 20, 150, fill=255, width=S // 2),
                (245, 252, 255), shaded=False)
    i.layer(lambda d, p: d.polygon(p([16, 0, 20.5, 5, 17.5, 5, 17.5, 8, 14.5, 8, 14.5, 5, 11.5, 5]), fill=255), (255, 255, 255), seed=25)
    for x, y in ((3, 18), (28, 20), (5, 26)):
        i.layer(lambda d, p, x=x, y=y: d.polygon(p([x, y, x + 3, y - 1.5, x + 2, y + 1.5]), fill=255), (110, 190, 110), seed=x)
    return i.image()


def shadow():
    """A player: normal on the left half, fading to a black, smoky shadow on the right half."""
    i = Icon()
    left = lambda d, p, box: d.rectangle(p(box), fill=255)

    # normal half (x < 16)
    i.layer(lambda d, p: left(d, p, [10, 3, 16, 14]), (222, 170, 128), seed=30)           # face
    i.layer(lambda d, p: left(d, p, [10, 2, 16, 6]), (92, 58, 30), seed=31)               # hair
    i.layer(lambda d, p: left(d, p, [11, 8, 13, 10]), (255, 255, 255), shaded=False)      # eye white
    i.layer(lambda d, p: left(d, p, [12, 8.5, 13.5, 10.5]), (70, 90, 200), shaded=False)  # pupil
    i.layer(lambda d, p: left(d, p, [8, 15, 16, 24]), (50, 120, 200), seed=32)            # shirt
    i.layer(lambda d, p: left(d, p, [5, 15, 8, 24]), (222, 170, 128), seed=33)            # arm
    i.layer(lambda d, p: left(d, p, [10, 24, 16, 30]), (60, 62, 150), seed=34)            # legs

    # shadow half (x >= 16)
    i.layer(lambda d, p: left(d, p, [16, 3, 22, 14]), (28, 20, 44), seed=35)
    i.layer(lambda d, p: left(d, p, [16, 2, 22, 6]), (14, 10, 24), seed=36)
    i.layer(lambda d, p: left(d, p, [16, 15, 24, 24]), (22, 16, 36), seed=37)
    i.layer(lambda d, p: left(d, p, [24, 15, 27, 24]), (30, 22, 48), seed=38)
    i.layer(lambda d, p: left(d, p, [16, 24, 22, 30]), (16, 12, 28), seed=39)
    i.layer(lambda d, p: left(d, p, [18, 8, 20.5, 9.8]), (190, 120, 255), shaded=False)   # glowing eye
    i.layer(lambda d, p: left(d, p, [18.8, 8.4, 19.7, 9.4]), (255, 240, 255), shaded=False)

    # smoke curling off the shadow side
    for pts in ([22, 3, 26, 1, 28, 5, 24, 7], [24, 11, 29, 9, 30, 14, 26, 16], [25, 19, 30, 18, 31, 24, 27, 25],
                [22, 26, 27, 25, 28, 30, 23, 30]):
        i.layer(lambda d, p, pts=pts: d.polygon(p(pts), fill=255), (60, 38, 110), seed=40 + pts[0])
    for pts in ([25, 2.5, 27, 3, 26, 4.5], [27, 10.5, 29, 11, 28.5, 13], [28, 20, 30, 21, 29.5, 23]):
        i.layer(lambda d, p, pts=pts: d.polygon(p(pts), fill=255), (150, 105, 230), shaded=False)
    # fade line between the two halves
    i.layer(lambda d, p: d.line(p([16, 3, 16, 30]), fill=255, width=S // 2), (120, 80, 170), shaded=False)
    return i.image()


def tailwind():
    """Tailwind: a winged boot with wind rushing behind it."""
    i = Icon()
    for y, x0 in ((19, 1), (24, 3), (29, 1)):
        i.layer(lambda d, p, y=y, x0=x0: d.line(p([x0, y, x0 + 8, y]), fill=255, width=S // 2 * 2), (170, 230, 180), shaded=False)
    i.layer(lambda d, p: d.polygon(p([12, 11, 4, 3, 5, 9, 1, 11, 5, 14, 2, 18, 12, 16]), fill=255), (235, 248, 238), seed=30)
    i.layer(lambda d, p: d.polygon(p([11, 12, 5, 6, 6, 11, 3, 13, 6, 15, 4, 17, 11, 15]), fill=255), (255, 255, 255), shaded=False)
    i.layer(lambda d, p: d.line(p([11, 12, 4, 6]), fill=255, width=S // 2), (140, 205, 160), shaded=False)
    i.layer(lambda d, p: d.polygon(p([12, 5, 21, 5, 21, 18, 29, 21, 29, 27, 12, 27]), fill=255), (60, 160, 85), seed=31)
    i.layer(lambda d, p: d.polygon(p([12, 5, 21, 5, 21, 8, 12, 8]), fill=255), (190, 245, 200), seed=32)
    i.layer(lambda d, p: d.rectangle(p([12, 25, 29, 28]), fill=255), (60, 45, 40), seed=33)
    i.layer(lambda d, p: d.line(p([14, 12, 20, 12]), fill=255, width=S // 2), (30, 100, 55), shaded=False)
    i.layer(lambda d, p: d.rectangle(p([13, 6, 14, 7.5]), fill=255), (255, 255, 255), shaded=False)
    return i.image()


ICONS = {
    "pyroclastic": pyroclastic, "void": void, "reaper": reaper, "sonic": sonic, "quantum": quantum,
    "chitinous": chitinous, "overclock": overclock, "aegis": aegis, "gale": gale, "shadow": shadow,
    "tailwind": tailwind,
}
