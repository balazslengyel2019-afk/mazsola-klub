(() => {
  'use strict';

  // This script is loaded on the homepage only. The leaf layer never intercepts taps or clicks.
  if (!document.body.classList.contains('home-page')) return;
  const layer = document.querySelector('.autumn-leaves');
  if (!layer) return;

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const mobile = window.matchMedia('(max-width: 640px)');
  const allLeaves = [...layer.querySelectorAll('.autumn-leaf')];
  const rand = (min, max) => min + Math.random() * (max - min);
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  let states = [];
  let viewWidth = window.innerWidth;
  let viewHeight = window.innerHeight;
  let lastFrame = 0;
  let frameId = 0;
  let mouse = null;

  function resetLeaf(state, initial = false, index = 0) {
    const width = state.element.offsetWidth || 38;
    state.x = rand(0, Math.max(1, viewWidth - width));
    // Each leaf always enters from above the screen, but at a different time.
    state.y = -width - (initial ? rand(0, viewHeight * .65) + index * 30 : rand(0, 160));
    state.speed = rand(28, 59);          // Different natural falling speeds, in px/sec.
    state.drift = rand(-8, 8);
    state.sway = rand(7, 20);
    state.phase = rand(0, Math.PI * 2);
    state.swayRate = rand(.65, 1.35);
    state.angle = rand(-160, 160);
    state.spin = rand(-23, 23);
    state.windX = 0;
    state.windY = 0;
    state.width = width;
  }

  function buildStates() {
    const count = mobile.matches ? Math.min(3, allLeaves.length) : allLeaves.length;
    viewWidth = window.innerWidth;
    viewHeight = window.innerHeight;
    states = allLeaves.slice(0, count).map((element, index) => {
      const state = { element };
      resetLeaf(state, true, index);
      return state;
    });
  }

  function tick(now) {
    if (reduceMotion.matches || document.hidden) {
      frameId = 0;
      lastFrame = 0;
      return;
    }

    const dt = lastFrame ? Math.min((now - lastFrame) / 1000, .05) : 0;
    lastFrame = now;

    for (const leaf of states) {
      leaf.phase += leaf.swayRate * dt;
      leaf.x += (leaf.drift + leaf.windX) * dt;
      leaf.y += (leaf.speed + leaf.windY) * dt;
      leaf.angle += leaf.spin * dt;

      // Any mouse-induced breeze fades gradually, without flicking leaves away.
      leaf.windX *= Math.exp(-2.4 * dt);
      leaf.windY *= Math.exp(-3.2 * dt);

      const x = leaf.x + Math.sin(leaf.phase) * leaf.sway;
      // Keep each leaf near the visible width, even with a gentle side drift.
      leaf.x = clamp(leaf.x, -8, viewWidth - leaf.width + 8);
      leaf.element.style.transform =
        'translate3d(' + x.toFixed(1) + 'px,' + leaf.y.toFixed(1) + 'px,0) rotate(' + leaf.angle.toFixed(1) + 'deg)';

      if (leaf.y > viewHeight + leaf.width) resetLeaf(leaf);
    }

    frameId = window.requestAnimationFrame(tick);
  }

  function start() {
    if (frameId || reduceMotion.matches || document.hidden) return;
    lastFrame = 0;
    frameId = window.requestAnimationFrame(tick);
  }

  // A cursor passing nearby behaves like a small gust of wind.
  // No click/drag interception; buttons, text selection and page scrolling stay intact.
  document.addEventListener('pointermove', (event) => {
    if (event.pointerType === 'touch' || !states.length || reduceMotion.matches) return;
    const now = performance.now();
    const previous = mouse;
    mouse = { x: event.clientX, y: event.clientY, time: now };
    if (!previous || now - previous.time > 160) return;

    const dx = mouse.x - previous.x;
    const dy = mouse.y - previous.y;
    if (dx * dx + dy * dy < 4) return;

    // Distance to the path the mouse crossed, not just its final position.
    const lengthSquared = dx * dx + dy * dy;
    for (const leaf of states) {
      const lx = leaf.x + Math.sin(leaf.phase) * leaf.sway + leaf.width / 2;
      const ly = leaf.y + leaf.width / 2;
      const t = clamp(((lx - previous.x) * dx + (ly - previous.y) * dy) / lengthSquared, 0, 1);
      const distance = Math.hypot(lx - (previous.x + t * dx), ly - (previous.y + t * dy));
      const influence = Math.pow(Math.max(0, 1 - distance / 110), 2);
      if (influence === 0) continue;

      // Hard limits prevent the old "shoots off the screen" behaviour.
      leaf.windX = clamp(leaf.windX + clamp(dx, -65, 65) * .28 * influence, -33, 33);
      leaf.windY = clamp(leaf.windY + clamp(dy, -65, 65) * .09 * influence, -9, 9);
    }
  }, { passive: true });

  window.addEventListener('resize', () => {
    buildStates();
    mouse = null;
    start();
  }, { passive: true });

  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) start();
  });

  if (reduceMotion.addEventListener) {
    reduceMotion.addEventListener('change', () => {
      if (reduceMotion.matches) {
        window.cancelAnimationFrame(frameId);
        frameId = 0;
        lastFrame = 0;
      } else {
        start();
      }
    });
  }

  buildStates();
  start();
})();
