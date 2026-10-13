/* Shared accessible mobile navigation for Mazsola Klub. */
(() => {
  'use strict';

  const init = () => {
    document.querySelectorAll('.site-header').forEach((header, index) => {
      const nav = header.querySelector(':scope > nav');
      if (!nav || !header.querySelector(':scope > .site-brand') || header.querySelector('.mobile-menu-toggle')) return;

      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'mobile-menu-toggle';
      button.setAttribute('aria-label', 'Menü megnyitása');
      button.setAttribute('aria-expanded', 'false');
      nav.id = nav.id || 'mobile-site-navigation-' + (index + 1);
      button.setAttribute('aria-controls', nav.id);
      button.innerHTML = '<span class="mobile-menu-icon" aria-hidden="true"><span></span><span></span><span></span></span><span class="mobile-menu-caption">Menü</span>';

      header.insertBefore(button, nav);
      header.classList.add('mobile-menu-ready');

      const closeMenu = () => {
        header.classList.remove('mobile-menu-open');
        button.setAttribute('aria-expanded', 'false');
        button.setAttribute('aria-label', 'Menü megnyitása');
        button.querySelector('.mobile-menu-caption').textContent = 'Menü';
      };

      button.addEventListener('click', () => {
        const shouldOpen = !header.classList.contains('mobile-menu-open');
        if (shouldOpen) {
          header.classList.add('mobile-menu-open');
          button.setAttribute('aria-expanded', 'true');
          button.setAttribute('aria-label', 'Menü bezárása');
          button.querySelector('.mobile-menu-caption').textContent = 'Bezárás';
        } else {
          closeMenu();
        }
      });

      nav.addEventListener('click', (event) => {
        if (event.target.closest('a')) closeMenu();
      });

      document.addEventListener('pointerdown', (event) => {
        if (!header.contains(event.target)) closeMenu();
      });

      document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && header.classList.contains('mobile-menu-open')) {
          closeMenu();
          button.focus();
        }
      });

      const desktop = window.matchMedia('(min-width: 981px)');
      if (desktop.addEventListener) desktop.addEventListener('change', closeMenu);
    });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();