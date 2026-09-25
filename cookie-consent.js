'use strict';
(()=>{
  const STORAGE_KEY='mazsola_cookie_choice_v1';
  const GA_ID='G-N12YBLCV1N';
  const lang=(document.documentElement.lang||'hu').slice(0,2);
  const copy={
    hu:{title:'Sütik és látogatottságmérés',text:'A szükséges helyi tárhely az oldal és a játékok működését segíti. A Google Analytics csak akkor indul el, ha elfogadod a statisztikai sütiket.',reject:'Csak szükséges',accept:'Elfogadom',more:'Adatkezelési tájékoztató',contact:'Kapcsolat',privacy:'Adatkezelés',imprint:'Impresszum',settings:'Süti beállítások'},
    en:{title:'Cookies and analytics',text:'Necessary local storage supports the website and games. Google Analytics starts only if you accept analytics cookies.',reject:'Necessary only',accept:'Accept',more:'Privacy notice',contact:'Contact',privacy:'Privacy',imprint:'Imprint',settings:'Cookie settings'},
    es:{title:'Cookies y analítica',text:'El almacenamiento local necesario permite que el sitio y los juegos funcionen. Google Analytics solo se inicia si aceptas las cookies estadísticas.',reject:'Solo necesarias',accept:'Aceptar',more:'Privacidad',contact:'Contacto',privacy:'Privacidad',imprint:'Aviso legal',settings:'Configurar cookies'}
  }[lang]||null;
  let analyticsLoaded=false;

  function loadAnalytics(){
    if(analyticsLoaded)return;
    analyticsLoaded=true;
    window.dataLayer=window.dataLayer||[];
    window.gtag=window.gtag||function(){window.dataLayer.push(arguments)};
    window.gtag('js',new Date());
    window.gtag('config',GA_ID,{anonymize_ip:true});
    const script=document.createElement('script');
    script.async=true;
    script.src='https://www.googletagmanager.com/gtag/js?id='+encodeURIComponent(GA_ID);
    document.head.append(script);
  }

  function clearAnalyticsCookies(){
    for(const part of document.cookie.split(';')){
      const name=part.split('=')[0].trim();
      if(!name.startsWith('_ga'))continue;
      for(const domain of ['',location.hostname,'.mazsolaklub.com']){
        document.cookie=name+'=; Max-Age=0; path=/'+(domain?'; domain='+domain:'')+'; SameSite=Lax';
      }
    }
  }

  function storedChoice(){try{return localStorage.getItem(STORAGE_KEY)}catch{return null}}
  function saveChoice(value){try{localStorage.setItem(STORAGE_KEY,value)}catch{}}

  function ensureBanner(){
    let banner=document.querySelector('.cookie-consent');
    if(banner)return banner;
    banner=document.createElement('section');
    banner.className='cookie-consent';
    banner.setAttribute('role','dialog');
    banner.setAttribute('aria-modal','false');
    banner.setAttribute('aria-labelledby','cookie-consent-title');
    banner.innerHTML='<div class="cookie-consent__inner"><div><h2 id="cookie-consent-title">'+copy.title+'</h2><p>'+copy.text+' <a href="/adatkezeles/">'+copy.more+' →</a></p></div><div class="cookie-consent__actions"><button class="cookie-consent__reject" type="button">'+copy.reject+'</button><button class="cookie-consent__accept" type="button">'+copy.accept+'</button></div></div>';
    document.body.append(banner);
    banner.querySelector('.cookie-consent__accept').addEventListener('click',()=>setChoice('accepted'));
    banner.querySelector('.cookie-consent__reject').addEventListener('click',()=>setChoice('necessary'));
    return banner;
  }

  function setChoice(value){
    saveChoice(value);
    const banner=ensureBanner();
    banner.hidden=true;
    if(value==='accepted')loadAnalytics();
    else{
      if(window.gtag)window.gtag('consent','update',{analytics_storage:'denied'});
      clearAnalyticsCookies();
    }
  }

  function openSettings(){
    const banner=ensureBanner();
    banner.hidden=false;
    banner.querySelector('button')?.focus();
  }

  function addFooterLinks(){
    for(const footer of document.querySelectorAll('footer')){
      if(footer.querySelector('[data-footer-legal]'))continue;
      const group=document.createElement('span');
      group.className='footer-legal-links';
      group.dataset.footerLegal='true';
      const hasContact=footer.querySelector('a[href^="mailto:"], #open-idea-form, [data-contact]');
      group.innerHTML=(hasContact?'':'<a href="mailto:hello@mazsolaklub.com">'+copy.contact+'</a>')+'<a href="/adatkezeles/">'+copy.privacy+'</a><a href="/impresszum/">'+copy.imprint+'</a><button class="cookie-settings-button" type="button" data-cookie-settings>'+copy.settings+'</button>';
      const target=footer.querySelector('.footer-links,.footer-row')||footer;
      target.append(group);
    }
  }

  function updateHungarianChannelState(){
    if(location.pathname!=='/' || lang!=='hu')return;
    const languageBar=document.querySelector('.language-bar');
    if(languageBar)languageBar.remove();
    document.querySelectorAll('link[rel="alternate"][hreflang="en"],link[rel="alternate"][hreflang="es"]').forEach(el=>el.remove());
    const replacements=[
      ['Több mint 300 videó','269 videó'],
      ['Több mint 300 magyar gyerekdal','269 magyar nyelvű videó'],
      ['300+','269']
    ];
    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    const nodes=[];
    while(walker.nextNode())nodes.push(walker.currentNode);
    for(const node of nodes){
      let value=node.nodeValue;
      for(const [from,to] of replacements)value=value.split(from).join(to);
      node.nodeValue=value;
    }
    const description='269 magyar nyelvű gyerekdal, mondóka, népdal-feldolgozás, mese és saját játék a Mazsola Klubban. Szülőknek, óvodáknak és bölcsődéknek.';
    const meta=document.querySelector('meta[name="description"]');
    if(meta)meta.content=description;
    const og=document.querySelector('meta[property="og:description"]');
    if(og)og.content=description;
    const twitter=document.querySelector('meta[name="twitter:description"]');
    if(twitter)twitter.content=description;
  }

  function addTestimonials(){
    if(location.pathname!=='/' || lang!=='hu' || document.querySelector('.testimonials'))return;
    const audiences=document.querySelector('.audiences');
    if(!audiences)return;
    const section=document.createElement('section');
    section.className='testimonials wrap';
    section.setAttribute('aria-labelledby','testimonials-title');
    section.innerHTML='<div class="testimonials-head"><p class="eyebrow">VISSZAJELZÉSEK</p><h2 id="testimonials-title">Amit rólunk írtatok</h2><p>Valódi üzenetek családoktól és óvodáktól, akik velünk dalolnak.</p></div><div class="testimonial-grid"><article class="testimonial-card"><span class="testimonial-quote" aria-hidden="true">“</span><blockquote>„Annyira szép ez a dal, a 7 éves kislányommal beleszerettünk.”</blockquote><div class="testimonial-author"><strong>Dóri, anyuka</strong><span>a Fecskehívogató című dalról</span></div></article><article class="testimonial-card testimonial-card--featured"><span class="testimonial-quote" aria-hidden="true">“</span><blockquote>„Nagyon örültek a gyerekek. Hálásak vagyunk, nagyon szépen köszönjük.”</blockquote><div class="testimonial-author"><strong>Váci Evangélikus Egyházi Óvoda</strong><span>egy anyák napi műsor kapcsán</span></div></article><article class="testimonial-card"><span class="testimonial-quote" aria-hidden="true">“</span><blockquote>„Imádjuk a YouTube-csatornátokat. A Forgószél a kisfiam egyik kedvence.”</blockquote><div class="testimonial-author"><strong>Molnár család</strong><span>nézői visszajelzés</span></div></article></div>';
    audiences.insertAdjacentElement('afterend',section);
    const style=document.createElement('style');
    style.textContent='.testimonials{padding-top:4px;padding-bottom:72px}.testimonials-head{max-width:760px;margin-bottom:27px}.testimonials-head .eyebrow{color:#0866a9;margin-bottom:9px}.testimonials-head h2{font-size:clamp(32px,3.4vw,44px);line-height:1.12;letter-spacing:-1.1px;margin:0 0 10px;font-weight:1000}.testimonials-head>p:last-child{margin:0;color:#617486;font-size:15px}.testimonial-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.testimonial-card{position:relative;display:flex;flex-direction:column;min-height:245px;padding:28px 25px 23px;border:1px solid #d6e6f0;border-radius:22px;background:#fff;box-shadow:0 14px 38px #1236560b;overflow:hidden}.testimonial-card--featured{background:linear-gradient(150deg,#eef9ff 0%,#fffdf5 68%,#fff5cc 100%);border-color:#bddbea}.testimonial-quote{position:absolute;right:19px;top:5px;color:#ffcc35;font-size:76px;line-height:1;font-weight:1000;font-family:Georgia,serif;opacity:.75}.testimonial-card blockquote{position:relative;z-index:1;margin:25px 0 28px;color:#123656;font-size:17px;line-height:1.65;font-weight:800}.testimonial-author{position:relative;z-index:1;margin-top:auto;padding-top:18px;border-top:1px solid #e2edf3}.testimonial-author strong{display:block;color:#123656;font-size:14px;font-weight:1000;line-height:1.35}.testimonial-author span{display:block;margin-top:4px;color:#6a7d8c;font-size:12px;line-height:1.4}@media(max-width:850px){.testimonial-grid{grid-template-columns:1fr}.testimonial-card{min-height:0}.testimonials{padding-bottom:56px}}@media(max-width:620px){.testimonials{padding-top:0;padding-bottom:44px}.testimonials-head h2{font-size:32px}.testimonial-card{padding:24px 21px 21px}.testimonial-card blockquote{font-size:16px;margin:21px 0 24px}}';
    document.head.append(style);
  }

  document.addEventListener('click',event=>{
    if(event.target.closest('[data-cookie-settings]'))openSettings();
  });

  const boot=()=>{
    updateHungarianChannelState();
    addTestimonials();
    addFooterLinks();
    const choice=storedChoice();
    if(choice==='accepted')loadAnalytics();
    else if(!choice)openSettings();
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);
  else boot();
})();
