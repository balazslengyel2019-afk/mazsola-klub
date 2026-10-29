from pathlib import Path
from PIL import Image, ImageDraw

root=Path("android-hogolyocsata/app/src/main/assets/game")
j=root/"game.js"; c=root/"styles.css"; h=root/"index.html"
js=j.read_text(encoding="utf-8")
def change(src,dst,tag):
 global js
 n=js.count(src)
 if n!=1: raise RuntimeError(f"{tag}: wanted one match, got {n}")
 js=js.replace(src,dst,1)
change(
"function reset(){unlockAudio();life=3;hits=0;shots=0;ammo=MAX_AMMO;time=0;spawnAt=.75;lastDamage=-100;shakeUntil=0;flashUntil=0;bossAt=-30;id=0;level=1;enemies=[];projectiles=[];bursts=[];reloading=false;reloadAt=0;aim.visible=false;",
"function reset(){unlockAudio();life=3;hits=0;shots=0;ammo=MAX_AMMO;time=0;spawnAt=.75;lastDamage=-100;shakeUntil=0;flashUntil=0;bossAt=-30;id=0;level=1;enemies=[];projectiles=[];bursts=[];reloading=false;reloadAt=0;aim={x:640,y:360,visible:false,touch:false,lastTouch:-10};tipUntil=0;last=performance.now();",
"new game reset")
change("function goMenu(){enemies=[];projectiles=[];bursts=[];show('menu')}",
       "function goMenu(){enemies=[];projectiles=[];bursts=[];reloading=false;ammo=MAX_AMMO;hud();show('menu')}",
       "go menu")
change("function tick(now){const dt=clamp((now-last)/1000,0,.05);last=now;ambientTime+=dt;update(dt);draw();requestAnimationFrame(tick)}requestAnimationFrame(tick);",
"""let rafId=0,lastFrameWall=performance.now(),lastDrawError='',drawErrorCount=0;
function tick(now){
 rafId=0;lastFrameWall=performance.now();
 try{
  const dt=clamp((now-last)/1000,0,.05);last=now;ambientTime+=dt;update(dt);draw();
  drawErrorCount=0;
 }catch(error){
  drawErrorCount++;lastDrawError=String(error&&(error.stack||error.message||error));
  console.error('Hógolyócsata render error',error);
  if(drawErrorCount>=4){drawErrorCount=0;enemies=[];projectiles=[];bursts=[];reloading=false;
   if(state==='game'||state==='pause')goMenu()}
 }finally{if(!rafId)rafId=requestAnimationFrame(tick)}
}
function keepRendering(){if(document.hidden)return;
 if(performance.now()-lastFrameWall>1900){if(rafId)cancelAnimationFrame(rafId);
  rafId=0;last=performance.now();tick(last)}}
rafId=requestAnimationFrame(tick);
window.setInterval(keepRendering,1200);
canvas.addEventListener('contextlost',e=>e.preventDefault());
""","render watchdog")
start=js.index("function coords(e)")
end=js.index("canvas.addEventListener('pointermove'",start)
js=js[:start]+"""function coords(e){const rect=canvas.getBoundingClientRect();
 const scale=Math.max(rect.width/W,rect.height/H);
 const cropX=(W-rect.width/scale)/2,cropY=(H-rect.height/scale)/2;
 return{x:clamp(cropX+(e.clientX-rect.left)/scale,0,W),
        y:clamp(cropY+(e.clientY-rect.top)/scale,0,H)}}
"""+js[end:]
change(
"document.addEventListener('visibilitychange',()=>{if(document.hidden&&state==='game')show('pause');if(document.hidden)music.pause();else syncMusic()});",
"document.addEventListener('visibilitychange',()=>{if(document.hidden&&state==='game')show('pause');if(document.hidden)music.pause();else{last=performance.now();keepRendering();syncMusic()}});window.addEventListener('pageshow',()=>{last=performance.now();keepRendering()});",
"resume")
change(
"projectiles:projectiles.length});",
"projectiles:projectiles.length,renderHeartbeatMs:Math.round(performance.now()-lastFrameWall),lastDrawError});",
"test diagnostics")
j.write_text(js,encoding="utf-8")

css=c.read_text(encoding="utf-8")
css+="""
/* Android 1.1: edge-to-edge WebView with proportional, center-cropped game canvas. */
html,body{width:100%!important;height:100%!important;margin:0!important;overflow:hidden!important}
body{display:block!important;background:#061939!important}
#app{position:fixed!important;inset:0!important;top:0!important;left:0!important;
 width:100vw!important;height:100vh!important;height:100dvh!important;
 max-width:none!important;max-height:none!important;aspect-ratio:auto!important;
 transform:none!important;box-shadow:none!important;border:0!important}
#stage{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;
 object-fit:cover!important;object-position:center center!important}
.menu-links{left:2.3%!important;top:23%!important;right:auto!important;
 transform:none!important;display:flex!important;flex-direction:column!important;
 align-items:flex-start!important;gap:.9cqw!important;flex-wrap:nowrap!important}
.social-icon{width:3.8cqw!important;height:3.8cqw!important;min-width:42px!important;min-height:42px!important}
.menu-actions{top:48%!important;gap:1.4cqw!important}
.playlist-pill{top:84.5%!important;left:50%!important}
.menu-hint{bottom:1.2%!important}
"""
c.write_text(css,encoding="utf-8")
html=h.read_text(encoding="utf-8")
html=html.replace('<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>', '')
html=html.replace('<link href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;700&family=Nunito:wght@700;800;900;1000&display=swap" rel="stylesheet">','')
h.write_text(html,encoding="utf-8")

# Match the other Mazsola Klub APK launcher badges: white round tile + centered smaller art.
src=Image.open(root/"assets/hogolyocsata_icon.png").convert("RGBA")
bounds=src.getchannel("A").getbbox()
assert bounds
src=src.crop(bounds).resize((344,344),Image.Resampling.LANCZOS)
icon=Image.new("RGBA",(512,512),(0,0,0,0))
ImageDraw.Draw(icon).ellipse((9,9,503,503),fill=(255,255,255,255))
icon.alpha_composite(src,(84,84))
icon.save("android-hogolyocsata/app/src/main/res/drawable-nodpi/hogoly_icon.png",optimize=True)
print("Android 1.1 game layout, animation recovery and launcher badge prepared")
