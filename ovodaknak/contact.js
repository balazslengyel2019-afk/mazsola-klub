'use strict';
(()=>{
  const dialog=document.querySelector('#idea-dialog');
  const open=document.querySelector('#open-idea-form');
  const close=document.querySelector('#close-idea-form');
  const form=document.querySelector('#idea-form');
  const status=document.querySelector('#idea-status');
  const submit=document.querySelector('#idea-submit');
  if(!dialog||!open||!close||!form)return;

  const openDialog=()=>{status.textContent='';status.className='idea-status';dialog.showModal();};
  const closeDialog=()=>{if(dialog.open)dialog.close();};

  open.addEventListener('click',openDialog);
  close.addEventListener('click',closeDialog);
  dialog.addEventListener('click',e=>{
    if(e.target!==dialog)return;
    const r=dialog.getBoundingClientRect();
    if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)closeDialog();
  });

  form.addEventListener('submit',async e=>{
    e.preventDefault();
    if(!form.reportValidity())return;
    const data=Object.fromEntries(new FormData(form).entries());
    status.className='idea-status';
    status.textContent='Küldés…';
    form.classList.add('is-sending');
    submit.disabled=true;
    try{
      const res=await fetch('/api/contact',{
        method:'POST',
        headers:{'content-type':'application/json'},
        body:JSON.stringify(data)
      });
      const body=await res.json().catch(()=>({}));
      if(!res.ok)throw new Error(body.error||'Az üzenet most nem küldhető el.');
      status.className='idea-status is-success';
      status.textContent='Köszönjük! Az üzenet megérkezett.';
      form.reset();
      setTimeout(closeDialog,1800);
    }catch(err){
      status.className='idea-status is-error';
      status.textContent='A küldéshez még be kell kötnünk az e-mail szolgáltatást. Addig írj a hello@mazsolaklub.com címre.';
    }finally{
      form.classList.remove('is-sending');
      submit.disabled=false;
    }
  });
})();