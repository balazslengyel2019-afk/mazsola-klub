(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

  const app = $('#app');
  const intro = $('#intro');
  const introVideo = $('#introVideo');
  const keyboard = $('#keyboard');
  const toast = $('#toast');

  const instrumentNames = {
    piano: 'Zongora',
    bass: 'Bőgő',
    violin: 'Hegedű',
    xylophone: 'Xilofon',
    cimbalom: 'Cimbalom',
    synth: 'Szinti',
    tonguedrum: 'Nyelvdob'
  };

  const whiteMidi = [60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83];
  const blackMidi = [61, 63, 66, 68, 70, 73, 75, 78, 80, 82];
  const blackAfter = [0, 1, 3, 4, 5, 7, 8, 10, 11, 12];
  const colors = ['#ff4257','#ff7f2a','#f6a928','#ffd72f','#84d63d','#38c95e','#19bb7b','#25ace0','#337ee7','#485edb','#7e4fd6','#9f4bd5','#cf4bc0','#f35f96'];

  const songs = [
    {
      name: 'Boci, boci tarka',
      notes: [0,2,0,2,4,4,0,2,0,2,4,4,7,6,5,4,3,5,4,3,2,1,0,0]
    },
    {
      name: 'Érik a szőlő, hajlik a vessző',
      notes: [8,8,5,8,9,7,7,8,9,8,7,6,5,5,8,5,4,4,1,4,3,3,4,4,3,2,1,1,4,1]
    },
    {
      name: 'Hej, Dunáról fúj a szél',
      notes: [8,8,7,5,8,8,7,8,8,7,5,8,8,7,8,5,4,5,5,5,4,4,3,1,4,4,3,4,4,3,1,4,4,3,4,1,0,1,1,1]
    }
  ];

  let currentInstrument = 'piano';
  let metronomeOn = false;
  let echoOn = false;
  let metronomeTimer = null;

  let recording = false;
  let recordingStartedAt = 0;
  let recordedNotes = [];
  let playback = false;
  let playbackTimers = [];

  let lesson = { active: false, songIndex: 0, position: 0, finishTimer: null };
  const activePointers = new Map();
  const keyboardPointers = new Map();

  class AudioEngine {
    constructor() {
      this.ctx = null;
      this.master = null;
      this.buffers = new Map();
      this.loading = null;
      this.available = true;
    }

    async ensure() {
      if (!this.available) return false;
      try {
        if (!this.ctx) {
          const AC = window.AudioContext || window.webkitAudioContext;
          if (!AC) throw new Error('Web Audio nem támogatott');
          this.ctx = new AC({ latencyHint: 'interactive' });
          this.master = this.ctx.createGain();
          this.master.gain.value = 0.9;
          this.master.connect(this.ctx.destination);
        }
        if (this.ctx.state === 'suspended') await this.ctx.resume();
        if (!this.loading) this.loading = this.loadAll();
        await this.loading;
        return true;
      } catch (err) {
        console.warn('AudioEngine fallback:', err);
        this.available = false;
        return false;
      }
    }

    async loadAll() {
      const names = Object.keys(instrumentNames);
      const files = [];
      for (const inst of names) {
        files.push([`${inst}_c4`, `assets/audio/${inst}_c4.wav`]);
        files.push([`${inst}_c5`, `assets/audio/${inst}_c5.wav`]);
      }
      files.push(['metronome', 'assets/audio/metronome_click.wav']);

      await Promise.all(files.map(async ([key, url]) => {
        const res = await fetch(url, { cache: 'force-cache' });
        if (!res.ok) throw new Error(`${url}: ${res.status}`);
        const arrayBuffer = await res.arrayBuffer();
        const buffer = await this.ctx.decodeAudioData(arrayBuffer.slice(0));
        this.buffers.set(key, buffer);
      }));
    }

    async play(inst, midi, volume = 1) {
      const ok = await this.ensure();
      if (!ok) return this.playFallback(inst, midi, volume);

      const root = midi < 72 ? 'c4' : 'c5';
      const anchor = midi < 72 ? 60 : 72;
      const buffer = this.buffers.get(`${inst}_${root}`);
      if (!buffer) return null;

      const source = this.ctx.createBufferSource();
      source.buffer = buffer;
      source.playbackRate.value = Math.pow(2, (midi - anchor) / 12);

      const noteGain = this.ctx.createGain();
      noteGain.gain.value = Math.max(0, Math.min(1, 0.78 * volume));
      source.connect(noteGain);
      noteGain.connect(this.master);

      if (echoOn) {
        const wet = this.ctx.createGain();
        wet.gain.value = 0.24 * volume;
        const delay = this.ctx.createDelay(0.7);
        delay.delayTime.value = 0.16;
        const feedback = this.ctx.createGain();
        feedback.gain.value = 0.28;
        source.connect(wet);
        wet.connect(delay);
        delay.connect(feedback);
        feedback.connect(delay);
        delay.connect(this.master);
      }

      source.start();
      return {
        type: 'webaudio',
        source,
        gain: noteGain,
        stop: () => {
          try {
            const now = this.ctx.currentTime;
            noteGain.gain.cancelScheduledValues(now);
            noteGain.gain.setValueAtTime(noteGain.gain.value, now);
            noteGain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);
            source.stop(now + 0.14);
          } catch (_) {}
        }
      };
    }

    playFallback(inst, midi, volume = 1) {
      const root = midi < 72 ? 'c4' : 'c5';
      const anchor = midi < 72 ? 60 : 72;
      const audio = new Audio(`assets/audio/${inst}_${root}.wav`);
      audio.preload = 'auto';
      audio.volume = Math.max(0, Math.min(1, 0.75 * volume));
      audio.playbackRate = Math.max(0.5, Math.min(2, Math.pow(2, (midi - anchor) / 12)));
      audio.play().catch(() => {});
      return {
        type: 'htmlaudio',
        stop: () => setTimeout(() => {
          try { audio.pause(); audio.currentTime = 0; } catch (_) {}
        }, 80)
      };
    }

    async click() {
      const ok = await this.ensure();
      if (!ok) {
        const a = new Audio('assets/audio/metronome_click.wav');
        a.volume = 0.8;
        a.play().catch(() => {});
        return;
      }
      const buffer = this.buffers.get('metronome');
      if (!buffer) return;
      const source = this.ctx.createBufferSource();
      const gain = this.ctx.createGain();
      source.buffer = buffer;
      gain.gain.value = 0.82;
      source.connect(gain);
      gain.connect(this.master);
      source.start();
    }
  }

  const audioEngine = new AudioEngine();

  function finishIntro() {
    if (!intro || intro.classList.contains('fade-out')) return;
    intro.classList.add('fade-out');
    setTimeout(() => {
      intro.classList.add('hidden');
      app.classList.remove('hidden');
      try { introVideo.pause(); } catch (_) {}
    }, 360);
  }

  introVideo?.addEventListener('ended', finishIntro);
  introVideo?.addEventListener('error', () => setTimeout(finishIntro, 180));
  setTimeout(finishIntro, 3150);

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove('show'), 1050);
  }

  function makeKeyboard() {
    const count = whiteMidi.length;
    whiteMidi.forEach((midi, index) => {
      const key = document.createElement('button');
      key.type = 'button';
      key.className = 'white-key';
      key.dataset.kind = 'white';
      key.dataset.index = String(index);
      key.dataset.midi = String(midi);
      key.style.left = `${index * (100 / count)}%`;
      key.style.width = `${100 / count + 0.03}%`;
      key.innerHTML = `<span class="color-band" style="background:${colors[index]}">${index + 1}</span>`;
      keyboard.appendChild(key);
    });

    blackMidi.forEach((midi, index) => {
      const key = document.createElement('button');
      key.type = 'button';
      key.className = 'black-key';
      key.dataset.kind = 'black';
      key.dataset.index = String(index);
      key.dataset.midi = String(midi);
      const center = (blackAfter[index] + 1) * (100 / count);
      key.style.left = `calc(${center}% - ${100 / count * 0.30}%)`;
      key.style.width = `${100 / count * 0.60}%`;
      key.textContent = String(index + 1);
      keyboard.appendChild(key);
    });

    keyboard.addEventListener('pointerdown', onPointerDown);
    keyboard.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    window.addEventListener('pointercancel', onPointerUp);
  }

  function keyAtPoint(x, y) {
    return document.elementFromPoint(x, y)?.closest?.('.white-key,.black-key') || null;
  }

  function onPointerDown(event) {
    const key = event.target.closest('.white-key,.black-key');
    if (!key) return;
    event.preventDefault();
    try { keyboard.setPointerCapture(event.pointerId); } catch (_) {}
    pressKey(event.pointerId, key);
  }

  function onPointerMove(event) {
    if (!activePointers.has(event.pointerId)) return;
    const key = keyAtPoint(event.clientX, event.clientY);
    const current = activePointers.get(event.pointerId)?.key;
    if (key && key !== current) {
      releaseKey(event.pointerId);
      pressKey(event.pointerId, key);
    }
  }

  function onPointerUp(event) {
    if (activePointers.has(event.pointerId)) releaseKey(event.pointerId);
  }

  function pressKey(pointerId, key) {
    if (!key || activePointers.has(pointerId)) return;
    const midi = Number(key.dataset.midi);
    const now = performance.now();
    const note = {
      key,
      handle: null,
      midi,
      inst: currentInstrument,
      downAt: now,
      recordOffset: recording ? now - recordingStartedAt : null
    };

    key.classList.add('pressed');
    activePointers.set(pointerId, note);
    evaluateLesson(key);

    audioEngine.play(note.inst, midi).then(handle => {
      if (activePointers.get(pointerId) === note) note.handle = handle;
      else handle?.stop?.();
    }).catch(() => {});
  }

  function releaseKey(pointerId) {
    const note = activePointers.get(pointerId);
    if (!note) return;
    note.key.classList.remove('pressed');
    note.handle?.stop?.();

    if (recording && note.recordOffset !== null && recordedNotes.length < 600) {
      recordedNotes.push({
        inst: note.inst,
        midi: note.midi,
        start: note.recordOffset,
        duration: Math.max(120, Math.min(5000, performance.now() - note.downAt))
      });
    }
    activePointers.delete(pointerId);
  }

  async function chooseInstrument(inst, preview = true) {
    currentInstrument = inst;
    $$('.instrument-card').forEach(btn => {
      const active = btn.dataset.inst === inst;
      btn.classList.toggle('active', active);
      btn.setAttribute('aria-pressed', String(active));
    });
    showToast(instrumentNames[inst]);
    if (preview) {
      const handle = await audioEngine.play(inst, 60, 0.9);
      setTimeout(() => handle?.stop?.(), 430);
    }
  }

  $$('.instrument-card').forEach(btn => {
    btn.addEventListener('click', () => chooseInstrument(btn.dataset.inst, true));
  });

  function setToggle(button, on) {
    button.setAttribute('aria-pressed', String(on));
  }

  $('#metronomeBtn').addEventListener('click', async () => {
    metronomeOn = !metronomeOn;
    setToggle($('#metronomeBtn'), metronomeOn);
    clearInterval(metronomeTimer);
    metronomeTimer = null;
    if (metronomeOn) {
      await audioEngine.click();
      metronomeTimer = setInterval(() => audioEngine.click(), 600);
      showToast('Metronóm bekapcsolva');
    } else {
      showToast('Metronóm kikapcsolva');
    }
  });

  $('#echoBtn').addEventListener('click', async () => {
    echoOn = !echoOn;
    setToggle($('#echoBtn'), echoOn);
    await audioEngine.ensure();
    showToast(echoOn ? 'Visszhang bekapcsolva' : 'Visszhang kikapcsolva');
  });

  $('#scoreBtn').addEventListener('click', () => openModal('songModal'));
  $$('.song-list button').forEach(btn => {
    btn.addEventListener('click', () => {
      closeModal('songModal');
      startLesson(Number(btn.dataset.song));
    });
  });
  $('#lessonClose').addEventListener('click', stopLesson);

  function startLesson(songIndex) {
    lesson.active = true;
    lesson.songIndex = songIndex;
    lesson.position = 0;
    clearTimeout(lesson.finishTimer);
    const song = songs[songIndex];
    $('#lessonTitle').textContent = song.name;
    $('#lessonPanel').classList.remove('hidden', 'wrong');
    $('#lessonComplete').classList.add('hidden');
    renderLesson();
  }

  function stopLesson() {
    lesson.active = false;
    lesson.position = 0;
    clearTimeout(lesson.finishTimer);
    $('#lessonPanel').classList.add('hidden');
    $$('.white-key').forEach(key => key.classList.remove('lesson-target'));
  }

  function evaluateLesson(key) {
    if (!lesson.active) return;
    const song = songs[lesson.songIndex];
    if (lesson.position >= song.notes.length) return;

    const expected = song.notes[lesson.position];
    const correct = key.dataset.kind === 'white' && Number(key.dataset.index) === expected;
    const panel = $('#lessonPanel');

    if (correct) {
      lesson.position += 1;
      panel.classList.remove('wrong');
      renderLesson();
      if (lesson.position >= song.notes.length) finishLesson();
    } else {
      panel.classList.remove('wrong');
      void panel.offsetWidth;
      panel.classList.add('wrong');
    }
  }

  function renderLesson() {
    const song = songs[lesson.songIndex];
    const notes = $('#lessonNotes');
    notes.innerHTML = '';
    $$('.white-key').forEach(key => key.classList.remove('lesson-target'));

    if (lesson.position < song.notes.length) {
      $(`.white-key[data-index="${song.notes[lesson.position]}"]`)?.classList.add('lesson-target');
    }

    const visibleCount = 9;
    let start = Math.max(0, lesson.position - 2);
    if (start + visibleCount > song.notes.length) start = Math.max(0, song.notes.length - visibleCount);

    song.notes.slice(start, start + visibleCount).forEach((keyIndex, localIndex) => {
      const absoluteIndex = start + localIndex;
      const chip = document.createElement('span');
      chip.className = 'note-chip';
      if (absoluteIndex < lesson.position) chip.classList.add('done');
      if (absoluteIndex === lesson.position) chip.classList.add('current');
      chip.style.background = colors[keyIndex];
      chip.textContent = String(keyIndex + 1);
      notes.appendChild(chip);
    });

    $('#lessonProgress').textContent = `${Math.min(lesson.position + 1, song.notes.length)} / ${song.notes.length}`;
  }

  function finishLesson() {
    $$('.white-key').forEach(key => key.classList.remove('lesson-target'));
    $('#lessonNotes').innerHTML = '';
    $('#lessonProgress').textContent = '';
    $('#lessonComplete').classList.remove('hidden');
    lesson.finishTimer = setTimeout(stopLesson, 3000);
  }

  $('#recordBtn').addEventListener('click', async () => {
    await audioEngine.ensure();
    if (playback) stopPlayback();
    recording = !recording;
    const button = $('#recordBtn');
    button.classList.toggle('recording', recording);
    button.querySelector('b').textContent = recording ? 'LEÁLLÍTÁS' : 'FELVÉTEL';

    if (recording) {
      recordedNotes = [];
      recordingStartedAt = performance.now();
      $('#playBtn').disabled = true;
      showToast('Felvétel elindítva');
    } else {
      $('#playBtn').disabled = recordedNotes.length === 0;
      showToast(recordedNotes.length ? 'Felvétel kész' : 'Nem rögzítettél hangot');
    }
  });

  $('#playBtn').addEventListener('click', () => {
    if (playback) stopPlayback();
    else playRecording();
  });

  function playRecording() {
    if (!recordedNotes.length) return;
    recording = false;
    $('#recordBtn').classList.remove('recording');
    $('#recordBtn b').textContent = 'FELVÉTEL';
    playback = true;
    $('#playBtn').classList.add('playing');
    $('#playBtn b').textContent = 'LEÁLLÍTÁS';

    let end = 0;
    recordedNotes.forEach(note => {
      end = Math.max(end, note.start + note.duration);
      const timer = setTimeout(async () => {
        if (!playback) return;
        const handle = await audioEngine.play(note.inst, note.midi);
        const stopTimer = setTimeout(() => handle?.stop?.(), note.duration);
        playbackTimers.push(stopTimer);
      }, note.start);
      playbackTimers.push(timer);
    });

    playbackTimers.push(setTimeout(stopPlayback, end + 200));
  }

  function stopPlayback() {
    playback = false;
    playbackTimers.forEach(clearTimeout);
    playbackTimers = [];
    $('#playBtn').classList.remove('playing');
    $('#playBtn b').textContent = 'LEJÁTSZÁS';
  }

  function openModal(id) {
    $('#' + id).classList.remove('hidden');
  }

  function closeModal(id) {
    $('#' + id).classList.add('hidden');
  }

  $('#aboutOpen').addEventListener('click', () => openModal('aboutModal'));
  $$('[data-close]').forEach(btn => btn.addEventListener('click', () => closeModal(btn.dataset.close)));
  $$('.modal').forEach(modal => modal.addEventListener('pointerdown', event => {
    if (event.target === modal) closeModal(modal.id);
  }));

  const keyMap = {
    a:60,w:61,s:62,e:63,d:64,f:65,t:66,g:67,y:68,h:69,u:70,j:71,k:72,o:73,l:74,p:75,'ő':76,'[':77,']':79
  };

  document.addEventListener('keydown', event => {
    const lookup = event.key.toLowerCase();
    const midi = keyMap[lookup];
    if (event.repeat || midi === undefined) return;
    const key = $(`[data-midi="${midi}"]`);
    if (!key) return;
    const id = `kb-${lookup}`;
    if (keyboardPointers.has(id)) return;
    keyboardPointers.set(id, true);
    pressKey(id, key);
  });

  document.addEventListener('keyup', event => {
    const id = `kb-${event.key.toLowerCase()}`;
    if (!keyboardPointers.has(id)) return;
    releaseKey(id);
    keyboardPointers.delete(id);
  });

  window.addEventListener('pointerdown', () => audioEngine.ensure(), { once: true, passive: true });

  makeKeyboard();
})();
