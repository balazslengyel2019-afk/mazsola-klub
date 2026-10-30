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
 {name:'Bal fenyő',hidden:[136,560],peek:[295,417]},
 {name:'Bal kerítés',hidden:[344,582],peek:[344,446]},
 {name:'Középső kerítés',hidden:[520,586],peek:[520,448]},
 {name:'Jobb kerítés',hidden:[666,584],peek:[666,447]},
 {name:'Iglu bal',hidden:[808,588],peek:[744,424]},
 {name:'Iglu jobb',hidden:[914,590],peek:[1030,426]},
 {name:'Jobb fenyő',hidden:[1140,562],peek:[1022,415]},
 {name:'Távoli kerítés',hidden:[854,586],peek:[854,441]}
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
let levelUpTimer=null;
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
let championCelebrating=0,championShown=false,championTimeout=null;
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
function hud(){const h=$('hearts');h.replaceChildren();for(let i=0;i<3;i++){const s=document.createElement('span');s.className=i<life?'live-heart':'lost-heart';s.innerHTML='<svg viewBox="0 0 40 40" aria-hidden="true" focusable="false"><path d="M20 35 C16.5 31.5 6.5 25 3.5 20.5 C-1 13.5 4.5 5 11.5 5 C16 5 18.5 7.5 20 10.2 C21.5 7.5 24 5 28.5 5 C35.5 5 41 13.5 36.5 20.5 C33.5 25 23.5 31.5 20 35Z"/></svg>';h.appendChild(s)}
 $('hitValue').textContent=String(hits);$('ammoValue').textContent=reloading?'…':String(ammo);$('difficultyLabel').textContent=level+'. SZINT'}
function reset(){unlockAudio();life=3;hits=0;shots=0;ammo=MAX_AMMO;time=0;spawnAt=.75;lastDamage=-100;shakeUntil=0;flashUntil=0;bossAt=-30;id=0;level=1;enemies=[];projectiles=[];bursts=[];reloading=false;reloadAt=0;aim={x:640,y:360,visible:false,touch:false,lastTouch:-10};tipUntil=0;last=performance.now();dismissChampion();championShown=false;if(levelUpTimer)clearTimeout(levelUpTimer);const levelUp=document.getElementById('levelUpNotice');if(levelUp)levelUp.classList.remove('visible');
 tip('Találd el a felbukkanó figurákat!',4);hud();show('game');last=performance.now()}
function goMenu(){dismissChampion();enemies=[];projectiles=[];bursts=[];reloading=false;ammo=MAX_AMMO;hud();show('menu')}
function finishGame(){if(state!=='game')return;show('over');const acc=shots?Math.round(100*hits/shots):0;best=Math.max(best,hits);try{localStorage.setItem(KEY,String(best))}catch(e){}
 $('totalShots').textContent=String(shots);$('totalHits').textContent=String(hits);$('accuracy').textContent=acc+'%';$('best').textContent=String(best);$('resultHeading').textContent=hits>=16?'Nagyszerű hógolyózás!':'Szép játék volt!'}
function hurt(){if(state!=='game'||time-lastDamage<damageImmunity())return;life--;lastDamage=time;flashUntil=time+.7;shakeUntil=time+.24;addBurst(640,359,'camera');playFx('puff',.65);playFx('hurt',.63);setTimeout(()=>playFx('laugh',.81),160);hud();tip('Hoppá, eltaláltak!',2);if(life<=0)finishGame()}
function difficulty(){return Math.min(20,1+Math.floor(hits/8))}
function occupied(except){return new Set(enemies.filter(e=>e.status==='active'&&e.id!==except).map(e=>e.slot))}
function chooseSlot(old=-1,except=-1){const used=occupied(except);let options=slots.map((_,i)=>i).filter(i=>i!==old&&!used.has(i));if(!options.length)options=slots.map((_,i)=>i).filter(i=>!used.has(i));return options.length?options[Math.floor(Math.random()*options.length)]:null}
function levelProgress(){return (level-1)/19}
function activeLimit(){return level===20?7:Math.min(6,2+Math.floor((level-1)/4))}
function enemyShotLimit(){return Math.min(6,2+Math.floor((level-1)/4))}
function spawnInterval(){return Math.max(.92,rand(3.5,4.6)-2.55*levelProgress())}
function attackDelay(e,initial=false){
 const strength=Math.max(0,Math.min(2,(e.maxHp||1)-1));
 const base=initial?rand(3.5,4.5):rand(4.8,6.7);
 const difficultyCut=(initial?2.1:3.15)*levelProgress();
 return Math.max(initial?1.80:1.65,base-difficultyCut)-strength*.5
}
function enemyFlightDuration(){return 1.12-.60*levelProgress()}
function damageImmunity(){return 3.5-2.15*levelProgress()}
function bossCooldown(){return 19-11*levelProgress()}
function spawn(){
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
function getPosition(e){const sl=slots[e.slot];let f=1;if(e.status==='hiding')f=1-smooth((time-e.hideAt)/.26);
 else if(e.status==='dead')f=1-smooth((time-e.deadAt)/.32);
 else f=smooth((time-e.born)/.38);
 const w=Math.sin(time*2+e.phase)*5*f;
 return{x:sl.hidden[0]+(sl.peek[0]-sl.hidden[0])*f+w,y:sl.hidden[1]+(sl.peek[1]-sl.hidden[1])*f+Math.sin(time*2.8+e.phase)*2*f,visibility:f}}
function addBurst(x,y,type='hit'){bursts.push({x,y,t:time,type,seed:rand(0,9999)})}
const spriteAlphaCache=new Map();
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
function fire(x,y){if(state!=='game'||reloading||championCelebrating>0)return;if(ammo<=0){reload();return}
 ammo--;shots++;hud();playFx('throw',.59);
 let block=null,dmin=75;for(const p of projectiles){if(!p.enemy)continue;const at=projectilePosition(p);const radius=25+85*at.progress;if(distance(at.x,at.y,x,y)<Math.max(radius,45)&&distance(at.x,at.y,x,y)<dmin+radius*.45){block=p;dmin=distance(at.x,at.y,x,y)}}
 if(block){projectiles.splice(projectiles.indexOf(block),1);const at=projectilePosition(block);addBurst(at.x,at.y,'block');playFx('puff',.84);tip('Kivédted a hógolyót!',1.5)}
 else projectiles.push({enemy:false,from:[640,715],to:[x,y],born:time,duration:.22});
 if(ammo===0)tip('Elfogytak a hógolyók!',1.1)
}
function throwEnemy(e){const p=getPosition(e);if(p.visibility<.84)return;
 const enemyFlying=projectiles.filter(p=>p.enemy).length;if(enemyFlying>=enemyShotLimit())return;
 projectiles.push({enemy:true,owner:e.id,from:[p.x,p.y],to:[640,362],born:time,duration:enemyFlightDuration()});playFx('throw',.29,.83)}
function update(dt){if(state!=='game')return;if(championCelebrating>0){championCelebrating=Math.max(0,championCelebrating-dt);return}time+=dt;
 if(reloading&&time>=reloadAt){reloading=false;ammo=MAX_AMMO;hud();playFx('reload',.49)}
 const nowLevel=difficulty();if(nowLevel>level){
 const earned=nowLevel-level,restored=Math.min(3-life,earned);
 life+=restored;level=nowLevel;hud();
 if(level===20){showChampionCelebration();tip('Megübernagyon jó hógolyólövő vagy!',4.5)}
 else{showLevelUp(restored);
  tip(restored>0?'Szintlépés! Visszakaptál egy életet!':'Szintlépés! 3 életből 3 megvan!',2.4)}
}
 if(time>spawnAt){spawn();spawnAt=time+spawnInterval()}
 for(const e of enemies){if(e.status==='hiding'){
 if(time>=e.returnAt){const s=chooseSlot(e.oldSlot,e.id);if(s!==null){e.slot=s;e.status='active';e.born=time;e.timeout=time+rand(10,13);e.attackAt=time+attackDelay(e,true)}else e.returnAt=time+.25}continue}
 if(e.status!=='active')continue;if(time>e.timeout){e.status='dead';e.deadAt=time;e.defeated=false;continue}
 if(time>e.attackAt){throwEnemy(e);e.attackAt=time+attackDelay(e,false)}
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
let rafId=0,lastFrameWall=performance.now(),lastDrawError='',drawErrorCount=0;
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

function coords(e){const rect=canvas.getBoundingClientRect();
 const scale=Math.max(rect.width/W,rect.height/H);
 const cropX=(W-rect.width/scale)/2,cropY=(H-rect.height/scale)/2;
 return{x:clamp(cropX+(e.clientX-rect.left)/scale,0,W),
        y:clamp(cropY+(e.clientY-rect.top)/scale,0,H)}}
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
document.addEventListener('visibilitychange',()=>{if(document.hidden&&state==='game')show('pause');if(document.hidden)music.pause();else{last=performance.now();keepRendering();syncMusic()}});window.addEventListener('pageshow',()=>{last=performance.now();keepRendering()});
['newGame','showAbout','closeAbout','menuMusicButton','menuSfxButton','tryAgain','backHome','pauseHome','pauseButton','resume','reloadButton','soundButton','hudSfxButton','pauseSfxButton'].forEach(id=>bindButtonSound(id,['newGame','tryAgain','resume'].includes(id)?'primary':(id==='reloadButton'?'danger':'soft')));
document.querySelectorAll('.social-icon,.playlist-pill').forEach(a=>a.addEventListener('pointerdown',()=>buttonFx('soft'),{passive:true}));
refreshSoundButtons();
window.__mazsolaTest=()=>({state,life,hits,shots,ammo,level,musicEnabled,sfxEnabled,playedSounds,soundsReady,soundErrors,audioState:audioContext?.state||'none',enemyCount:enemies.length,enemyStates:enemies.map(e=>({kind:e.kind,hp:e.hp,slot:e.slot,status:e.status,position:getPosition(e)})),projectiles:projectiles.length,renderHeartbeatMs:Math.round(performance.now()-lastFrameWall),lastDrawError});
})();
