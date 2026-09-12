(() => {
  'use strict';

  const W = 1920;
  const H = 1080;
  const SIDEWALK_Y = 840;
  const SIDEWALK_H = 240;
  const GROUND_Y = 848;
  const CITY_H = 760;

  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => [...document.querySelectorAll(sel)];

  const menuScreen = $('#menuScreen');
  const selectScreen = $('#selectScreen');
  const gameScreen = $('#gameScreen');
  const gameOverScreen = $('#gameOverScreen');
  const characterGrid = $('#characterGrid');
  const canvas = $('#gameCanvas');
  const ctx = canvas.getContext('2d', { alpha: false });
  const transition = $('#transition');
  const introOverlay = $('#introOverlay');
  const introVideo = $('#introVideo');
  const loading = $('#loading');

  const bgMusic = $('#bgMusic');
  const booSound = $('#booSound');
  const evilSound = $('#evilSound');

  bgMusic.volume = 0.52;
  booSound.volume = 0.78;
  evilSound.volume = 0.78;

  const characters = [
    { name: 'Kis vámpír', front: 'assets/images/characters/vampire_front.png', side: 'assets/images/characters/vampire_side.png' },
    { name: 'Csontváz', front: 'assets/images/characters/skeleton_front.png', side: 'assets/images/characters/skeleton_side.png' },
    { name: 'Szellem', front: 'assets/images/characters/ghost_front.png', side: 'assets/images/characters/ghost_side.png' },
    { name: 'Múmia', front: 'assets/images/characters/mummy_front.png', side: 'assets/images/characters/mummy_side.png' },
    { name: 'Varázsló', front: 'assets/images/characters/wizard_front.png', side: 'assets/images/characters/wizard_side.png' },
    { name: 'Boszorkány', front: 'assets/images/characters/little_witch_front.png', side: 'assets/images/characters/little_witch_side.png' },
    { name: 'Tökjelmez', front: 'assets/images/characters/pumpkin_front.png', side: 'assets/images/characters/pumpkin_side.png' },
    { name: 'Farkas', front: 'assets/images/characters/wolf_front.png', side: 'assets/images/characters/wolf_side.png' }
  ];

  const imageSources = {
    sky: 'assets/images/game/sky.jpg',
    city: 'assets/images/game/city.png',
    sidewalk: 'assets/images/game/sidewalk.jpg',
    frog: 'assets/images/game/frog.png',
    witch: 'assets/images/game/witch.png',
    chocolate: 'assets/images/game/chocolate.png',
    lollipop: 'assets/images/game/lollipop.png',
    candy: 'assets/images/game/candy.png'
  };

  const imgs = {};
  const characterImgs = characters.map(() => ({ front: null, side: null }));

  let currentScreen = 'menu';
  let selectedCharacter = -1;
  let musicEnabled = true;
  let audioUnlocked = false;
  let introFinished = false;

  let score = 0;
  let bestScore = readBestScore();
  let speed = 430;
  let cityX = 0;
  let sidewalkX = 0;
  let fogX = 0;
  let charY = GROUND_Y;
  let charVy = 0;
  let jumpsUsed = 0;
  let nextSweetAt = 0;
  let nextFrogAt = 0;
  let sweets = [];
  let frogs = [];
  let dying = false;
  let dyingStarted = 0;
  let witchX = 0;
  let witchY = 0;
  let lastFrame = performance.now();
  let raf = 0;

  function readBestScore() {
    try { return Number(localStorage.getItem('mazsola_halloween_best') || 0); }
    catch { return 0; }
  }

  function writeBestScore(value) {
    try { localStorage.setItem('mazsola_halloween_best', String(value)); }
    catch { /* localStorage can be blocked in privacy modes */ }
  }

  function showScreen(name) {
    currentScreen = name;
    [menuScreen, selectScreen, gameScreen, gameOverScreen].forEach(el => el.classList.remove('active'));
    if (name === 'menu') menuScreen.classList.add('active');
    if (name === 'select') selectScreen.classList.add('active');
    if (name === 'game') gameScreen.classList.add('active');
    if (name === 'gameover') {
      gameScreen.classList.add('active');
      gameOverScreen.classList.add('active');
    }
    updateMusicButtons();
  }

  function buildCharacterGrid() {
    characterGrid.innerHTML = '';
    characters.forEach((ch, index) => {
      const card = document.createElement('button');
      card.type = 'button';
      card.className = 'character-card';
      card.dataset.index = String(index);
      card.innerHTML = `<img src="${ch.front}" alt="${ch.name}"><span>${ch.name}</span>`;
      card.addEventListener('click', () => chooseCharacter(index));
      characterGrid.appendChild(card);
    });
  }

  function chooseCharacter(index) {
    unlockAudio();
    if (selectedCharacter === index) {
      playSfx(evilSound);
      transition.classList.add('show');
      setTimeout(() => {
        startGame();
        requestAnimationFrame(() => {
          requestAnimationFrame(() => transition.classList.remove('show'));
        });
      }, 620);
      return;
    }

    selectedCharacter = index;
    $$('.character-card').forEach((card, i) => card.classList.toggle('selected', i === index));
  }

  function unlockAudio() {
    if (!audioUnlocked) audioUnlocked = true;
    if (musicEnabled) ensureMusic();
  }

  function ensureMusic() {
    if (!musicEnabled) return;
    bgMusic.play().catch(() => {});
  }

  function toggleMusic() {
    unlockAudio();
    musicEnabled = !musicEnabled;
    if (musicEnabled) ensureMusic();
    else bgMusic.pause();
    updateMusicButtons();
  }

  function updateMusicButtons() {
    $$('.music-btn').forEach(btn => {
      btn.textContent = musicEnabled ? '♫' : '×';
      btn.setAttribute('aria-label', musicEnabled ? 'Zene kikapcsolása' : 'Zene bekapcsolása');
    });
  }

  function playSfx(audio) {
    try {
      audio.pause();
      audio.currentTime = 0;
      audio.play().catch(() => {});
    } catch { /* ignore */ }
  }

  function startGame() {
    unlockAudio();
    score = 0;
    speed = 430;
    cityX = 0;
    sidewalkX = 0;
    fogX = 0;
    charY = GROUND_Y;
    charVy = 0;
    jumpsUsed = 0;
    sweets = [];
    frogs = [];
    dying = false;
    const now = performance.now();
    nextSweetAt = now + 550 + Math.random() * 500;
    nextFrogAt = now + 1700 + Math.random() * 1400;
    lastFrame = now;
    showScreen('game');
    cancelAnimationFrame(raf);
    raf = requestAnimationFrame(gameLoop);
  }

  function jump() {
    if (currentScreen !== 'game' || dying || jumpsUsed >= 2) return;
    charVy = jumpsUsed === 0 ? -860 : -1080;
    jumpsUsed += 1;
  }

  function gameLoop(now) {
    if (currentScreen !== 'game') return;
    const dt = Math.min(0.033, Math.max(0, (now - lastFrame) / 1000));
    lastFrame = now;
    updateGame(dt, now);
    drawGame();
    if (currentScreen === 'game') raf = requestAnimationFrame(gameLoop);
  }

  function updateGame(dt, now) {
    if (dying) {
      const targetX = 265;
      const targetY = charY - 300;
      const dx = targetX - witchX;
      const dy = targetY - witchY;
      const dist = Math.hypot(dx, dy) || 1;
      const step = Math.min(dist, 1180 * dt);
      witchX += dx / dist * step;
      witchY += dy / dist * step;
      if (dist < 95 || now - dyingStarted > 2600) endGame();
      return;
    }

    speed = Math.min(760, 430 + score * 5.5);

    const cityW = CITY_H * imgs.city.width / imgs.city.height;
    cityX -= speed * 0.28 * dt;
    while (cityX <= -cityW) cityX += cityW;

    const sidewalkW = SIDEWALK_H * imgs.sidewalk.width / imgs.sidewalk.height;
    sidewalkX -= speed * dt;
    while (sidewalkX <= -sidewalkW) sidewalkX += sidewalkW;

    fogX -= speed * 0.07 * dt;
    if (fogX < -700) fogX += 700;

    charVy += 1900 * dt;
    charY += charVy * dt;
    if (charY >= GROUND_Y) {
      charY = GROUND_Y;
      charVy = 0;
      jumpsUsed = 0;
    }

    if (now >= nextSweetAt) {
      spawnSweet();
      nextSweetAt = now + 500 + Math.random() * 650;
    }

    if (now >= nextFrogAt) {
      spawnFrog();
      nextFrogAt = now + 1750 + Math.random() * 2400;
    }

    sweets.forEach(it => { it.x -= speed * dt; });
    frogs.forEach(it => { it.x -= speed * dt; });

    const hit = characterHitbox();
    sweets = sweets.filter(it => {
      if (rectsOverlap(hit, insetRect(it, 0.15))) {
        score += 1;
        return false;
      }
      return it.x + it.w >= 0;
    });

    for (let i = frogs.length - 1; i >= 0; i -= 1) {
      const frog = frogs[i];
      if (rectsOverlap(hit, { x: frog.x + frog.w * .16, y: frog.y + frog.h * .25, w: frog.w * .68, h: frog.h * .65 })) {
        frogs.splice(i, 1);
        triggerWitch(now);
        break;
      }
      if (frog.x + frog.w < 0) frogs.splice(i, 1);
    }
  }

  function spawnSweet() {
    const count = Math.random() < 0.32 ? 2 : 1;
    for (let i = 0; i < count; i += 1) {
      const type = Math.floor(Math.random() * 3);
      const size = type === 1 ? 120 : 112;
      const minY = 75;
      const maxY = GROUND_Y - size - 12;
      const y = minY + Math.random() * (maxY - minY);
      const x = W + 70 + i * (145 + Math.random() * 100);
      sweets.push({ x, y, w: size, h: size, type });
    }
  }

  function spawnFrog() {
    const w = 138 + Math.random() * 28;
    const h = w * .75;
    const x = W + 70 + Math.random() * 260;
    frogs.push({ x, y: GROUND_Y - h, w, h });
  }

  function characterMetrics() {
    const index = selectedCharacter >= 0 ? selectedCharacter : 0;
    const img = characterImgs[index].side;
    const h = 320;
    const w = h * img.width / img.height;
    return { img, x: 150, y: charY - h, w, h };
  }

  function characterHitbox() {
    const m = characterMetrics();
    return {
      x: m.x + m.w * .24,
      y: m.y + 32,
      w: m.w * .52,
      h: m.h - 42
    };
  }

  function insetRect(r, amount) {
    return { x: r.x + r.w * amount, y: r.y + r.h * amount, w: r.w * (1 - amount * 2), h: r.h * (1 - amount * 2) };
  }

  function rectsOverlap(a, b) {
    return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
  }

  function triggerWitch(now) {
    dying = true;
    dyingStarted = now;
    witchX = W + 80;
    witchY = 185;
  }

  function endGame() {
    if (score > bestScore) {
      bestScore = score;
      writeBestScore(bestScore);
    }
    $('#finalScore').textContent = `Összegyűjtött édesség: ${score}`;
    $('#bestScore').textContent = `Legjobb eredmény: ${bestScore}`;
    drawGame();
    showScreen('gameover');
  }

  function drawGame() {
    ctx.clearRect(0, 0, W, H);
    drawCover(imgs.sky, 0, 0, W, H);

    const cityW = CITY_H * imgs.city.width / imgs.city.height;
    const cityY = SIDEWALK_Y - CITY_H;
    for (let x = cityX; x < W; x += cityW) ctx.drawImage(imgs.city, x, cityY, cityW, CITY_H);

    drawFog();

    const sidewalkW = SIDEWALK_H * imgs.sidewalk.width / imgs.sidewalk.height;
    for (let x = sidewalkX; x < W; x += sidewalkW) ctx.drawImage(imgs.sidewalk, x, SIDEWALK_Y, sidewalkW, SIDEWALK_H);

    const m = characterMetrics();
    ctx.drawImage(m.img, m.x, m.y, m.w, m.h);

    const sweetImgs = [imgs.chocolate, imgs.lollipop, imgs.candy];
    sweets.forEach(it => ctx.drawImage(sweetImgs[it.type], it.x, it.y, it.w, it.h));
    frogs.forEach(it => ctx.drawImage(imgs.frog, it.x, it.y, it.w, it.h));

    roundRect(ctx, 35, 28, 345, 114, 28, 'rgba(36,17,83,.92)');
    ctx.textAlign = 'left';
    ctx.font = '900 40px Arial';
    ctx.fillStyle = '#fff';
    ctx.fillText(`ÉDESSÉG: ${score}`, 65, 78);
    ctx.font = '900 28px Arial';
    ctx.fillStyle = '#ffe071';
    ctx.fillText(`REKORD: ${bestScore}`, 65, 120);

    if (dying) {
      ctx.fillStyle = 'rgba(0,0,0,.33)';
      ctx.fillRect(0,0,W,H);
      const wh = 380;
      const ww = wh * imgs.witch.width / imgs.witch.height;
      ctx.drawImage(imgs.witch, witchX, witchY, ww, wh);
      ctx.textAlign = 'center';
      ctx.fillStyle = '#fff';
      ctx.font = '900 52px Arial';
      ctx.fillText('Jaj! A boszi viszi az édességet!', W / 2, 150);
    }
  }

  function drawCover(img, x, y, w, h) {
    const s = Math.max(w / img.width, h / img.height);
    const dw = img.width * s;
    const dh = img.height * s;
    ctx.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh);
  }

  function drawFog() {
    ctx.save();
    for (let i = 0; i < 5; i += 1) {
      const x = fogX + i * 520 - 220;
      ctx.fillStyle = i % 2 === 0 ? 'rgba(213,201,255,.08)' : 'rgba(255,255,255,.06)';
      ellipse(x + 310, 685, 310, 75);
      ellipse(x + 470, 750, 290, 72);
    }
    ctx.restore();
  }

  function ellipse(cx, cy, rx, ry) {
    ctx.beginPath();
    ctx.ellipse(cx, cy, rx, ry, 0, 0, Math.PI * 2);
    ctx.fill();
  }

  function roundRect(c, x, y, w, h, r, fill) {
    c.beginPath();
    c.roundRect(x, y, w, h, r);
    c.fillStyle = fill;
    c.fill();
  }

  function bindUi() {
    $('#playBtn').addEventListener('click', () => {
      unlockAudio();
      playSfx(booSound);
      showScreen('select');
    });

    $('#backBtn').addEventListener('click', () => {
      selectedCharacter = -1;
      $$('.character-card').forEach(card => card.classList.remove('selected'));
      showScreen('menu');
    });

    $('#jumpBtn').addEventListener('pointerdown', (e) => {
      e.preventDefault();
      unlockAudio();
      jump();
    });

    $('#againBtn').addEventListener('click', startGame);
    $('#menuBtn').addEventListener('click', () => {
      selectedCharacter = -1;
      $$('.character-card').forEach(card => card.classList.remove('selected'));
      showScreen('menu');
    });

    $$('.music-btn').forEach(btn => btn.addEventListener('click', toggleMusic));

    $('#aboutBtn').addEventListener('click', () => {
      $('#aboutModal').classList.add('open');
      $('#aboutModal').setAttribute('aria-hidden', 'false');
    });
    $('#closeAbout').addEventListener('click', closeAbout);
    $('#aboutModal').addEventListener('click', (e) => { if (e.target.id === 'aboutModal') closeAbout(); });

    window.addEventListener('keydown', (e) => {
      if ((e.code === 'Space' || e.code === 'ArrowUp') && currentScreen === 'game') {
        e.preventDefault();
        unlockAudio();
        jump();
      } else if (e.code === 'Escape') {
        if ($('#aboutModal').classList.contains('open')) closeAbout();
        else if (currentScreen === 'select' || currentScreen === 'gameover') showScreen('menu');
      }
    });

    window.addEventListener('pointerdown', unlockAudio, { once: true });

    $$('.btn, .btn-3d').forEach(btn => {
      btn.addEventListener('pointerdown', () => btn.classList.add('pressed'));
      ['pointerup','pointercancel','pointerleave'].forEach(ev => btn.addEventListener(ev, () => btn.classList.remove('pressed')));
    });
  }

  function closeAbout() {
    $('#aboutModal').classList.remove('open');
    $('#aboutModal').setAttribute('aria-hidden', 'true');
  }

  async function preload() {
    const jobs = [];
    Object.entries(imageSources).forEach(([key, src]) => jobs.push(loadImage(src).then(img => { imgs[key] = img; })));
    characters.forEach((ch, i) => {
      jobs.push(loadImage(ch.front).then(img => { characterImgs[i].front = img; }));
      jobs.push(loadImage(ch.side).then(img => { characterImgs[i].side = img; }));
    });
    await Promise.all(jobs);
  }

  function loadImage(src) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error(`Nem tölthető be: ${src}`));
      img.src = src;
    });
  }

  function startIntro() {
    let finished = false;
    const finish = () => {
      if (finished) return;
      finished = true;
      introFinished = true;
      introOverlay.classList.add('fade');
      setTimeout(() => {
        introOverlay.classList.add('hidden');
        introVideo.pause();
        introVideo.currentTime = 0;
      }, 460);
    };

    introVideo.muted = true;
    introVideo.volume = 0;
    introVideo.currentTime = 0;
    introVideo.play().catch(() => {});
    setTimeout(finish, 3000);
  }

  async function init() {
    buildCharacterGrid();
    bindUi();
    showScreen('menu');
    updateMusicButtons();

    try {
      await preload();
      loading.classList.add('hidden');
      setTimeout(() => loading.remove(), 300);
      startIntro();
    } catch (err) {
      console.error(err);
      loading.textContent = 'Hiba történt a játék betöltésekor.';
    }
  }

  init();
})();
