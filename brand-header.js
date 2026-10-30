/* Shared accessible mobile navigation for Mazsola Klub. */
(() => {
  'use strict';


  // Accessible branded social icons shared by all Mazsola Klub pages.
  const socialSites = [["YouTube","https://www.youtube.com/@mazsola-klub","youtube","M23.498 6.186a3.002 3.002 0 0 0-2.108-2.108C19.53 3.58 12 3.58 12 3.58s-7.53 0-9.39.498A3 3 0 0 0 .502 6.186 31.1 31.1 0 0 0 0 12c0 1.962.168 3.9.502 5.814a3.002 3.002 0 0 0 2.108 2.108c1.86.498 9.39.498 9.39.498s7.53 0 9.39-.498a3.002 3.002 0 0 0 2.108-2.108C23.832 15.9 24 13.962 24 12c0-1.962-.168-3.9-.502-5.814zM9.6 15.55v-7.1L15.84 12z"],["TikTok","https://www.tiktok.com/@mazsolaklub","tiktok","M12.525.02c1.31-.02 2.61-.01 3.91-.02.08 1.53.63 3.09 1.75 4.17 1.12 1.11 2.7 1.63 4.24 1.8v4.03c-1.46-.05-2.93-.36-4.26-.99-.58-.26-1.12-.59-1.65-.94-.01 2.92.01 5.85-.02 8.76-.08 1.39-.54 2.77-1.35 3.91-1.33 1.95-3.6 3.22-5.96 3.26-1.45.08-2.9-.31-4.13-1.04C3.01 20.62 1.98 18.3 2.31 16.04c.24-2.01 1.42-3.88 3.19-4.91 1.45-.85 3.18-1.18 4.84-.9v4.33c-1.33-.43-2.94-.03-3.73 1.16-.77 1.03-.69 2.51.21 3.45.89.93 2.47 1.09 3.57.46.7-.4 1.1-1.18 1.13-1.97.03-5.88.01-11.76.01-17.64z"],["Facebook","https://www.facebook.com/mazsolaklub/","facebook","M15.8 22v-8.3h2.76l.42-3.25H15.8V8.4c0-.94.26-1.58 1.59-1.58H19V3.9a23 23 0 0 0-2.44-.13c-2.41 0-4.07 1.48-4.07 4.2v2.48H9.75v3.25h2.74V22z"]];
  const renderSocials = (container) => {
    if (container.dataset.socialReady === 'true') return;
    container.dataset.socialReady = 'true';
    if (!container.hasAttribute('aria-label')) container.setAttribute('aria-label','Mazsola Klub közösségi oldalai');
    for (const [name,href,kind,path] of socialSites) {
      const link=document.createElement('a');
      link.href=href;link.rel='noopener noreferrer';link.target='_blank';
      link.className='social-icon social-icon--'+kind;
      link.setAttribute('aria-label','Mazsola Klub '+name+' (új lapon)');
      link.title='Mazsola Klub '+name;
      link.innerHTML='<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false"><path d="'+path+'"/></svg>';
      container.appendChild(link);
    }
  };

  const init = () => {
    document.querySelectorAll('[data-social-links]').forEach(renderSocials);
    document.querySelectorAll('.site-header').forEach((header, index) => {
      const nav = header.querySelector(':scope > nav');
      if (nav) {
        const old = nav.querySelector('.nav-yt');
        if (old) {
          const group = document.createElement('span');
          group.className='social-links social-links--header';
          renderSocials(group);
          old.replaceWith(group);
        }
      }
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