from pathlib import Path
from PIL import Image
import sys

root = Path(sys.argv[1])
js = root / "www/app.js"
s = js.read_text(encoding="utf-8")

vehicles = """  const VEHICLES = [
    { id:'traktor', name:'Türkiz traktor', asset:'assets/vehicles/traktor.png', selectorVw:48, selectorMaxH:50 },
    { id:'villamos', name:'Sárga villamos', asset:'assets/vehicles/villamos.png', selectorVw:76, selectorMaxH:44 },
    { id:'hev', name:'Zöld HÉV', asset:'assets/vehicles/hev.png', selectorVw:78, selectorMaxH:44 },
    { id:'hokotro', name:'Narancssárga hókotró', asset:'assets/vehicles/hokotro.png', selectorVw:58, selectorMaxH:48 },
    { id:'pickup', name:'Barna pickup', asset:'assets/vehicles/pickup.png', selectorVw:46, selectorMaxH:43 },
    { id:'police-suv', name:'SUV rendőrautó', asset:'assets/vehicles/police-suv.png', selectorVw:48, selectorMaxH:43 },
    { id:'police-sedan', name:'Szedán rendőrautó', asset:'assets/vehicles/police-sedan.png', selectorVw:47, selectorMaxH:41 },
    { id:'blue-sedan', name:'Kék szedán', asset:'assets/vehicles/blue-sedan.png', selectorVw:46, selectorMaxH:41 },
    { id:'kiss', name:'KISS vonat', asset:'assets/vehicles/kiss.png', selectorVw:84, selectorMaxH:39 },
    { id:'firetruck', name:'Tűzoltóautó', asset:'assets/vehicles/firetruck.png', selectorVw:60, selectorMaxH:46 },
    { id:'ambulance', name:'Mentőautó', asset:'assets/vehicles/ambulance.png', selectorVw:54, selectorMaxH:45 },
    { id:'black-offroad', name:'Fekete terepjáró', asset:'assets/vehicles/black-offroad.png', selectorVw:49, selectorMaxH:47 },
    { id:'white-suv', name:'Fehér SUV', asset:'assets/vehicles/white-suv.png', selectorVw:48, selectorMaxH:42 },
    { id:'pink-cabrio', name:'Rózsaszín kabrió', asset:'assets/vehicles/pink-cabrio.png', selectorVw:48, selectorMaxH:39 },
    { id:'blue-bus', name:'Kék busz', asset:'assets/vehicles/blue-bus.png', selectorVw:78, selectorMaxH:42 },
    { id:'yellow-truck', name:'Sárga kamion', asset:'assets/vehicles/yellow-truck.png', selectorVw:52, selectorMaxH:49 },
    { id:'grey-truck', name:'Szürke teherautó', asset:'assets/vehicles/grey-truck.png', selectorVw:64, selectorMaxH:44 },
    { id:'crane-truck', name:'Daruskocsi', asset:'assets/vehicles/crane-truck.png', selectorVw:72, selectorMaxH:46 },
    { id:'microcar', name:'Mopedautó', asset:'assets/vehicles/microcar.png', selectorVw:34, selectorMaxH:39 },
    { id:'tow-truck', name:'Autómentő', asset:'assets/vehicles/tow-truck.png', selectorVw:68, selectorMaxH:42 },
    { id:'garbage-truck', name:'Kukásautó', asset:'assets/vehicles/garbage-truck.png', selectorVw:64, selectorMaxH:47 },
    { id:'excavator', name:'Markoló', asset:'assets/vehicles/excavator.png', selectorVw:58, selectorMaxH:49 },
    { id:'monster-truck', name:'Monsterautó', asset:'assets/vehicles/monster-truck.png', selectorVw:49, selectorMaxH:50 },
    { id:'mixer', name:'Mixerautó', asset:'assets/vehicles/mixer.png', selectorVw:72, selectorMaxH:46 },
    { id:'street-sweeper', name:'Utcatakarító', asset:'assets/vehicles/street-sweeper.png', selectorVw:50, selectorMaxH:48 },
    { id:'forklift', name:'Targonca', asset:'assets/vehicles/forklift.png', selectorVw:38, selectorMaxH:48 },
    { id:'sportscar', name:'Sportkocsi', asset:'assets/vehicles/sportscar.png', selectorVw:46, selectorMaxH:36 },
    { id:'camper', name:'Lakóautó', asset:'assets/vehicles/camper.png', selectorVw:60, selectorMaxH:47 },
    { id:'wagon', name:'Kombi', asset:'assets/vehicles/wagon.png', selectorVw:50, selectorMaxH:39 },
    { id:'combine', name:'Kombájn', asset:'assets/vehicles/combine.png', selectorVw:84, selectorMaxH:48 }
  ];"""

start = s.index("  const VEHICLES = [")
end = s.index("  ];", start) + 4
s = s[:start] + vehicles + s[end:]

needle = "      btn.setAttribute('aria-label', v.name);\n      btn.innerHTML = `<img src=\"${v.asset}\" alt=\"\" draggable=\"false\">`;"
replacement = "      btn.setAttribute('aria-label', v.name);\n      btn.style.setProperty('--vehicle-vw', `${v.selectorVw || 56}vw`);\n      btn.style.setProperty('--vehicle-maxh', `${v.selectorMaxH || 48}vh`);\n      btn.innerHTML = `<img src=\"${v.asset}\" alt=\"\" draggable=\"false\">`;"
if needle in s:
    s = s.replace(needle, replacement, 1)
elif "--vehicle-vw" not in s:
    raise RuntimeError("Carousel injection point not found")

js.write_text(s, encoding="utf-8")

build = root / "app/build.gradle"
s = build.read_text(encoding="utf-8")
s = s.replace("versionCode 7", "versionCode 8").replace("versionName '1.6'", "versionName '2.0'")
build.write_text(s, encoding="utf-8")

for p in (root / "www/assets/vehicles").glob("*.png"):
    im = Image.open(p).convert("RGBA")
    alpha = im.getchannel("A")
    bbox = alpha.getbbox()
    if bbox:
        l,t,r,b = bbox
        pad = 6
        l=max(0,l-pad); t=max(0,t-pad); r=min(im.width,r+pad); b=min(im.height,b+pad)
        im = im.crop((l,t,r,b))
    im.thumbnail((1200,800), Image.Resampling.LANCZOS)
    im.save(p)
