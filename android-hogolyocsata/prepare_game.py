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

# Android v1.3 — clearer enemy emergence behind trees, with no foreground-order changes.
change(
" {name:'Bal fenyő',hidden:[136,560],peek:[240,448]},",
" {name:'Bal fenyő',hidden:[136,560],peek:[260,436]},",
"left pine emergence")
change(
" {name:'Jobb fenyő',hidden:[1140,562],peek:[1072,446]},",
" {name:'Jobb fenyő',hidden:[1140,562],peek:[1054,436]},",
"right pine emergence")

# Replace font-dependent hearts with aspect-ratio-safe SVG shapes.
change(
"s.textContent='♥';h.appendChild(s)",
"""s.innerHTML='<svg viewBox="0 0 40 40" aria-hidden="true" focusable="false"><path d="M20 35 C16.5 31.5 6.5 25 3.5 20.5 C-1 13.5 4.5 5 11.5 5 C16 5 18.5 7.5 20 10.2 C21.5 7.5 24 5 28.5 5 C35.5 5 41 13.5 36.5 20.5 C33.5 25 23.5 31.5 20 35Z"/></svg>';h.appendChild(s)""",
"vector hearts")

# Reward one life for each level cleared, up to the original maximum of three.
change(
"const nowLevel=difficulty();if(nowLevel!==level){level=nowLevel;hud();tip('Új szint! Egy kicsit sűrűbb a hócsata!',2.2)}",
"""const nowLevel=difficulty();if(nowLevel>level){
 const earned=nowLevel-level,restored=Math.min(3-life,earned);
 life+=restored;level=nowLevel;hud();showLevelUp(restored);
 tip(restored>0?'Szintlépés! Visszakaptál egy életet!':'Szintlépés! 3 életből 3 megvan!',2.4)
}""",
"life regeneration on level up")

# Dedicated visual feedback for level-up, with no extra gameplay input.
level_overlay="""let levelUpTimer=null;
function showLevelUp(restored){
 let el=document.getElementById('levelUpNotice');
 if(!el){el=document.createElement('div');el.id='levelUpNotice';el.className='level-up-notice';
   el.setAttribute('aria-live','polite');$('app').appendChild(el)}
 el.classList.remove('visible');
 el.innerHTML='<strong>'+level+'. SZINT!</strong><span>'+(restored>0?'+1 ÉLET ♥':'MÁR 3 ÉLETED VAN ♥')+'</span>';
 void el.offsetWidth;
 el.classList.add('visible');
 if(levelUpTimer)clearTimeout(levelUpTimer);
 levelUpTimer=setTimeout(()=>el.classList.remove('visible'),2400);
}
"""
change("function hud(){",level_overlay+"function hud(){","level up UI insertion")
change("tipUntil=0;last=performance.now();",
       "tipUntil=0;last=performance.now();if(levelUpTimer)clearTimeout(levelUpTimer);const levelUp=document.getElementById('levelUpNotice');if(levelUp)levelUp.classList.remove('visible');",
       "clear level-up badge on restart")

# Android v1.4 — distinct peeking positions so the characters are visible above the fence and beside the trees.
# These overrides follow the v1.3 tree improvements in this build-only copy.
for old,new in [
 ("peek:[260,436]","peek:[295,417]"),
 ("peek:[1054,436]","peek:[1022,415]"),
 ("peek:[344,474]","peek:[344,446]"),
 ("peek:[520,478]","peek:[520,448]"),
 ("peek:[666,476]","peek:[666,447]"),
 ("peek:[780,478]","peek:[744,424]"),
 ("peek:[973,481]","peek:[1030,426]"),
 ("peek:[854,474]","peek:[854,441]")
]:
 change(old,new,"more visible peek "+old)

# Hit testing must use *the visible opaque pixels of the same sprite we draw*, not a displaced box.
# Only a small 18-pixel tolerance at exposed edges is allowed; thick foreground wood remains protective.
start=js.index("function impact(x,y){")
end=js.index("function reload(){",start)
original=js[start:end]
old_header=original.split(" const {e,p}=candidates[0],info=types[e.kind];",1)[0]
assert "covered(x,y)" in old_header and "cy=p.y-h*.54" in old_header
hit_source="""const spriteAlphaCache=new Map();
function visibleSpriteHit(e,p,tx,ty){
 const img=imgs[e.kind],w=types[e.kind].w,h=w*img.naturalHeight/img.naturalWidth;
 const left=p.x-w/2,top=p.y-h/2;
 if(tx<left-20||tx>left+w+20||ty<top-20||ty>top+h+20)return false;
 let entry=spriteAlphaCache.get(e.kind);
 if(!entry||entry.width!==img.naturalWidth||entry.height!==img.naturalHeight){
  const layer=document.createElement('canvas');layer.width=img.naturalWidth;layer.height=img.naturalHeight;
  const layerCtx=layer.getContext('2d',{willReadFrequently:true});layerCtx.drawImage(img,0,0);
  entry=layerCtx.getImageData(0,0,layer.width,layer.height);
  spriteAlphaCache.set(e.kind,entry);
 }
 const offsets=[[0,0],[12,0],[-12,0],[0,12],[0,-12],[10,10],[-10,10],[10,-10],[-10,-10],[18,0],[-18,0],[0,18],[0,-18]];
 for(const [dx,dy] of offsets){
  const xx=tx+dx, yy=ty+dy;
  if(xx<left||xx>=left+w||yy<top||yy>=top+h||covered(xx,yy))continue;
  const ix=Math.floor((xx-left)/w*entry.width), iy=Math.floor((yy-top)/h*entry.height);
  if(entry.data[(iy*entry.width+ix)*4+3]>55)return true;
 }
 return false;
}
function impact(x,y){const candidates=[];
 for(const e of enemies){
  if(e.status!=='active')continue;
  const p=getPosition(e),im=imgs[e.kind];if(!im||!im.complete||!im.naturalWidth||p.visibility<.70)continue;
  if(visibleSpriteHit(e,p,x,y))candidates.push({e,p,delta:distance(x,y,p.x,p.y)});
 }
 candidates.sort((a,b)=>a.delta-b.delta);
 if(!candidates.length){addBurst(x,y,'ground');playFx('puff',.28);return}
"""
js=js[:start]+hit_source+original[len(old_header):].replace(" const {e,p}=candidates[0],info=types[e.kind];"," const {e,p}=candidates[0],info=types[e.kind];",1)+js[end:]
# Note: replaced only the header, retaining effects, sound, multi-hit logic, and the subsequent reload() function.


# Android v1.5: 20 progressively harder levels. These changes apply only to the APK copy.
change("function difficulty(){return Math.min(6,1+Math.floor(hits/8))}",
       "function difficulty(){return Math.min(20,1+Math.floor(hits/8))}",
       "twenty levels")

# Level 1 HP1 uses original 3.5–4.5s first shot / 4.8–6.7s repeat.
# HP2 shoots 0.5s earlier and HP3 shoots 1.0s earlier, at ALL levels.
# At level 20, intensity rises through spawn rate, simultaneity, shot velocity and attack interval.
new_difficulty_helpers="""function levelProgress(){return (level-1)/19}
function activeLimit(){return level===20?7:Math.min(6,2+Math.floor((level-1)/4))}
function enemyShotLimit(){return Math.min(6,2+Math.floor((level-1)/4))}
function spawnInterval(){return Math.max(.92,rand(3.5,4.6)-2.55*levelProgress())}
function attackDelay(e,initial=false){
 const strength=Math.max(0,Math.min(2,(e.maxHp||1)-1));
 const base=initial?rand(3.5,4.5):rand(4.8,6.7);
 const difficultyCut=(initial?2.1:3.15)*levelProgress();
 return Math.max(initial?.85:.78,base-strength*.5-difficultyCut)
}
function enemyFlightDuration(){return 1.12-.60*levelProgress()}
function damageImmunity(){return 3.5-2.15*levelProgress()}
function bossCooldown(){return 19-11*levelProgress()}
"""
change("function spawn(){",new_difficulty_helpers+"function spawn(){","progression helper functions")

start=js.index("function spawn(){")
end=js.index("function getPosition(e){",start)
js=js[:start]+"""function spawn(){
 const active=enemies.filter(e=>e.status!=='dead'&&e.status!=='gone').length;
 if(active>=activeLimit())return;
 const slot=chooseSlot();if(slot===null)return;
 const chance=rand(0,1),p=levelProgress();
 const bossChance=level>1&&hits>=8&&time-bossAt>bossCooldown()? .12+.18*p : 0;
 const mediumChance=.38+.22*p;
 let kind;
 if(chance<bossChance){kind='boss';bossAt=time;tip('Hóember-boss! Három találat kell!',2)}
 else if(chance<bossChance+mediumChance)kind=Math.random()<.5?'penguin':'deer';
 else kind=['red','girl','green'][Math.floor(rand(0,3))];
 const info=types[kind];
 enemies.push({id:++id,kind,hp:info.hp,maxHp:info.hp,slot,oldSlot:slot,
 status:'active',born:time,hideAt:0,returnAt:0,deadAt:0,
 timeout:time+(kind==='boss'?14:12)+rand(-1,1),
 attackAt:time+attackDelay({maxHp:info.hp},true),
 phase:rand(0,Math.PI*2),hitAt:-9,defeated:false})
}
"""+js[end:]

change("if(state!=='game'||time-lastDamage<3.5)return;",
       "if(state!=='game'||time-lastDamage<damageImmunity())return;",
       "level-scaled damage immunity")
change("const enemyFlying=projectiles.filter(p=>p.enemy).length;if(enemyFlying>=2)return;",
       "const enemyFlying=projectiles.filter(p=>p.enemy).length;if(enemyFlying>=enemyShotLimit())return;",
       "progressive simultaneous projectiles")
change("born:time,duration:1.12});playFx('throw',.29,.83)",
       "born:time,duration:enemyFlightDuration()});playFx('throw',.29,.83)",
       "progressive snowball flight speed")
change("if(time>spawnAt){spawn();spawnAt=time+Math.max(1.75,rand(3.5,4.6)-.30*(level-1))}",
       "if(time>spawnAt){spawn();spawnAt=time+spawnInterval()}",
       "progressive enemy arrivals")
change("e.attackAt=time+rand(3.3,4.5)",
       "e.attackAt=time+attackDelay(e,true)",
       "respawn attack speed")
change("e.attackAt=time+rand(4.8,6.7)-.14*(level-1)",
       "e.attackAt=time+attackDelay(e,false)",
       "repeat attack speed")

# The celebration is visual, not a victory screen. It briefly pauses attack timers,
# then level 20 continues at maximum difficulty. Level rewards still work.
champion_code="""let championCelebrating=0,championShown=false,championTimeout=null;
function dismissChampion(){
 championCelebrating=0;
 if(championTimeout){clearTimeout(championTimeout);championTimeout=null}
 const panel=document.getElementById('championOverlay');
 if(panel)panel.remove()
}
function showChampionCelebration(){
 if(championShown)return;
 championShown=true;championCelebrating=4.5;
 const panel=document.createElement('div');
 panel.id='championOverlay';panel.className='champion-overlay';
 panel.innerHTML='<div class="champion-banner"><strong>20. SZINT!</strong><span>MEGÜBERNAGYON JÓ<br>HÓGOLYÓLÖVŐ VAGY!</span></div>';
 const colors=['#ffce45','#49c4ff','#f6608f','#77e08f','#b39bff','#fff5d8'];
 for(let i=0;i<110;i++){
  const bit=document.createElement('i');bit.className='champion-confetti';
  bit.style.setProperty('--x',(Math.random()*100).toFixed(1)+'%');
  bit.style.setProperty('--delay',(Math.random()*1.8).toFixed(2)+'s');
  bit.style.setProperty('--fall',(2.2+Math.random()*2).toFixed(2)+'s');
  bit.style.setProperty('--drift',((Math.random()-.5)*32).toFixed(1)+'vw');
  bit.style.setProperty('--rot',Math.round(350+Math.random()*900)+'deg');
  bit.style.background=colors[Math.floor(Math.random()*colors.length)];
  panel.appendChild(bit)
 }
 $('app').appendChild(panel);
 if(levelUpTimer)clearTimeout(levelUpTimer);
 const levelUp=document.getElementById('levelUpNotice');
 if(levelUp)levelUp.classList.remove('visible');
 championTimeout=setTimeout(()=>{
  const el=document.getElementById('championOverlay');if(el)el.remove();
  championTimeout=null
 },4900)
}
"""
change("function hud(){",champion_code+"function hud(){","20th level celebration")
change("if(levelUpTimer)clearTimeout(levelUpTimer);const levelUp=document.getElementById('levelUpNotice');",
       "dismissChampion();championShown=false;if(levelUpTimer)clearTimeout(levelUpTimer);const levelUp=document.getElementById('levelUpNotice');",
       "reset champion state")
change("function goMenu(){enemies=[];projectiles=[];bursts=[];reloading=false;ammo=MAX_AMMO;hud();show('menu')}",
       "function goMenu(){dismissChampion();enemies=[];projectiles=[];bursts=[];reloading=false;ammo=MAX_AMMO;hud();show('menu')}",
       "menu champion teardown")
change("function fire(x,y){if(state!=='game'||reloading)return;",
       "function fire(x,y){if(state!=='game'||reloading||championCelebrating>0)return;",
       "disable shots during celebration")
change("function update(dt){if(state!=='game')return;time+=dt;",
       "function update(dt){if(state!=='game')return;if(championCelebrating>0){championCelebrating=Math.max(0,championCelebrating-dt);return}time+=dt;",
       "safe pause during celebration")
change(
"""life+=restored;level=nowLevel;hud();showLevelUp(restored);
 tip(restored>0?'Szintlépés! Visszakaptál egy életet!':'Szintlépés! 3 életből 3 megvan!',2.4)""",
"""life+=restored;level=nowLevel;hud();
 if(level===20){showChampionCelebration();tip('Megübernagyon jó hógolyólövő vagy!',4.5)}
 else{showLevelUp(restored);
  tip(restored>0?'Szintlépés! Visszakaptál egy életet!':'Szintlépés! 3 életből 3 megvan!',2.4)}""",
"final stage special message")


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
css+="\n/* Android v1.3: SVG hearts never stretch on ultra-wide mobile screens. */\n.heart-row{align-items:center!important;justify-content:center!important;gap:.52cqw!important;\n margin-top:1.12cqw!important;line-height:0!important}\n.heart-row .live-heart,.heart-row .lost-heart{display:inline-flex!important;align-items:center!important;\n justify-content:center!important;flex:0 0 3.25cqw!important;width:3.25cqw!important;\n height:3.25cqw!important;max-width:100%;font-size:0!important;line-height:0!important;\n filter:none!important}\n.heart-row svg{display:block;width:100%;height:100%;aspect-ratio:1/1;overflow:visible}\n.heart-row .live-heart svg path{fill:#f32a56;stroke:#bc1841;stroke-width:.65}\n.heart-row .lost-heart svg path{fill:#aab6ce;stroke:#7e8da9;stroke-width:.65}\n.level-up-notice{position:absolute;z-index:19;top:40%;left:50%;transform:translate(-50%,-50%);\n text-align:center;pointer-events:none;opacity:0;display:flex;flex-direction:column;align-items:center;\n gap:.5cqw;text-shadow:0 .25cqw .4cqw #0e255e}\n.level-up-notice strong{font-size:5.7cqw;line-height:1.1;font-weight:1000;color:#fff;\n background:linear-gradient(#72b8ff,#1d63c5);padding:.18em .8em;border:.25cqw solid #e2f5ff;\n border-radius:1.5cqw;box-shadow:0 .35cqw 0 #103971}\n.level-up-notice span{font-size:3.2cqw;white-space:nowrap;font-weight:1000;color:white;\n padding:.28em .95em;border:.2cqw solid #fff4b8;border-radius:999px;\n background:linear-gradient(#ff7895,#e82b56)}\n.level-up-notice.visible{opacity:1;animation:mkLevelPop 2.4s both}\n@keyframes mkLevelPop{0%{opacity:0;transform:translate(-50%,-50%) scale(.65)}\n 12%,80%{opacity:1;transform:translate(-50%,-50%) scale(1)}\n 100%{opacity:0;transform:translate(-50%,-54%) scale(1.06)}}\n@media(prefers-reduced-motion:reduce){.level-up-notice.visible{animation:none;opacity:1}}\n"

# Intro video must FILL the already full-screen Android app; 'contain' caused vertical side bars.
css+="""
/* Android v1.4: intro edge-to-edge with proportional crop, no vertical side bars. */
#intro{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;
 overflow:hidden!important;background:#061939!important}
#introVideo,.intro-screen video{position:absolute!important;inset:0!important;top:0!important;left:0!important;
 width:100%!important;height:100%!important;min-width:100%!important;min-height:100%!important;
 max-width:none!important;max-height:none!important;object-fit:cover!important;object-position:50% 50%!important}
"""


css+="""
/* Android v1.5 celebratory confetti and 20th-level message. */
.champion-overlay{position:absolute;z-index:31;inset:0;overflow:hidden;pointer-events:none;
 background:radial-gradient(circle at center,rgba(16,40,93,.05),rgba(0,16,47,.28))}
.champion-banner{position:absolute;left:50%;top:38%;transform:translate(-50%,-50%);
 width:min(75%,1000px);display:flex;align-items:center;flex-direction:column;gap:1.3cqw;
 text-align:center;z-index:2;filter:drop-shadow(0 .6cqw .55cqw rgba(5,22,57,.7));
 animation:championEnter 4.55s both}
.champion-banner strong{font-size:5.3cqw;line-height:1;color:#fff;
 background:linear-gradient(#75c5ff,#265abf);border:.28cqw solid #f9f1c9;border-radius:1.5cqw;padding:.2em .8em}
.champion-banner span{font-size:4cqw;line-height:1.08;letter-spacing:.015em;
 color:#fff5cb;text-shadow:.09cqw .1cqw #193c8b,-.09cqw .1cqw #193c8b;
 font-weight:1000;background:linear-gradient(#ff7595,#d92b52);padding:.32em .75em;
 border:.32cqw solid white;border-radius:1.8cqw;box-shadow:0 .5cqw 0 #94253a}
.champion-confetti{position:absolute;top:-12%;left:var(--x);width:1.15cqw;height:2.05cqw;
 border-radius:.25cqw;opacity:0;animation:championFall var(--fall) var(--delay) linear 1 forwards}
@keyframes championFall{0%{opacity:0;transform:translate(0,-10vh) rotate(0)}
 8%{opacity:1}90%{opacity:1}
 100%{opacity:0;transform:translate(var(--drift),118vh) rotate(var(--rot))}}
@keyframes championEnter{0%{opacity:0;transform:translate(-50%,-50%) scale(.55)}
 11%,86%{opacity:1;transform:translate(-50%,-50%) scale(1)}
 100%{opacity:0;transform:translate(-50%,-50%) scale(1.08)}}
@media(prefers-reduced-motion:reduce){.champion-banner{animation:none}.champion-confetti{animation-duration:5s}}
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
print("Android 1.5: twenty escalating levels and champion confetti ready")
