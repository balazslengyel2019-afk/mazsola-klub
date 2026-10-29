/* Mazsola Klub – Hógolyócsata v1.8 · HD background, icon, menu links */
(()=>{'use strict';
const W=1280,H=720,MAX_AMMO=10,KEY='mazsola_hogolyocsata_best_v17';
const app=document.getElementById('app'),canvas=document.getElementById('stage'),ctx=canvas.getContext('2d');
const $=id=>document.getElementById(id);
const panels={intro:$('intro'),menu:$('menu'),hud:$('hud'),over:$('gameOver'),pause:$('pause'),about:$('about')};
const clamp=(x,a,b)=>Math.min(b,Math.max(a,x)),rand=(a,b)=>Math.random()*(b-a)+a;
const distance=(x,y,a,b)=>Math.hypot(x-a,y-b);
const smooth=t=>{t=clamp(t,0,1);return t*t*(3-2*t)};
const paths={village:'village_hd.png',snowball:'snowball_clean.png',
 red:'gyerek_fiu_piros_front.png',green:'gyerek_fiu_zold_front.png',girl:'gyerek_lany_lila_front.png',deer:'renszarvas_front.png',penguin:'pingvin_front.png',boss:'hoember_boss_front.png',
 fence:'fence_front.png',pine_left:'pine_left.png',pine_right:'pine_right.png',pine_far:'pine_far.png',igloo:'igloo.png',bush:'bush.png'};
const imgs={};Object.entries(paths).forEach(([n,p])=>{const im=new Image();imgs[n]=im;im.src='assets/'+p;im.onload=()=>{coverDirty=true}});
// The independently generated props are painted AFTER the opponents so they truly hide behind them.
// Each slot contains a hidden position and a visible peek-out position, both in the same world layer.
const slots=[
 {name:'Bal fenyő',hidden:[136,560],peek:[240,448]},
 {name:'Bal kerítés',hidden:[344,582],peek:[344,474]},
 {name:'Középső kerítés',hidden:[520,586],peek:[520,478]},
 {name:'Jobb kerítés',hidden:[666,584],peek:[666,476]},
 {name:'Iglu bal',hidden:[808,588],peek:[780,478]},
 {name:'Iglu jobb',hidden:[914,590],peek:[973,481]},
 {name:'Jobb fenyő',hidden:[1140,562],peek:[1072,446]},
 {name:'Távoli kerítés',hidden:[854,586],peek:[854,474]}
];
const types={red:{hp:1,w:158,voice:'ouch_easy',pitch:1.08},girl:{hp:1,w:158,voice:'ouch_medium',pitch:1.13},green:{hp:1,w:157,voice:'ouch_boss_final',pitch:1.20},deer:{hp:2,w:171,voice:'ouch_easy',pitch:.79},penguin:{hp:2,w:168,voice:'ouch_medium',pitch:.86},boss:{hp:3,w:207,voice:'ouch_boss',pitch:.88}};
let ambientTime=0;let state='intro',life=3,hits=0,shots=0,ammo=MAX_AMMO,best=0,time=0,last=0,spawnAt=.6,lastDamage=-100,shakeUntil=0,flashUntil=0,bossAt=-30,id=0,
 enemies=[],projectiles=[],bursts=[],snow=[],aim={x:640,y:360,visible:false},reloading=false,reloadAt=0,tipUntil=0,level=1;
let musicEnabled=true,sfxEnabled=true,musicGestureRequired=false;
let audioContext=null,soundsReady=0,playedSounds=0,soundErrors=0;
const soundNames=['throw','reload','puff','hurt','laugh','ouch_easy','ouch_medium','ouch_boss','ouch_boss_final'];
const soundFiles=Object.fromEntries(soundNames.map(n=>[n,'assets/sfx_'+n+'.mp3']));
const clips={},buffers={};
try{const v=localStorage.getItem('mazsola_hogolyocsata_music');if(v==='0')musicEnabled=false;
 const v2=localStorage.getItem('mazsola_hogolyocsata_v17_sfx');if(v2==='0')sfxEnabled=false;
 best=Number(localStorage.getItem(KEY)||0)}catch(e){}
const music=$('gameMusic');music.volume=.22;
for(const n of soundNames){const group=[];for(let i=0;i<3;i++){const a=new Audio(soundFiles[n]);a.preload='auto';group.push(a)}clips[n]={group,index:0}}
async function unlockAudio(){
 if(!audioContext){try{audioContext=new(window.AudioContext||window.webkitAudioContext)();
   // Immediately fetch/decode all MP3s on HTTP(S); file:// audio playback uses preloaded HTMLMediaElements.
   if(location.protocol!=='file:')for(const n of soundNames){fetch(soundFiles[n]).then(r=>{if(!r.ok)throw Error('Audio HTTP '+r.status);return r.arrayBuffer()}).then(b=>audioContext.decodeAudioData(b)).then(b=>{buffers[n]=b;soundsReady++}).catch(()=>{})}
  }catch(e){audioContext=null}}
 if(audioContext&&audioContext.state==='suspended')try{await audioContext.resume()}catch(e){}
}
function playFx(n,volume=.7,pitch=1){
 if(!sfxEnabled)return;let gain=clamp(volume*1.25,0,1),rate=clamp(pitch,.7,1.3);
 if(audioContext&&audioContext.state==='running'&&buffers[n]){
  try{const src=audioContext.createBufferSource(),gn=audioContext.createGain();src.buffer=buffers[n];src.playbackRate.value=rate;gn.gain.value=gain;src.connect(gn).connect(audioContext.destination);src.start();playedSounds++;return}catch(e){soundErrors++}}
 const pool=clips[n];if(!pool)return;
 let a=pool.group[pool.index];pool.index=(pool.index+1)%pool.group.length;
 try{a.pause();a.currentTime=0;a.volume=gain;a.playbackRate=rate;
   const result=a.play();playedSounds++;if(result&&result.catch)result.catch(err=>{soundErrors++;if(err?.name!=='NotAllowedError')console.warn('Hang lejátszási hiba',n,err.name)})
 }catch(e){soundErrors++}
}
function refreshSoundButtons(){const m=musicEnabled?(musicGestureRequired?'ZENE: INDÍTÁS':'ZENE: BE'):'ZENE: KI';for(const key of ['menuMusicButton','soundButton']){const b=$(key);b.textContent=m;b.setAttribute('aria-pressed',String(musicEnabled))}
 for(const key of ['menuSfxButton','pauseSfxButton','hudSfxButton']){const b=$(key);b.textContent='HANGOK: '+(sfxEnabled?'BE':'KI');b.setAttribute('aria-pressed',String(sfxEnabled))}}
function syncMusic(){if(!musicEnabled||document.hidden||state==='intro'){music.pause();musicGestureRequired=false;refreshSoundButtons();return}
 const p=music.play();if(p&&typeof p.then==='function')p.then(()=>{musicGestureRequired=false;refreshSoundButtons()}).catch(()=>{musicGestureRequired=true;refreshSoundButtons()});refreshSoundButtons()}
function setMusic(){if(!musicEnabled&&!musicGestureRequired){musicEnabled=true}else if(musicEnabled&&musicGestureRequired){musicGestureRequired=false;syncMusic();return}else musicEnabled=!musicEnabled;
 try{localStorage.setItem('mazsola_hogolyocsata_music',musicEnabled?'1':'0')}catch(e){}syncMusic()}
function setEffects(){sfxEnabled=!sfxEnabled;try{localStorage.setItem('mazsola_hogolyocsata_v17_sfx',sfxEnabled?'1':'0')}catch(e){}refreshSoundButtons();if(sfxEnabled)playFx('puff',.45)}
$('menuMusicButton').onclick=setMusic;$('soundButton').onclick=setMusic;$('menuSfxButton').onclick=setEffects;$('pauseSfxButton').onclick=setEffects;$('hudSfxButton').onclick=setEffects;

function buttonFx(kind='soft'){ if(!sfxEnabled) return; if(kind==='primary') playFx('throw',.20,1.16); else if(kind==='danger') playFx('reload',.17,1.04); else playFx('puff',.14,1.22) }
function bindButtonSound(id,kind='soft'){ const el=$(id); if(!el) return; el.addEventListener('pointerdown',()=>buttonFx(kind),{passive:true}); }

// Unlock audio on EVERY genuine gesture; music starts automatically as soon as the browser permits it.
document.addEventListener('pointerdown',()=>{unlockAudio();if(musicEnabled&&state!=='intro'&&(music.paused||musicGestureRequired))syncMusic()},{capture:true});
document.addEventListener('keydown',()=>{unlockAudio();if(musicEnabled&&state!=='intro'&&music.paused)syncMusic()},{capture:true});
// Visible flakes in the menu use their own animation; they keep falling even when gameplay time is paused.
const snowLayer=$('menuSnow');for(let i=0;i<74;i++){const f=document.createElement('i');f.className='menu-flake';const r=((i*73+17)%101)/100;
f.style.setProperty('--x',(r*100).toFixed(2)+'%');f.style.setProperty('--s',(2+(i%7)*.9).toFixed(1)+'px');f.style.setProperty('--a',(.35+(i%5)*.12).toFixed(2));
f.style.setProperty('--duration',(7+(i%13)*.65).toFixed(2)+'s');f.style.setProperty('--delay',(-(i*1.47)%18).toFixed(2)+'s');f.style.setProperty('--drift',((i%2?1:-1)*(20+i%9*8))+'px');snowLayer.appendChild(f)}
function show(name){state=name;Object.keys(panels).forEach(p=>panels[p].classList.toggle('hidden',p!==name&&!(name==='game'&&p==='hud')));
 app.style.cursor=name==='game'?'none':'default';syncMusic()}
function tip(str,duration=2){$('gameTip').textContent=str;tipUntil=time+duration}
function hud(){const h=$('hearts');h.replaceChildren();for(let i=0;i<3;i++){const s=document.createElement('span');s.className=i<life?'live-heart':'lost-heart';s.textContent='♥';h.appendChild(s)}
 $('hitValue').textContent=String(hits);$('ammoValue').textContent=reloading?'…':String(ammo);$('difficultyLabel').textContent=level+'. SZINT'}
function reset(){unlockAudio();life=3;hits=0;shots=0;ammo=MAX_AMMO;time=0;spawnAt=.75;lastDamage=-100;shakeUntil=0;flashUntil=0;bossAt=-30;id=0;level=1;enemies=[];projectiles=[];bursts=[];reloading=false;reloadAt=0;aim.visible=false;
 tip('Találd el a felbukkanó figurákat!',4);hud();show('game');last=performance.now()}
function goMenu(){enemies=[];projectiles=[];bursts=[];show('menu')}
function finishGame(){if(state!=='game')return;show('over');const acc=shots?Math.round(100*hits/shots):0;best=Math.max(best,hits);try{localStorage.setItem(KEY,String(best))}catch(e){}
 $('totalShots').textContent=String(shots);$('totalHits').textContent=String(hits);$('accuracy').textContent=acc+'%';$('best').textContent=String(best);$('resultHeading').textContent=hits>=16?'Nagyszerű hógolyózás!':'Szép játék volt!'}
function hurt(){if(state!=='game'||time-lastDamage<3.5)return;life--;lastDamage=time;flashUntil=time+.7;shakeUntil=time+.24;addBurst(640,359,'camera');playFx('puff',.65);playFx('hurt',.63);setTimeout(()=>playFx('laugh',.81),160);hud();tip('Hoppá, eltaláltak!',2);if(life<=0)finishGame()}
function difficulty(){return Math.min(6,1+Math.floor(hits/8))}
function occupied(except){return new Set(enemies.filter(e=>e.status==='active'&&e.id!==except).map(e=>e.slot))}
function chooseSlot(old=-1,except=-1){const used=occupied(except);let options=slots.map((_,i)=>i).filter(i=>i!==old&&!used.has(i));if(!options.length)options=slots.map((_,i)=>i).filter(i=>!used.has(i));return options.length?options[Math.floor(Math.random()*options.length)]:null}
function spawn(){const active=enemies.filter(e=>e.status!=='dead'&&e.status!=='gone').length,limit=level<=2?2:level<=4?3:4;
 if(active>=limit)return;
 const slot=chooseSlot();if(slot===null)return;
 let kind;const chance=rand(0,1);if(hits>=9&&time-bossAt>19&&chance<.12+.02*(level-1)){kind='boss';bossAt=time;tip('Hóember-boss! Három találat kell!',2.7)}
 else if(chance<.38+.045*(level-1))kind=Math.random()<.5?'penguin':'deer';else kind=['red','girl','green'][Math.floor(rand(0,3))];
 const info=types[kind];enemies.push({id:++id,kind,hp:info.hp,maxHp:info.hp,slot,oldSlot:slot,status:'active',born:time,hideAt:0,returnAt:0,deadAt:0,timeout:time+(kind==='boss'?14:12)+rand(-1,1),attackAt:time+rand(3.5,4.5),phase:rand(0,Math.PI*2),hitAt:-9,defeated:false})}
function getPosition(e){const sl=slots[e.slot];let f=1;if(e.status==='hiding')f=1-smooth((time-e.hideAt)/.26);
 else if(e.status==='dead')f=1-smooth((time-e.deadAt)/.32);
 else f=smooth((time-e.born)/.38);
 const w=Math.sin(time*2+e.phase)*5*f;
 return{x:sl.hidden[0]+(sl.peek[0]-sl.hidden[0])*f+w,y:sl.hidden[1]+(sl.peek[1]-sl.hidden[1])*f+Math.sin(time*2.8+e.phase)*2*f,visibility:f}}
function addBurst(x,y,type='hit'){bursts.push({x,y,t:time,type,seed:rand(0,9999)})}
function impact(x,y){const candidates=[];
 for(const e of enemies){if(e.status!=='active')continue;const p=getPosition(e),info=types[e.kind],im=imgs[e.kind];if(!im.complete||!im.naturalWidth||p.visibility<.82)continue;
 const h=info.w*im.naturalHeight/im.naturalWidth;
 const cx=p.x, cy=p.y-h*.54;
 if(Math.abs(x-cx)<info.w*.38+6&&Math.abs(y-cy)<h*.42+8&&!covered(x,y))candidates.push({e,p,delta:distance(x,y,cx,cy)})}
 candidates.sort((a,b)=>a.delta-b.delta);
 if(!candidates.length){addBurst(x,y,'ground');playFx('puff',.28);return}
 const {e,p}=candidates[0],info=types[e.kind];e.hp--;e.hitAt=time;hits++;addBurst(x,y,'hit');playFx('puff',.86);
 const voice=e.kind==='boss'&&e.hp===0?'ouch_boss_final':info.voice;
 setTimeout(()=>playFx(voice,.90,info.pitch),70);hud();if(e.hp<=0){e.status='dead';e.deadAt=time;e.defeated=true;tip(e.kind==='boss'?'Legyőzted a boss-t!':'Szép találat!',1.5)}
 else{e.status='hiding';e.hideAt=time;e.returnAt=time+rand(1.15,1.65);e.oldSlot=e.slot;projectiles=projectiles.filter(p=>!p.enemy||p.owner!==e.id);tip('Elbújt! Máshol fog előbukkanni!',2)}
}
function reload(){if(state!=='game'||reloading||ammo===MAX_AMMO)return;reloading=true;reloadAt=time+2.0;hud();playFx('reload',.49)}
function projectilePosition(p){const f=clamp((time-p.born)/p.duration,0,1),ease=1-(1-f)*(1-f);
 const x=p.from[0]+(p.to[0]-p.from[0])*ease;
 const y=p.from[1]+(p.to[1]-p.from[1])*ease-Math.sin(f*Math.PI)*(p.enemy?30:90);
 return{x,y,progress:f}}
function fire(x,y){if(state!=='game'||reloading)return;if(ammo<=0){reload();return}
 ammo--;shots++;hud();playFx('throw',.59);
 let block=null,dmin=75;for(const p of projectiles){if(!p.enemy)continue;const at=projectilePosition(p);const radius=25+85*at.progress;if(distance(at.x,at.y,x,y)<Math.max(radius,45)&&distance(at.x,at.y,x,y)<dmin+radius*.45){block=p;dmin=distance(at.x,at.y,x,y)}}
 if(block){projectiles.splice(projectiles.indexOf(block),1);const at=projectilePosition(block);addBurst(at.x,at.y,'block');playFx('puff',.84);tip('Kivédted a hógolyót!',1.5)}
 else projectiles.push({enemy:false,from:[640,715],to:[x,y],born:time,duration:.22});
 if(ammo===0)tip('Elfogytak a hógolyók!',1.1)
}
function throwEnemy(e){const p=getPosition(e);if(p.visibility<.84)return;
 const enemyFlying=projectiles.filter(p=>p.enemy).length;if(enemyFlying>=2)return;
 projectiles.push({enemy:true,owner:e.id,from:[p.x,p.y],to:[640,362],born:time,duration:1.12});playFx('throw',.29,.83)}
function update(dt){if(state!=='game')return;time+=dt;
 if(reloading&&time>=reloadAt){reloading=false;ammo=MAX_AMMO;hud();playFx('reload',.49)}
 const nowLevel=difficulty();if(nowLevel!==level){level=nowLevel;hud();tip('Új szint! Egy kicsit sűrűbb a hócsata!',2.2)}
 if(time>spawnAt){spawn();spawnAt=time+Math.max(1.75,rand(3.5,4.6)-.30*(level-1))}
 for(const e of enemies){if(e.status==='hiding'){
 if(time>=e.returnAt){const s=chooseSlot(e.oldSlot,e.id);if(s!==null){e.slot=s;e.status='active';e.born=time;e.timeout=time+rand(10,13);e.attackAt=time+rand(3.3,4.5)}else e.returnAt=time+.25}continue}
 if(e.status!=='active')continue;if(time>e.timeout){e.status='dead';e.deadAt=time;e.defeated=false;continue}
 if(time>e.attackAt){throwEnemy(e);e.attackAt=time+rand(4.8,6.7)-.14*(level-1)}
 }
 enemies=enemies.filter(e=>e.status!=='dead'||time-e.deadAt<.4);
 for(let i=projectiles.length-1;i>=0;i--){const p=projectiles[i];if(time-p.born>=p.duration){projectiles.splice(i,1);if(p.enemy)hurt();else impact(p.to[0],p.to[1])}}
 bursts=bursts.filter(b=>time-b.t<(b.type==='camera'?.75:.58));if(time>tipUntil){$('gameTip').textContent='Célozz és dobj!';tipUntil=Infinity}
 if(ammo===0&&!reloading)reload();if(aim.touch&&time-aim.lastTouch>.3)aim.visible=false;
}
// Draw a transparent foreground mask and read its alpha for visibility-aware enemy collision.
const coverCanvas=document.createElement('canvas');coverCanvas.width=W;coverCanvas.height=H;const mask=coverCanvas.getContext('2d',{willReadFrequently:true});let coverDirty=true;
function blit(c,n,x,y,w,h){const im=imgs[n];if(im&&im.complete&&im.naturalWidth)c.drawImage(im,x,y,w,h)}
function coverProps(c){
 // Trees and igloo are in front of the characters, but the fence is the topmost foreground layer.
 blit(c,'pine_left',35,242,295,391);
 blit(c,'pine_right',995,250,265,390);
 blit(c,'igloo',733,443,334,251);
 blit(c,'bush',1150,553,136,116);
 blit(c,'fence',257,492,653,217);
}
function drawMidProps(c){
 blit(c,'pine_left',35,242,295,391);
 blit(c,'pine_right',995,250,265,390);
 blit(c,'igloo',733,443,334,251);
 blit(c,'bush',1150,553,136,116);
}
function drawFrontFence(c){ blit(c,'fence',257,492,653,217); }
function covered(x,y){if(coverDirty){mask.clearRect(0,0,W,H);coverProps(mask);coverDirty=false}
 try{return mask.getImageData(clamp(Math.round(x),0,W-1),clamp(Math.round(y),0,H-1),1,1).data[3]>110}catch(_){return false}}
function drawBG(){
 ctx.clearRect(0,0,W,H);
 const bg=imgs.village;
 // The new source is 16:9; draw the WHOLE source at the same ratio, never crop/stretch its top.
 if(bg.complete&&bg.naturalWidth)ctx.drawImage(bg,0,0,W,H);
 else{const g=ctx.createLinearGradient(0,0,0,H);g.addColorStop(0,'#103a94');g.addColorStop(.65,'#2c66b7');g.addColorStop(1,'#e5f2ff');ctx.fillStyle=g;ctx.fillRect(0,0,W,H)}
 for(let i=0;i<84;i++){
  if(!snow[i])snow[i]={x:rand(0,W),y:rand(0,H),r:rand(.9,3.3),s:rand(12,31)};
  const f=snow[i],y=(f.y+ambientTime*f.s)%H;
  ctx.fillStyle='rgba(250,253,255,.76)';ctx.beginPath();ctx.arc(f.x+Math.sin(ambientTime*.43+i)*8,y,f.r,0,Math.PI*2);ctx.fill();
 }
}
function drawEnemies(){for(const e of enemies){const p=getPosition(e),im=imgs[e.kind];if(!im||!im.complete||!im.naturalWidth)continue;
 if(p.visibility<.06)continue;const width=types[e.kind].w,height=width*im.naturalHeight/im.naturalWidth;
 ctx.save();ctx.translate(p.x,p.y);if(e.status==='dead'&&e.defeated)ctx.rotate((time-e.deadAt)*.36);if(time-e.hitAt<.16){ctx.shadowColor='#a6eaff';ctx.shadowBlur=18}
 ctx.drawImage(im,-width/2,-height/2,width,height);ctx.restore();
 if(e.status==='active'&&p.visibility>.9&&e.hp>0&&e.maxHp>1){ctx.save();ctx.textAlign='center';ctx.font='bold 22px Arial';for(let i=0;i<e.maxHp;i++){ctx.fillStyle=i<e.hp?'#f52957':'#8797b1';ctx.strokeStyle='#fff';ctx.lineWidth=2;const x=p.x+(i-(e.maxHp-1)/2)*27,y=p.y-height*.52-13;
 ctx.beginPath();ctx.moveTo(x,y+8);ctx.bezierCurveTo(x-14,y-3,x-19,y+10,x,y+22);ctx.bezierCurveTo(x+19,y+10,x+14,y-3,x,y+8);ctx.fill();ctx.stroke()};ctx.restore()}}
}
function drawSnowburst(x,y,progress,scale=1,seed=0){
 // Clean particle ring plus quickly dissolving snow powder: NOT a scaled sprite atlas.
 const opacity=Math.pow(1-progress,1.45),radius=(10+68*progress)*scale;
 ctx.save();ctx.globalAlpha=opacity;
 const cloud=ctx.createRadialGradient(x,y,2,x,y,Math.max(12,radius*.55));
 cloud.addColorStop(0,'rgba(255,255,255,.91)');cloud.addColorStop(.45,'rgba(238,249,255,.66)');cloud.addColorStop(1,'rgba(224,239,255,0)');
 ctx.fillStyle=cloud;ctx.beginPath();ctx.arc(x,y,Math.max(12,radius*.55),0,Math.PI*2);ctx.fill();
 for(let i=0;i<18;i++){const ang=i*2.399963+seed*.014,d=(.29+(i%5)*.15)*radius;
  const px=x+Math.cos(ang)*d,py=y+Math.sin(ang)*d-.12*radius;
  const rr=Math.max(.7,(5+(i%4)*2)*scale*(1-progress*.5));
  ctx.fillStyle=i%3===0?'#c5e4ff':'#f8fcff';ctx.beginPath();ctx.arc(px,py,rr,0,Math.PI*2);ctx.fill();
 }
 ctx.restore();
}
function drawBursts(){for(const b of bursts){const isCam=b.type==='camera',duration=isCam?.65:.42;const t=clamp((time-b.t)/duration,0,1);
 if(isCam){drawSnowburst(b.x,b.y,t,2.3,b.seed);continue}
 drawSnowburst(b.x,b.y,t,b.type==='hit'?.95:.7,b.seed)}}
function drawBalls(){for(const p of projectiles){const loc=projectilePosition(p),f=loc.progress;
 const sz=p.enemy?28+100*f*f:42-17*f;
 const im=imgs.snowball;
 ctx.save();
 // light translucent motion streak aligned with the actual trajectory
 const dx=p.to[0]-p.from[0],dy=p.to[1]-p.from[1],norm=Math.hypot(dx,dy)||1;
 const tx=dx/norm,ty=dy/norm;let trail=sz*(p.enemy?.9:1.4);
 const grad=ctx.createLinearGradient(loc.x-tx*trail,loc.y-ty*trail,loc.x,loc.y);
 grad.addColorStop(0,'rgba(231,248,255,0)');grad.addColorStop(1,'rgba(244,252,255,.57)');
 ctx.strokeStyle=grad;ctx.lineCap='round';ctx.lineWidth=Math.max(4,sz*.35);
 ctx.beginPath();ctx.moveTo(loc.x-tx*trail,loc.y-ty*trail);ctx.lineTo(loc.x-tx*sz*.14,loc.y-ty*sz*.14);ctx.stroke();
 if(im.complete&&im.naturalWidth)ctx.drawImage(im,loc.x-sz/2,loc.y-sz/2,sz,sz);
 else{const orb=ctx.createRadialGradient(loc.x-sz*.22,loc.y-sz*.3,2,loc.x,loc.y,sz*.56);orb.addColorStop(0,'#fff');orb.addColorStop(.72,'#f6fcff');orb.addColorStop(1,'#b2d8fc');ctx.fillStyle=orb;ctx.beginPath();ctx.arc(loc.x,loc.y,sz/2,0,2*Math.PI);ctx.fill()}
 ctx.restore();}}
function crosshair(x,y){ctx.save();ctx.translate(x,y);ctx.lineWidth=7;ctx.strokeStyle='#fff';ctx.beginPath();ctx.arc(0,0,22,0,7);ctx.stroke();ctx.lineWidth=5;ctx.strokeStyle='#ed3153';ctx.beginPath();ctx.arc(0,0,22,0,7);for(let a=0;a<4;a++){const rad=a*Math.PI/2;ctx.moveTo(Math.cos(rad)*12,Math.sin(rad)*12);ctx.lineTo(Math.cos(rad)*35,Math.sin(rad)*35)}ctx.stroke();ctx.fillStyle='#ed3153';ctx.beginPath();ctx.arc(0,0,4.8,0,7);ctx.fill();ctx.restore()}
function damageOverlay(){if(time>=flashUntil)return;const f=(flashUntil-time)/.7;
 // Red is applied to the entire CAMERA frame, never to the fence or ground.
 ctx.fillStyle='rgba(244,36,57,'+(.15*f)+')';ctx.fillRect(0,0,W,H);
 const vign=ctx.createRadialGradient(640,350,70,640,350,800);vign.addColorStop(0,'rgba(255,59,67,0)');vign.addColorStop(1,'rgba(240,20,44,'+(.57*f)+')');ctx.fillStyle=vign;ctx.fillRect(0,0,W,H)}
function draw(){ctx.save();if(time<shakeUntil&&state==='game')ctx.translate(rand(-3,3),rand(-3,3));drawBG();
 if(state==='game'||state==='over'||state==='pause'){drawEnemies();drawMidProps(ctx);drawFrontFence(ctx);drawBalls();drawBursts();damageOverlay();if(state==='game'&&aim.visible)crosshair(aim.x,aim.y)}
 else{drawMidProps(ctx);drawFrontFence(ctx)}ctx.restore()}
function tick(now){const dt=clamp((now-last)/1000,0,.05);last=now;ambientTime+=dt;update(dt);draw();requestAnimationFrame(tick)}requestAnimationFrame(tick);
function coords(e){const rect=canvas.getBoundingClientRect();let x,y;
 if(matchMedia('(max-aspect-ratio: 9/12)').matches){const a=(e.clientX-rect.left)/rect.width,b=(e.clientY-rect.top)/rect.height;x=b*W;y=(1-a)*H}
 else{const scale=Math.max(rect.width/W,rect.height/H);const cropX=(W*scale-rect.width)/2,cropY=(H*scale-rect.height)/2;x=(e.clientX-rect.left+cropX)/scale;y=(e.clientY-rect.top+cropY)/scale}return{x:clamp(x,0,W),y:clamp(y,0,H)}}
canvas.addEventListener('pointermove',e=>{if(state!=='game')return;const p=coords(e);aim={...p,visible:e.pointerType!=='touch',touch:false}});
canvas.addEventListener('pointerdown',e=>{if(state!=='game')return;e.preventDefault();const p=coords(e);aim={...p,visible:true,touch:e.pointerType==='touch',lastTouch:time};fire(p.x,p.y)});
canvas.addEventListener('contextmenu',e=>e.preventDefault());
window.addEventListener('keydown',e=>{if((e.key==='Escape'||e.key.toLowerCase()==='p')&&state==='game'){show('pause');e.preventDefault()}
 else if((e.key==='Escape'||e.key.toLowerCase()==='p')&&state==='pause'){show('game');last=performance.now();e.preventDefault()}
 else if(e.key.toLowerCase()==='r'&&state==='game'){reload();e.preventDefault()}
 else if(e.code==='Space'&&state==='game'){fire(aim.x,aim.y);e.preventDefault()}});
$('newGame').onclick=reset;$('tryAgain').onclick=reset;$('backHome').onclick=goMenu;$('pauseHome').onclick=goMenu;
$('showAbout').onclick=()=>show('about');$('closeAbout').onclick=goMenu;$('pauseButton').onclick=()=>show('pause');$('resume').onclick=()=>{show('game');last=performance.now()};$('reloadButton').onclick=reload;
let introDone=false;function finishIntro(){if(introDone)return;introDone=true;const v=$('introVideo');v.pause();show('menu')}
const introVideo=$('introVideo');introVideo.onclick=finishIntro;introVideo.onended=finishIntro;introVideo.onerror=finishIntro;setTimeout(finishIntro,3400);
document.addEventListener('visibilitychange',()=>{if(document.hidden&&state==='game')show('pause');if(document.hidden)music.pause();else syncMusic()});
['newGame','showAbout','closeAbout','menuMusicButton','menuSfxButton','tryAgain','backHome','pauseHome','pauseButton','resume','reloadButton','soundButton','hudSfxButton','pauseSfxButton'].forEach(id=>bindButtonSound(id,['newGame','tryAgain','resume'].includes(id)?'primary':(id==='reloadButton'?'danger':'soft')));
document.querySelectorAll('.social-icon,.playlist-pill').forEach(a=>a.addEventListener('pointerdown',()=>buttonFx('soft'),{passive:true}));
refreshSoundButtons();
window.__mazsolaTest=()=>({state,life,hits,shots,ammo,level,musicEnabled,sfxEnabled,playedSounds,soundsReady,soundErrors,audioState:audioContext?.state||'none',enemyCount:enemies.length,enemyStates:enemies.map(e=>({kind:e.kind,hp:e.hp,slot:e.slot,status:e.status,position:getPosition(e)})),projectiles:projectiles.length});
})();
