from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "automoso-android")

p = root / "app/build.gradle"
s = p.read_text()
s = s.replace("versionCode 5", "versionCode 6")
s = s.replace("versionName '1.4'", "versionName '1.5'")
p.write_text(s)

p = root / "www/index.html"
s = p.read_text()
s = s.replace(
    'class="round-back-button" aria-label="Vissza a főmenübe"',
    'class="back-button select-back-button" aria-label="Vissza a főmenübe"'
)
p.write_text(s)

p = root / "www/app.js"
s = p.read_text()
s = s.replace(
    "const step = Math.max(340, vw * .72);",
    "const step = Math.max(340, vw);"
)
old = """const vw = carouselViewport.clientWidth || window.innerWidth;
    card.style.transform = `translate(calc(-50% + ${vw * 1.18}px), -50%) scale(.93)`;
    carouselPrompt.innerHTML = '<span>✨</span> Irány a mosó!';
    await sleep(720);"""
new = """const vw = window.innerWidth || carouselViewport.clientWidth || 1600;
    const cardWidth = card.getBoundingClientRect().width || vw * .62;
    const exitPx = (vw / 2) + (cardWidth / 2) + 80;
    card.style.transform = `translate(calc(-50% + ${exitPx}px), -50%) scale(.93)`;
    await sleep(880);"""
if old not in s:
    raise SystemExit("Expected v1.4 departure block not found")
s = s.replace(old, new)
s = s.replace("const centerX = 635;", "const centerX = W / 2;")
p.write_text(s)
