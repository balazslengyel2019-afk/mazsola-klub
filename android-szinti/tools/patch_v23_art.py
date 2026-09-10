from PIL import Image, ImageDraw, ImageFilter
import math, os

OUT = 'app/src/main/res/drawable-nodpi'
os.makedirs(OUT, exist_ok=True)

# Generic child-friendly steel tongue drum icon, based on the visual idea of a top-view tongue drum.
W, H = 360, 250
im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(im)

cx, cy, r = 180, 124, 100
# soft shadow
d.ellipse((cx-r+7, cy-r+13, cx+r+7, cy+r+13), fill=(0, 0, 0, 55))
# metallic body with concentric shading
for rr in range(r, 12, -2):
    t = rr / r
    col = (
        int(22 + 18 * (1-t)),
        int(100 + 48 * (1-t)),
        int(93 + 40 * (1-t)),
        255
    )
    d.ellipse((cx-rr, cy-rr, cx+rr, cy+rr), fill=col)
d.ellipse((cx-r, cy-r, cx+r, cy+r), outline=(18, 50, 49, 255), width=6)
d.ellipse((cx-r+8, cy-r+8, cx+r-8, cy+r-8), outline=(93, 188, 165, 170), width=3)

# central tongue
d.rounded_rectangle((cx-28, cy-34, cx+28, cy+34), 18, outline=(10, 47, 44, 255), width=5)

# surrounding tongues
for idx, a in enumerate([-90, -45, 0, 45, 90, 135, 180, 225]):
    rad = math.radians(a)
    tx = cx + math.cos(rad) * 62
    ty = cy + math.sin(rad) * 62
    length = 50
    width = 29
    # draw a capsule-like tongue aligned radially using a small temporary layer
    layer = Image.new('RGBA', (90, 90), (0, 0, 0, 0))
    q = ImageDraw.Draw(layer)
    q.rounded_rectangle((31, 10, 59, 65), 14, outline=(9, 45, 42, 255), width=5)
    # leave the lower end open to resemble a cut tongue
    q.rectangle((28, 54, 62, 72), fill=(0, 0, 0, 0))
    layer = layer.rotate(-(a + 90), resample=Image.Resampling.BICUBIC, expand=False)
    im.alpha_composite(layer, (int(tx-45), int(ty-45)))
    # tiny friendly note number marker
    d.ellipse((tx-8, ty-8, tx+8, ty+8), fill=(230, 242, 217, 235))

# center highlight and small musical sparkle
d.ellipse((cx-8, cy-8, cx+8, cy+8), fill=(235, 242, 220, 240))
d.line((70, 58, 85, 58), fill=(255, 226, 84, 230), width=5)
d.line((77, 50, 77, 66), fill=(255, 226, 84, 230), width=5)
d.line((282, 176, 297, 176), fill=(255, 226, 84, 230), width=5)
d.line((289, 168, 289, 184), fill=(255, 226, 84, 230), width=5)

im = im.filter(ImageFilter.GaussianBlur(0.18))
im.save(os.path.join(OUT, 'inst_tonguedrum.png'), optimize=True)
