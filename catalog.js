'use strict';
const catalog=document.querySelector('#song-catalog'),search=document.querySelector('#song-search'),count=document.querySelector('#song-count'),more=document.querySelector('#load-more'),dialog=document.querySelector('#player-dialog'),mount=document.querySelector('#player-mount');
let songs=[],filtered=[],limit=12;
const normalize=s=>s.toLocaleLowerCase('hu').normalize('NFD').replace(/[\u0300-\u036f]/g,'');
function play(song){document.querySelector('#player-title').textContent=song.title;document.querySelector('#youtube-fallback').href='https://www.youtube.com/watch?v='+song.id;const frame=document.createElement('iframe');frame.src='https://www.youtube-nocookie.com/embed/'+song.id;frame.title=song.title;frame.allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';frame.allowFullscreen=true;frame.referrerPolicy='strict-origin-when-cross-origin';mount.replaceChildren(frame);dialog.showModal();}
function render(){catalog.replaceChildren();for(const song of filtered.slice(0,limit)){const card=document.createElement('button');card.type='button';card.className='song-card';card.setAttribute('aria-label','Lejátszás: '+song.title);const thumb=document.createElement('div');thumb.className='thumb';const img=document.createElement('img');img.src=song.thumbnail;img.alt='';img.width=720;img.height=405;img.loading='lazy';img.addEventListener('error',()=>{img.src='assets/logo.jpg'},{once:true});const badge=document.createElement('span');badge.className='small-play';badge.textContent='▶';badge.setAttribute('aria-hidden','true');thumb.append(img,badge);const info=document.createElement('div');info.className='song-info';const duration=document.createElement('span');duration.className='tag blue';duration.textContent=song.duration||'GYEREKDAL';const title=document.createElement('h3');title.textContent=song.title;info.append(duration,title);card.append(thumb,info);card.addEventListener('click',()=>play(song));catalog.append(card)}count.textContent=search.value.trim()?filtered.length+' találat · '+songs.length+' videóból':songs.length+' videó a csatornáról';more.hidden=filtered.length<=limit;document.querySelector('#catalog-empty').hidden=filtered.length!==0;}
search.addEventListener('input',()=>{const q=normalize(search.value.trim());filtered=songs.filter(s=>normalize(s.title).includes(q));limit=12;render()});more.addEventListener('click',()=>{limit+=12;render()});document.querySelector('#close-player').addEventListener('click',()=>dialog.close());dialog.addEventListener('close',()=>mount.replaceChildren());dialog.addEventListener('click',e=>{if(e.target===dialog){const r=dialog.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)dialog.close()}});
fetch('songs.json').then(r=>{if(!r.ok)throw Error('catalog');return r.json()}).then(data=>{songs=data;filtered=data;render()}).catch(()=>{count.textContent='';document.querySelector('#catalog-error').hidden=false;search.disabled=true});

(function insertVideoBanner(){
  if(!document.querySelector('link[href="video-banner.css"]')){
    const css=document.createElement('link');css.rel='stylesheet';css.href='video-banner.css';document.head.append(css);
  }
  const about=document.querySelector('#rolunk');
  if(!about||document.querySelector('.video-banner'))return;
  const section=document.createElement('section');
  section.className='video-banner';
  section.setAttribute('aria-label','Mazsola Klub gyerekdalok');
  section.innerHTML='<video autoplay muted loop playsinline preload="metadata" poster="assets/cover.png" aria-hidden="true"><source src="assets/mazsola-bg.mp4" type="video/mp4"></video><div class="video-banner__content"><p class="video-banner__eyebrow">MAZSOLA KLUB</p><h2>Több mint <strong>300</strong> magyar gyerekdal egy helyen.</h2><p class="video-banner__lead">Járművek, állatok, népdalok, évszakok és zenés kalandok a Mazsola Klubban.</p></div>';
  about.before(section);
  const video=section.querySelector('video');
  if(window.matchMedia('(prefers-reduced-motion: reduce)').matches){video.removeAttribute('autoplay');video.pause();}
})();
