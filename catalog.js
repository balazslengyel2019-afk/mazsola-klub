'use strict';
const catalog=document.querySelector('#song-catalog'),search=document.querySelector('#song-search'),count=document.querySelector('#song-count'),more=document.querySelector('#load-more'),dialog=document.querySelector('#player-dialog'),mount=document.querySelector('#player-mount'),popularGrid=document.querySelector('#popular-grid');
const chips=[...document.querySelectorAll('.filter-chip')];
let songs=[],filtered=[],limit=12,activeFilter='all';
const normalize=s=>s.toLocaleLowerCase('hu').normalize('NFD').replace(/[\u0300-\u036f]/g,'');
const cleanTitle=s=>s.replace(/[\u2600-\u27BF\u{1F000}-\u{1FAFF}\uFE0F]/gu,'').split('|')[0].replace(/\s+/g,' ').trim();
const categories={
  jarmuvek:['jarmu','vonat','mozdony','busz','auto','traktor','tuzolto','rendor','markolo','kotro','daru','uthenger','teherauto','terepjaro','mento','repulo','hajo','tengeralattjaro','motor','kukas','utcasepro','munkagep','kamion','taxi','villamos','metro','bicikli','kerekpar'],
  allatok:['allat','kutya','kutyus','cica','macska','nyuszi','nyul','barany','kecske','tehen','malac','tyuk','kacsa','liba','beka','suni','mokus','medve','macko','roka','bagoly','madar','csiga','pillango','szunyog','tucsok','dino','dinoszaurusz','allatkert','pingvin','oroszlan','elefant','zebra','majom','halacska'],
  nepdalok:['nepdal','nepi','folklor','hopp juliska','nagyabony','baranykam','nyulacska','boszorka','felpenzzel','tucsok','rozsa, rozsa','sej,'],
  evszakok:['osz','tavasz','nyar','tel ','szeptember','oktober','november','december','januar','februar','marcius','aprilis','majus','junius','julius','augusztus','hoember','havazas','balaton','szuret','falevel','csillaghullas'],
  unnepek:['karacsony','mikulas','telapo','husvet','anyak nap','halloween','farsang','szulinap','szuletesnap','advent','szilveszter','uj ev','valentin'],
  tanulos:['tanul','szamol','abc','abece','szin','forma','nyelvtoro','beszedfejleszto','ismeretterjeszto','biztonsag','ora','napok','honap','fogmos','takaritos','pakolas','kresz','kozlekedes','bolygo','meteor','csillag','termeszet','ovoda','ovis']
};
const popularPicks=[
  {id:'YUM_wsp4-Qo',title:'Terepjáró, Terepjáró!',note:'Járműves kaland'},
  {id:'c4FL3mhotPA',title:'Óvodába megyek',note:'Ovis kedvenc'},
  {id:'Tk8-ElfvaoY',title:'Ősz az ajtón bekopogott',note:'Évszakos dal'},
  {id:'XUWX3kCLybY',title:'Nyelvtörő Dal',note:'Játékos beszéd'},
  {id:'M5NH4aO1K2c',title:'Rózsa, rózsa, bazsarózsa',note:'Magyar népdal'},
  {id:'HoQ5kOMbuq4',title:'Kis Mackó Kalandja',note:'Állatos kaland'}
];
function play(song){const label=cleanTitle(song.title);document.querySelector('#player-title').textContent=label;document.querySelector('#youtube-fallback').href='https://www.youtube.com/watch?v='+song.id;const frame=document.createElement('iframe');frame.src='https://www.youtube-nocookie.com/embed/'+song.id;frame.title=label;frame.allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';frame.allowFullscreen=true;frame.referrerPolicy='strict-origin-when-cross-origin';mount.replaceChildren(frame);dialog.showModal();}
function matchesCategory(song,key){if(key==='all')return true;const t=normalize(song.title);return (categories[key]||[]).some(word=>t.includes(word));}
function makeThumb(song){const thumb=document.createElement('div');thumb.className='thumb';const img=document.createElement('img');img.src=song.thumbnail;img.alt='';img.width=720;img.height=405;img.loading='lazy';img.addEventListener('error',()=>{img.src='assets/logo.jpg'},{once:true});const badge=document.createElement('span');badge.className='small-play';badge.textContent='▶';badge.setAttribute('aria-hidden','true');thumb.append(img,badge);return thumb;}
function renderPopular(){if(!popularGrid)return;popularGrid.replaceChildren();for(const pick of popularPicks){const song=songs.find(s=>s.id===pick.id);if(!song)continue;const card=document.createElement('button');card.type='button';card.className='popular-card';card.setAttribute('aria-label','Lejátszás: '+pick.title);const info=document.createElement('div');info.className='popular-info';const note=document.createElement('span');note.textContent=pick.note;const title=document.createElement('h4');title.textContent=pick.title;info.append(note,title);card.append(makeThumb(song),info);card.addEventListener('click',()=>play(song));popularGrid.append(card);}}
function render(){catalog.replaceChildren();for(const song of filtered.slice(0,limit)){const card=document.createElement('button');card.type='button';card.className='song-card';card.setAttribute('aria-label','Lejátszás: '+cleanTitle(song.title));const info=document.createElement('div');info.className='song-info';const duration=document.createElement('span');duration.className='tag blue';duration.textContent=song.duration||'GYEREKDAL';const title=document.createElement('h3');title.textContent=cleanTitle(song.title);info.append(duration,title);card.append(makeThumb(song),info);card.addEventListener('click',()=>play(song));catalog.append(card)}const narrowed=search.value.trim()||activeFilter!=='all';count.textContent=narrowed?filtered.length+' találat · '+songs.length+' videóból':songs.length+' videó a csatornáról';more.hidden=filtered.length<=limit;document.querySelector('#catalog-empty').hidden=filtered.length!==0;}
function applyFilters(reset=true){const q=normalize(search.value.trim());filtered=songs.filter(song=>(!q||normalize(song.title).includes(q))&&matchesCategory(song,activeFilter));if(reset)limit=12;render();}
function updateChipState(){for(const chip of chips)chip.setAttribute('aria-pressed',String(chip.dataset.filter===activeFilter));}
function setFilter(key,scroll=false){activeFilter=key;updateChipState();applyFilters(true);if(scroll)document.querySelector('.filter-wrap')?.scrollIntoView({behavior:'smooth',block:'start'});}
search.addEventListener('input',()=>applyFilters(true));
for(const chip of chips)chip.addEventListener('click',()=>setFilter(chip.dataset.filter,false));
for(const button of document.querySelectorAll('[data-quick-filter]'))button.addEventListener('click',()=>{search.value='';setFilter(button.dataset.quickFilter,true)});
for(const button of document.querySelectorAll('[data-quick-query]'))button.addEventListener('click',()=>{activeFilter='all';updateChipState();search.value=button.dataset.quickQuery;applyFilters(true);document.querySelector('.filter-wrap')?.scrollIntoView({behavior:'smooth',block:'start'});});
const initialParams=new URLSearchParams(location.search);
const initialTheme=initialParams.get('tema');
const initialQuery=initialParams.get('kereses');
if(initialTheme && (initialTheme==='all' || categories[initialTheme]))activeFilter=initialTheme;
if(initialQuery)search.value=initialQuery;
updateChipState();
more.addEventListener('click',()=>{limit+=12;render()});document.querySelector('#close-player').addEventListener('click',()=>dialog.close());dialog.addEventListener('close',()=>mount.replaceChildren());dialog.addEventListener('click',e=>{if(e.target===dialog){const r=dialog.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)dialog.close()}});
fetch('songs.json').then(r=>{if(!r.ok)throw Error('catalog');return r.json()}).then(data=>{songs=data;renderPopular();applyFilters(false)}).catch(()=>{count.textContent='';document.querySelector('#catalog-error').hidden=false;search.disabled=true;for(const chip of chips)chip.disabled=true});

(function insertVideoBanner(){
  if(!document.querySelector('link[href*="video-banner.css"]')){
    const css=document.createElement('link');css.rel='stylesheet';css.href='video-banner.css?v=20260911-2';document.head.append(css);
  }
  const about=document.querySelector('#rolunk');
  if(!about||document.querySelector('.video-banner'))return;
  const section=document.createElement('section');
  section.className='video-banner';
  section.setAttribute('aria-label','Mazsola Klub gyerekdalok');
  section.innerHTML='<video muted loop playsinline preload="none" poster="assets/cover.png" aria-hidden="true"><source src="assets/mazsola-bg.mp4" type="video/mp4"></video><div class="video-banner__content"><p class="video-banner__eyebrow">MAZSOLA KLUB</p><h2>Több mint <strong>300</strong> magyar gyerekdal egy helyen.</h2><p class="video-banner__lead">Járművek, állatok, népdalok, évszakok és zenés kalandok a Mazsola Klubban.</p></div>';
  about.before(section);
  const video=section.querySelector('video');
  const reduced=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if(reduced){video.pause();return;}
  const start=()=>video.play().catch(()=>{});
  if('IntersectionObserver' in window){
    const observer=new IntersectionObserver(entries=>{if(entries.some(e=>e.isIntersecting)){start();observer.disconnect();}},{rootMargin:'300px 0px'});
    observer.observe(section);
  }else start();
})();

(function turnHeroLogoIntoBackground(){
  const hero=document.querySelector('.hero');
  const welcome=document.querySelector('.welcome-video');
  const video=welcome?.querySelector('video');
  if(!hero||!welcome||!video)return;

  if(!document.querySelector('#hero-video-background-styles')){
    const style=document.createElement('style');
    style.id='hero-video-background-styles';
    style.textContent=`
      .hero.hero-video-bg{position:relative;display:flex;align-items:center;min-height:430px;overflow:hidden;padding-top:58px;padding-bottom:58px;border-radius:28px;isolation:isolate}
      .hero-video-layer{position:absolute;z-index:-2;right:-55px;top:50%;width:min(58vw,720px);height:min(58vw,720px);transform:translateY(-50%);overflow:hidden;pointer-events:none;border-radius:50%}
      .hero-video-layer video{display:block;width:100%;height:100%;object-fit:contain;object-position:center;transform:none;opacity:.64;filter:saturate(.96) contrast(1)}
      .hero.hero-video-bg::after{content:'';position:absolute;z-index:-1;inset:0;pointer-events:none;background:linear-gradient(90deg,#fffefa 0%,#fffefa 40%,rgba(255,254,250,.94) 52%,rgba(255,254,250,.50) 72%,rgba(255,254,250,.08) 100%)}
      .hero.hero-video-bg .hero-copy{position:relative;z-index:1;max-width:610px}
      @media(max-width:850px){.hero.hero-video-bg{min-height:390px;padding-top:48px;padding-bottom:48px}.hero-video-layer{right:-70px;width:540px;height:540px}.hero-video-layer video{opacity:.48}.hero.hero-video-bg::after{background:linear-gradient(90deg,#fffefa 0%,#fffefa 48%,rgba(255,254,250,.92) 61%,rgba(255,254,250,.42) 100%)}}
      @media(max-width:620px){.hero.hero-video-bg{min-height:410px;padding-top:42px;padding-bottom:42px;border-radius:22px}.hero-video-layer{right:-145px;width:430px;height:430px}.hero-video-layer video{opacity:.27}.hero.hero-video-bg::after{background:linear-gradient(90deg,rgba(255,254,250,.98) 0%,rgba(255,254,250,.94) 65%,rgba(255,254,250,.62) 100%)}.hero.hero-video-bg .hero-copy{max-width:96%}}
      @media(prefers-reduced-motion:reduce){.hero-video-layer video{display:none}.hero.hero-video-bg::after{background:#fffefa}}
    `;
    document.head.append(style);
  }

  hero.classList.add('hero-video-bg');
  const layer=document.createElement('div');
  layer.className='hero-video-layer';
  layer.setAttribute('aria-hidden','true');

  video.removeAttribute('controls');
  video.removeAttribute('aria-label');
  video.setAttribute('aria-hidden','true');
  video.autoplay=true;
  video.muted=true;
  video.loop=true;
  video.playsInline=true;
  video.preload='metadata';
  video.tabIndex=-1;
  layer.append(video);
  hero.prepend(layer);
  welcome.remove();

  if(window.matchMedia('(prefers-reduced-motion: reduce)').matches){
    video.removeAttribute('autoplay');
    video.pause();
  }else{
    video.play().catch(()=>{});
  }
})();


(function loadRabbitShowcase(){
  const section=document.querySelector('.rabbit-showcase');
  const video=section?.querySelector('video[data-src]');
  if(!section||!video)return;
  if(window.matchMedia('(prefers-reduced-motion: reduce)').matches)return;

  let loaded=false;
  const load=()=>{
    if(loaded)return;
    loaded=true;
    video.src=video.dataset.src;
    video.removeAttribute('data-src');
    video.addEventListener('canplay',()=>video.play().catch(()=>{}),{once:true});
    video.load();
  };

  if('IntersectionObserver' in window){
    const observer=new IntersectionObserver(entries=>{
      if(entries.some(e=>e.isIntersecting)){
        observer.disconnect();
        load();
      }
    },{rootMargin:'500px 0px'});
    observer.observe(section);
  }else load();
})();
