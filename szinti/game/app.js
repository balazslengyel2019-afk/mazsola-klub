(() => {
  'use strict';
  const $ = s => document.querySelector(s);
  const $$ = s => [...document.querySelectorAll(s)];
  const stage = $('#stage');
  const intro = $('#intro');
  const introVideo = $('#introVideo');
  const keyboard = $('#keyboard');
  const toast = $('#toast');

  const instruments = ['piano','bass','violin','xylophone','cimbalom','synth','tonguedrum'];
  const instNames = {piano:'Zongora',bass:'Bőgő',violin:'Hegedű',xylophone:'Xilofon',cimbalom:'Cimbalom',synth:'Szinti',tonguedrum:'Nyelvdob'};
  const whiteMidi = [60,62,64,65,67,69,71,72,74,76,77,79,81,83];
  const blackMidi = [61,63,66,68,70,73,75,78,80,82];
  const blackAfter = [0,1,3,4,5,7,8,10,11,12];
  const colors = ['#ff4154','#ff8527','#ffb02b','#ffe22a','#85dc39','#30cb62','#18c57f','#2bb7e8','#327fed','#4266e5','#8751dd','#a74fde','#d84fc7','#ff6ca0'];
  const songNames = ['Boci, boci tarka','Érik a szőlő, hajlik a vessző','Hej, Dunáról fúj a szél','Száz liba egy sorba','Kis kece lányom'];
  const songs = [
    [0,2,0,2,4,4,0,2,0,2,4,4,7,6,5,4,3,5,4,3,2,1,0,0],
    [8,8,5,8,9,7,7,8,9,8,7,6,5,5,8,5,4,4,1,4,3,3,4,4,3,2,1,1,4,1],
    [8,8,7,5,8,8,7,8,8,7,5,8,8,7,8,5,4,5,5,5,4,4,3,1,4,4,3,4,4,3,1,4,4,3,4,1,0,1,1,1],
    [11,9,7,8,4,7,11,9,7,8,4,7,12,12,12,12,12,10,11,11,11,11,11,9],
    [1,5,5,5,4,5,3,3,2,1,1,5,5,5,4,5,3,3,2,1,1,2,3,3,4,3,2,3,2,2,2,2,1,2,3,3,4,3,2,3,1,1,1,1]
  ];

  let currentInst = 'piano';
  let metronomeOn = false, hallOn = false;
  let metronomeTimer = null;
  let recording = false, playback = false;
  let recordingStart = 0;
  let recorded = [];
  let playbackTimers = [];
  let lesson = {active:false,song:0,pos:0,finishTimer:null};
  const activePointers = new Map();
  const activeKeyboard = new Map();

  function finishIntro(){
    intro.classList.add('hidden'); stage.classList.remove('hidden');
    try{introVideo.pause();}catch(e){}
  }
  introVideo.addEventListener('ended',finishIntro);
  introVideo.addEventListener('error',()=>setTimeout(finishIntro,250));
  setTimeout(finishIntro,3400);

  function showToast(msg){
    toast.textContent=msg;toast.classList.add('show');
    clearTimeout(showToast.t);showToast.t=setTimeout(()=>toast.classList.remove('show'),1100);
  }

  function audioPath(inst,midi){
    const root=midi<72?'c4':'c5'; return `assets/audio/${inst}_${root}.wav`;
  }
  function startAudio(inst,midi,volScale=1){
    const a=new Audio(audioPath(inst,midi));
    const anchor=midi<72?60:72;
    a.playbackRate=Math.max(.5,Math.min(1.95,Math.pow(2,(midi-anchor)/12)));
    let base=(inst==='xylophone'||inst==='cimbalom')?.72:.68;
    if(inst==='tonguedrum')base=.72;
    a.volume=Math.max(0,Math.min(1,base*volScale));
    const p=a.play(); if(p&&p.catch)p.catch(()=>{});
    if(hallOn && volScale===1){
      setTimeout(()=>startAudio(inst,midi,.20),95);
      setTimeout(()=>startAudio(inst,midi,.09),205);
    }
    return a;
  }
  function stopAudio(a){try{a.pause();a.currentTime=0}catch(e){}}
  function clickMetronome(){const a=new Audio('assets/audio/metronome_click.wav');a.volume=.72;a.play().catch(()=>{});}

  function makeKeyboard(){
    const n=whiteMidi.length;
    whiteMidi.forEach((midi,i)=>{
      const key=document.createElement('button');key.type='button';key.className='white-key';key.dataset.kind='white';key.dataset.index=i;key.dataset.midi=midi;
      key.style.left=`${i*(100/n)}%`;key.style.width=`${100/n+.04}%`;
      key.innerHTML=`<span class="color-band" style="background:${colors[i]}">${i+1}</span>`;
      keyboard.appendChild(key);
    });
    blackMidi.forEach((midi,i)=>{
      const key=document.createElement('button');key.type='button';key.className='black-key';key.dataset.kind='black';key.dataset.index=i;key.dataset.midi=midi;
      const center=(blackAfter[i]+1)*(100/n);key.style.left=`calc(${center}% - 2.0%)`;key.style.width='4.0%';key.textContent=String(i+1);
      keyboard.appendChild(key);
    });
    keyboard.addEventListener('pointerdown',keyDown);
    keyboard.addEventListener('pointermove',keyMove);
    window.addEventListener('pointerup',keyUp);window.addEventListener('pointercancel',keyUp);
  }
  function keyFromPoint(x,y){
    const el=document.elementFromPoint(x,y); return el?.closest?.('.white-key,.black-key') || null;
  }
  function keyDown(e){
    const key=e.target.closest('.white-key,.black-key');if(!key)return;
    e.preventDefault();keyboard.setPointerCapture?.(e.pointerId);pressKey(e.pointerId,key);
  }
  function keyMove(e){
    if(!activePointers.has(e.pointerId))return;
    const key=keyFromPoint(e.clientX,e.clientY);const cur=activePointers.get(e.pointerId)?.key;
    if(key&&key!==cur){releaseKey(e.pointerId);pressKey(e.pointerId,key)}
  }
  function keyUp(e){if(activePointers.has(e.pointerId))releaseKey(e.pointerId)}
  function pressKey(id,key){
    if(!key||activePointers.has(id))return;
    const midi=Number(key.dataset.midi);key.classList.add('pressed');
    const audio=startAudio(currentInst,midi);
    const now=performance.now();
    activePointers.set(id,{key,audio,midi,inst:currentInst,down:now,offset:recording?now-recordingStart:null});
    evaluateLesson(key);
  }
  function releaseKey(id){
    const p=activePointers.get(id);if(!p)return;
    p.key.classList.remove('pressed');stopAudio(p.audio);
    if(recording&&p.offset!==null&&recorded.length<500){recorded.push({inst:p.inst,midi:p.midi,start:p.offset,duration:Math.max(80,Math.min(5000,performance.now()-p.down))})}
    activePointers.delete(id);
  }

  function setInstrument(inst){
    currentInst=inst;$$('.instrument').forEach(b=>b.classList.toggle('active',b.dataset.inst===inst));showToast(instNames[inst]);
  }
  $$('.instrument').forEach(b=>b.addEventListener('click',()=>setInstrument(b.dataset.inst)));

  function setToggle(btn,on){btn.setAttribute('aria-pressed',String(on))}
  $('#metronomeBtn').addEventListener('click',()=>{
    metronomeOn=!metronomeOn;setToggle($('#metronomeBtn'),metronomeOn);
    clearInterval(metronomeTimer);metronomeTimer=null;
    if(metronomeOn){clickMetronome();metronomeTimer=setInterval(clickMetronome,600)}
  });
  $('#hallBtn').addEventListener('click',()=>{hallOn=!hallOn;setToggle($('#hallBtn'),hallOn);showToast(hallOn?'Terem effekt bekapcsolva':'Terem effekt kikapcsolva')});

  $('#scoreBtn').addEventListener('click',()=>openModal('songModal'));
  $$('.song-list button').forEach(b=>b.addEventListener('click',()=>{closeModal('songModal');startLesson(Number(b.dataset.song))}));
  $('#lessonClose').addEventListener('click',stopLesson);

  function startLesson(song){lesson.active=true;lesson.song=song;lesson.pos=0;clearTimeout(lesson.finishTimer);$('#lessonTitle').textContent=songNames[song];$('#lessonStrip').classList.remove('hidden','wrong');$('#lessonComplete').classList.add('hidden');renderLesson()}
  function stopLesson(){lesson.active=false;lesson.pos=0;clearTimeout(lesson.finishTimer);$('#lessonStrip').classList.add('hidden');$$('.white-key').forEach(k=>k.classList.remove('lesson-target'))}
  function evaluateLesson(key){
    if(!lesson.active||lesson.pos>=songs[lesson.song].length)return;
    const expected=songs[lesson.song][lesson.pos];const correct=key.dataset.kind==='white'&&Number(key.dataset.index)===expected;
    if(correct){lesson.pos++;$('#lessonStrip').classList.remove('wrong');renderLesson();if(lesson.pos>=songs[lesson.song].length)finishLesson()}
    else{$('#lessonStrip').classList.remove('wrong');void $('#lessonStrip').offsetWidth;$('#lessonStrip').classList.add('wrong')}
  }
  function renderLesson(){
    const song=songs[lesson.song];const notes=$('#lessonNotes');notes.innerHTML='';
    $$('.white-key').forEach(k=>k.classList.remove('lesson-target'));
    if(lesson.pos<song.length){const target=$(`.white-key[data-index="${song[lesson.pos]}"]`);target?.classList.add('lesson-target')}
    const max=9;let start=Math.max(0,lesson.pos-2);if(start+max>song.length)start=Math.max(0,song.length-max);
    song.slice(start,start+max).forEach((key,j)=>{const idx=start+j;const el=document.createElement('span');el.className='note-chip'+(idx<lesson.pos?' done':'')+(idx===lesson.pos?' current':'');el.style.background=colors[key];el.textContent=key+1;notes.appendChild(el)});
    $('#lessonProgress').textContent=`${Math.min(lesson.pos+1,song.length)} / ${song.length}`;
  }
  function finishLesson(){
    $$('.white-key').forEach(k=>k.classList.remove('lesson-target'));$('#lessonNotes').innerHTML='';$('#lessonProgress').textContent='';$('#lessonComplete').classList.remove('hidden');
    lesson.finishTimer=setTimeout(stopLesson,3000);
  }

  $('#recordBtn').addEventListener('click',()=>{
    if(playback)stopPlayback();
    recording=!recording;const b=$('#recordBtn');b.classList.toggle('recording',recording);b.querySelector('b').textContent=recording?'LEÁLLÍTÁS':'FELVÉTEL';
    if(recording){recorded=[];recordingStart=performance.now();$('#playBtn').disabled=true;showToast('Felvétel elindítva')}
    else{$('#playBtn').disabled=recorded.length===0;showToast('Felvétel kész')}
  });
  $('#playBtn').addEventListener('click',()=>playback?stopPlayback():playRecording());
  function playRecording(){
    if(!recorded.length)return;recording=false;$('#recordBtn').classList.remove('recording');$('#recordBtn b').textContent='FELVÉTEL';
    playback=true;$('#playBtn').classList.add('playing');$('#playBtn b').textContent='LEÁLLÍTÁS';
    let end=0;
    recorded.forEach(n=>{end=Math.max(end,n.start+n.duration);playbackTimers.push(setTimeout(()=>{if(!playback)return;const a=startAudio(n.inst,n.midi);playbackTimers.push(setTimeout(()=>stopAudio(a),n.duration))},n.start))});
    playbackTimers.push(setTimeout(stopPlayback,end+180));
  }
  function stopPlayback(){playback=false;playbackTimers.forEach(clearTimeout);playbackTimers=[];$('#playBtn').classList.remove('playing');$('#playBtn b').textContent='LEJÁTSZÁS'}

  function openModal(id){$('#'+id).classList.remove('hidden')}
  function closeModal(id){$('#'+id).classList.add('hidden')}
  $('#aboutOpen').addEventListener('click',()=>openModal('aboutModal'));
  $$('[data-close]').forEach(b=>b.addEventListener('click',()=>closeModal(b.dataset.close)));
  $$('.modal').forEach(m=>m.addEventListener('pointerdown',e=>{if(e.target===m)closeModal(m.id)}));

  const keyboardMap={a:60,w:61,s:62,e:63,d:64,f:65,t:66,g:67,y:68,h:69,u:70,j:71,k:72,o:73,l:74,p:75,'ő':76,'[':77,']':79};
  document.addEventListener('keydown',e=>{
    if(e.repeat||!keyboardMap[e.key.toLowerCase()])return;const midi=keyboardMap[e.key.toLowerCase()];const key=$(`[data-midi="${midi}"]`);if(!key)return;const id='kb-'+e.key.toLowerCase();if(activeKeyboard.has(id))return;activeKeyboard.set(id,true);pressKey(id,key)
  });
  document.addEventListener('keyup',e=>{const id='kb-'+e.key.toLowerCase();if(activeKeyboard.has(id)){releaseKey(id);activeKeyboard.delete(id)}});

  makeKeyboard();
})();
