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
      group.innerHTML='<a href="mailto:hello@mazsolaklub.com">'+copy.contact+'</a><a href="/adatkezeles/">'+copy.privacy+'</a><a href="/impresszum/">'+copy.imprint+'</a><button class="cookie-settings-button" type="button" data-cookie-settings>'+copy.settings+'</button>';
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

  document.addEventListener('click',event=>{
    if(event.target.closest('[data-cookie-settings]'))openSettings();
  });

  const boot=()=>{
    updateHungarianChannelState();
    addFooterLinks();
    const choice=storedChoice();
    if(choice==='accepted')loadAnalytics();
    else if(!choice)openSettings();
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);
  else boot();
})();
