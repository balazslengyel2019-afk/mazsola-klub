'use strict';
(()=>{
  const header=document.querySelector('.game-site-nav');
  if(!header)return;
  const syncHeight=()=>document.documentElement.style.setProperty('--game-nav-height',header.getBoundingClientRect().height+'px');
  syncHeight();
  new ResizeObserver(syncHeight).observe(header);
})();
