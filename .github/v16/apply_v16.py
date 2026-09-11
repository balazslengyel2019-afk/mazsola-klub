from pathlib import Path
import sys

root = Path(sys.argv[1])
html = root / "www/index.html"
s = html.read_text(encoding="utf-8")
start = s.find('        <div class="about-links">')
if start != -1:
    end = s.find('        </div>', start)
    if end != -1:
        s = s[:start] + s[end + len('        </div>\n'):]
html.write_text(s, encoding="utf-8")

js = root / "www/app.js"
s = js.read_text(encoding="utf-8")
s = s.replace("card.style.transform = `translate(calc(-50% + ${x}px), -50%) scale(${scale})`;", "card.style.transform = `translateX(calc(-50% + ${x}px)) scale(${scale})`;" )
s = s.replace("card.style.transform = `translate(calc(-50% + ${exitPx}px), -50%) scale(.93)`;", "card.style.transform = `translateX(calc(-50% + ${exitPx}px)) scale(.93)`;" )
js.write_text(s, encoding="utf-8")

build = root / "app/build.gradle"
s = build.read_text(encoding="utf-8").replace("versionCode 6", "versionCode 7").replace("versionName '1.5'", "versionName '1.6'")
build.write_text(s, encoding="utf-8")

manifest = root / "app/src/main/AndroidManifest.xml"
s = manifest.read_text(encoding="utf-8")
if 'android:roundIcon=' not in s:
    s = s.replace('android:icon="@drawable/app_icon"', 'android:icon="@drawable/app_icon"\n        android:roundIcon="@drawable/app_icon"')
manifest.write_text(s, encoding="utf-8")

# Tight-crop transparent margins from vehicle artwork so carousel wheels align to the scene baseline.
from PIL import Image, ImageDraw
for p in (root / "www/assets/vehicles").glob("*.png"):
    im = Image.open(p).convert("RGBA")
    bbox = im.getbbox()
    if bbox:
        l,t,r,b = bbox
        pad = 4
        l=max(0,l-pad); t=max(0,t-pad); r=min(im.width,r+pad); b=min(im.height,b+pad)
        im.crop((l,t,r,b)).save(p)

# Launcher icon safety disc + optical one-pixel upward correction.
icon = root / "www/assets/app-icon.png"
src = Image.open(icon).convert("RGBA")
bbox = src.getbbox()
if bbox:
    badge = src.crop(bbox)
    scale = 506 / max(badge.width, badge.height)
    badge = badge.resize((round(badge.width*scale), round(badge.height*scale)), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (512,512), (0,0,0,0))
    draw = ImageDraw.Draw(canvas)
    draw.ellipse((-10,-10,522,522), fill=(246,173,14,255))
    x=(512-badge.width)//2
    y=(512-badge.height)//2 - 1
    canvas.alpha_composite(badge,(x,y))
    canvas.save(icon)
