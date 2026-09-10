from PIL import Image, ImageDraw, ImageFilter
import random, os

random.seed(21)
path = 'app/src/main/res/drawable-nodpi/season_bg.png'
im = Image.open(path).convert('RGB')
d = ImageDraw.Draw(im)

# Cover the old hedgehog area with a natural autumn shrub/tree cluster.
# This keeps the seasonal background as a single replaceable asset.
# Ground/back foliage
for cx, cy, rx, ry, col in [
    (2220, 700, 260, 210, (92, 158, 58)),
    (2325, 650, 230, 220, (105, 172, 61)),
    (2140, 760, 210, 150, (83, 149, 55)),
]:
    d.ellipse((cx-rx, cy-ry, cx+rx, cy+ry), fill=col)

# Small autumn tree on the far right
trunk = (104, 64, 35)
d.polygon([(2290, 835), (2360, 835), (2385, 505), (2320, 480), (2260, 560)], fill=trunk)
palette = [(232,74,27),(247,111,24),(255,157,29),(244,188,38),(211,58,28)]
for _ in range(46):
    x = random.randint(2140, 2440)
    y = random.randint(410, 650)
    r = random.randint(38, 82)
    d.ellipse((x-r, y-r, x+r, y+r), fill=random.choice(palette))

# A few foreground leaves for blending
for _ in range(35):
    x = random.randint(2060, 2399)
    y = random.randint(690, 900)
    r = random.randint(7, 18)
    col = random.choice(palette)
    d.ellipse((x-r, y-r//2, x+r, y+r//2), fill=col)

im = im.filter(ImageFilter.GaussianBlur(0.25))
im.save(path, optimize=True)
