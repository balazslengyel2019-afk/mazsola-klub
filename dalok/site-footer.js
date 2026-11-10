/* One maintained footer for every Mazsola Klub content page.
   The pages include equivalent server-rendered markup as a no-JS fallback. */
(() => {
  'use strict';
  const version='20261110-2';
  const markup="<div class=\"mz-footer__inner\">\n  <div class=\"mz-footer__brand\"><strong>© 2026 Mazsola Klub</strong><small>Kalandra hangolva!</small></div>\n  <div class=\"mz-footer__right\">\n    <nav class=\"mz-footer__links\" data-footer-legal aria-label=\"Lábléc navigáció\">\n      <a href=\"/dalok/\">Dalválogatások</a>\n      <a href=\"/ovodaknak/\">Óvodáknak</a>\n      <a href=\"mailto:hello@mazsolaklub.com\" data-contact>Kapcsolat</a>\n      <a href=\"https://ko-fi.com/mazsolaklub\" target=\"_blank\" rel=\"noopener noreferrer\">Támogatás ↗</a>\n      <a href=\"/adatkezeles/\">Adatkezelés</a>\n      <a href=\"/impresszum/\">Impresszum</a>\n      <button type=\"button\" class=\"mz-footer__cookie-settings\" data-cookie-settings>Süti beállítások</button>\n    </nav>\n    <span class=\"social-links social-links--footer\" data-social-links aria-label=\"Mazsola Klub közösségi oldalai\"></span>\n  </div>\n</div>";
  function render(){
    for(const footer of document.querySelectorAll('footer[data-site-footer]')){
      if(footer.dataset.footerVersion===version)continue;
      footer.className='mz-footer';
      footer.setAttribute('aria-label','Mazsola Klub lábléc');
      footer.innerHTML=markup;
      footer.dataset.footerVersion=version;
    }
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',render,{once:true});
  else render();
})();
