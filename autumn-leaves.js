(() => {
  'use strict';
  const layer = document.querySelector('.autumn-leaves');
  if (!layer || !window.PointerEvent || matchMedia('(prefers-reduced-motion: reduce)').matches) return;

  const leaves = [...layer.querySelectorAll('.autumn-leaf')];
  const busy = new WeakSet();
  const drags = new Map();
  const lastSweep = new WeakMap();
  let previousPointer = null;

  const start = (leaf) => {
    if (busy.has(leaf)) return false;
    busy.add(leaf);
    // Freeze the exact animated position so the leaf cannot jump when grabbed.
    const transform = getComputedStyle(leaf).transform;
    leaf.style.animation = 'none';
    leaf.style.transform = transform === 'none' ? 'translate3d(0,0,0)' : transform;
    leaf.style.zIndex = '5';
    return true;
  };

  const reset = (leaf) => {
    leaf.style.removeProperty('animation');
    leaf.style.removeProperty('transform');
    leaf.style.removeProperty('z-index');
    leaf.style.removeProperty('opacity');
    busy.delete(leaf);
  };

  const dismiss = (leaf, dx, dy, speed = 1) => {
    const base = leaf.style.transform;
    const distance = Math.min(440, 170 + Math.hypot(dx, dy) * 1.2);
    const norm = Math.max(1, Math.hypot(dx, dy));
    const endX = dx / norm * distance;
    const endY = dy / norm * distance + 85;
    const rotate = Math.max(-260, Math.min(260, endX * .85));
    const animation = leaf.animate([
      { transform: base, opacity: getComputedStyle(leaf).opacity },
      { transform: `translate3d(${endX}px,${endY}px,0) rotate(${rotate}deg) ${base}`, opacity: 0 }
    ], { duration: Math.max(380, 780 / speed), easing: 'cubic-bezier(.2,.65,.3,1)' });
    animation.finished.then(() => reset(leaf), () => reset(leaf));
  };

  leaves.forEach((leaf) => {
    leaf.addEventListener('pointerdown', (event) => {
      if (event.button !== 0 || !start(leaf)) return;
      event.preventDefault();
      event.stopPropagation();
      const now = performance.now();
      drags.set(event.pointerId, {
        leaf, x: event.clientX, y: event.clientY,
        lastX: event.clientX, lastY: event.clientY, lastTime: now,
        vx: 0, vy: 0, base: leaf.style.transform
      });
      leaf.setPointerCapture(event.pointerId);
      leaf.classList.add('is-grabbed');
    });
  });

  function finish(event) {
    const drag = drags.get(event.pointerId);
    if (!drag) return;
    drags.delete(event.pointerId);
    const { leaf } = drag;
    leaf.classList.remove('is-grabbed');
    if (leaf.hasPointerCapture(event.pointerId)) leaf.releasePointerCapture(event.pointerId);
    const dx = event.clientX - drag.x;
    const dy = event.clientY - drag.y;
    // Quick flicks fly away; slow drags float away on release.
    const velocity = Math.hypot(drag.vx, drag.vy);
    const flightX = velocity > .25 ? drag.vx * 260 : dx * .65 + (dx < 0 ? -100 : 100);
    const flightY = velocity > .25 ? drag.vy * 260 : dy + 80;
    dismiss(leaf, flightX, flightY, 1 + Math.min(1.5, velocity));
  }

  layer.addEventListener('pointermove', (event) => {
    const drag = drags.get(event.pointerId);
    if (!drag) return;
    event.preventDefault();
    const now = performance.now();
    const dt = Math.max(1, now - drag.lastTime);
    drag.vx = (event.clientX - drag.lastX) / dt;
    drag.vy = (event.clientY - drag.lastY) / dt;
    drag.lastX = event.clientX;
    drag.lastY = event.clientY;
    drag.lastTime = now;
    drag.leaf.style.transform = `translate3d(${event.clientX - drag.x}px,${event.clientY - drag.y}px,0) ${drag.base}`;
  });
  layer.addEventListener('pointerup', finish);
  layer.addEventListener('pointercancel', finish);

  // A fast cursor swipe can also brush nearby leaves without clicking.
  const cover = layer.parentElement;
  cover.addEventListener('pointermove', (event) => {
    if (event.pointerType === 'touch' || drags.has(event.pointerId)) return;
    const now = performance.now();
    if (!previousPointer || now - previousPointer.time > 160) {
      previousPointer = { x: event.clientX, y: event.clientY, time: now };
      return;
    }
    const dx = event.clientX - previousPointer.x;
    const dy = event.clientY - previousPointer.y;
    const dt = Math.max(1, now - previousPointer.time);
    previousPointer = { x: event.clientX, y: event.clientY, time: now };
    if (Math.hypot(dx, dy) / dt < .7) return;
    for (const leaf of leaves) {
      if (busy.has(leaf) || (now - (lastSweep.get(leaf) || 0) < 700)) continue;
      const r = leaf.getBoundingClientRect();
      const mx = r.left + r.width / 2, my = r.top + r.height / 2;
      if (Math.hypot(event.clientX - mx, event.clientY - my) > 48) continue;
      lastSweep.set(leaf, now);
      if (start(leaf)) dismiss(leaf, dx * 8, dy * 8, 1.4);
    }
  }, { passive: true });
})();
