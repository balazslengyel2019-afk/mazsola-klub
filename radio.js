
(() => {
  const PLAYLIST_ID = 'PLrrZf1EMUhtzDZzXC7HRXhkQGMhtPo1SM';
  const FIRST_VIDEO = 'mtUxeigveCg';
  const STORAGE_KEY = 'mazsola-radio-index-v1';
  let player = null;
  let apiLoading = false;
  let requestedStart = false;

  function el(tag, attrs={}, html=''){
    const node=document.createElement(tag);
    for(const [k,v] of Object.entries(attrs)){
      if(k==='class') node.className=v;
      else if(k==='ariaLabel') node.setAttribute('aria-label',v);
      else node.setAttribute(k,v);
    }
    if(html) node.innerHTML=html;
    return node;
  }

  const launcher=el('button',{class:'mk-radio-launcher',type:'button',ariaLabel:'Mazsola Rádió indítása'},'<b>▶</b><span>Mazsola Rádió</span>');
  const panel=el('section',{class:'mk-radio',hidden:'',role:'region','aria-label':'Mazsola Rádió'});
  panel.innerHTML=
    '<div class="mk-radio__top">'+
      '<div class="mk-radio__brand"><img src="/assets/logo.png" width="38" height="38" alt=""><div><strong>Mazsola Rádió</strong><span>Teljes YouTube lejátszási lista</span></div></div>'+
      '<button class="mk-radio__close" type="button" aria-label="Rádió bezárása">×</button>'+
    '</div>'+
    '<div class="mk-radio__video"><div id="mk-radio-player"></div></div>'+
    '<div class="mk-radio__body">'+
      '<p class="mk-radio__eyebrow">MOST SZÓL</p>'+
      '<p class="mk-radio__title" id="mk-radio-title">A lejátszó betöltése…</p>'+
      '<div class="mk-radio__controls">'+
        '<button class="mk-radio__control" id="mk-radio-prev" type="button" aria-label="Előző dal">⏮</button>'+
        '<button class="mk-radio__control mk-radio__control--play" id="mk-radio-play" type="button" aria-label="Lejátszás vagy szünet">▶ Lejátszás</button>'+
        '<button class="mk-radio__control" id="mk-radio-next" type="button" aria-label="Következő dal">⏭</button>'+
      '</div>'+
      '<div class="mk-radio__meta"><span id="mk-radio-position">Mazsola Klub</span><a href="https://www.youtube.com/watch?v='+FIRST_VIDEO+'&list='+PLAYLIST_ID+'" target="_blank" rel="noopener">YouTube ↗</a></div>'+
    '</div>';

  document.body.append(launcher,panel);

  const title=panel.querySelector('#mk-radio-title');
  const playBtn=panel.querySelector('#mk-radio-play');
  const prevBtn=panel.querySelector('#mk-radio-prev');
  const nextBtn=panel.querySelector('#mk-radio-next');
  const pos=panel.querySelector('#mk-radio-position');
  const closeBtn=panel.querySelector('.mk-radio__close');

  function loadAPI(){
    if(window.YT && window.YT.Player){createPlayer();return;}
    if(apiLoading) return;
    apiLoading=true;
    const old=window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady=()=>{
      if(typeof old==='function') old();
      createPlayer();
    };
    const s=document.createElement('script');
    s.src='https://www.youtube.com/iframe_api';
    s.async=true;
    document.head.appendChild(s);
  }

  function savedIndex(){
    const n=Number(localStorage.getItem(STORAGE_KEY));
    return Number.isFinite(n)&&n>=0?n:0;
  }

  function createPlayer(){
    if(player || !(window.YT&&window.YT.Player)) return;
    player=new YT.Player('mk-radio-player',{
      height:'230',
      width:'100%',
      videoId:FIRST_VIDEO,
      playerVars:{
        listType:'playlist',
        list:PLAYLIST_ID,
        index:savedIndex(),
        autoplay:requestedStart?1:0,
        controls:1,
        playsinline:1,
        rel:0,
        origin:location.origin
      },
      events:{
        onReady:(e)=>{
          try{
            e.target.loadPlaylist({list:PLAYLIST_ID,listType:'playlist',index:savedIndex(),startSeconds:0});
            if(requestedStart) e.target.playVideo();
          }catch(_){}
          update();
        },
        onStateChange:()=>{
          update();
          try{
            const idx=player.getPlaylistIndex();
            if(idx>=0) localStorage.setItem(STORAGE_KEY,String(idx));
          }catch(_){}
        },
        onError:()=>{ title.textContent='A dal most nem indítható. Nyisd meg a lejátszási listát YouTube-on.'; }
      }
    });
  }

  function update(){
    if(!player) return;
    try{
      const data=player.getVideoData();
      if(data && data.title) title.textContent=data.title;
      const idx=player.getPlaylistIndex();
      const list=player.getPlaylist();
      if(idx>=0 && list && list.length) pos.textContent=(idx+1)+'. dal / '+list.length;
      const state=player.getPlayerState();
      playBtn.textContent=state===YT.PlayerState.PLAYING?'⏸ Szünet':'▶ Lejátszás';
    }catch(_){}
  }

  function openRadio(start){
    requestedStart=!!start;
    launcher.hidden=true;
    panel.hidden=false;
    loadAPI();
    if(player && start){
      try{player.playVideo();}catch(_){}
    }
  }

  launcher.addEventListener('click',()=>openRadio(true));
  closeBtn.addEventListener('click',()=>{
    if(player){try{player.pauseVideo();}catch(_){}}
    panel.hidden=true;
    launcher.hidden=false;
  });
  playBtn.addEventListener('click',()=>{
    if(!player){openRadio(true);return;}
    try{
      if(player.getPlayerState()===YT.PlayerState.PLAYING) player.pauseVideo();
      else player.playVideo();
    }catch(_){}
  });
  prevBtn.addEventListener('click',()=>{if(player){try{player.previousVideo();}catch(_){}}});
  nextBtn.addEventListener('click',()=>{if(player){try{player.nextVideo();}catch(_){}}});
})();
