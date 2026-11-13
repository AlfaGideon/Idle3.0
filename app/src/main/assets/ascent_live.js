/* Observe the official page's own WebSocket; do not initiate requests or pass credentials to Android. */
(function () {
  'use strict';
  if (window.__idleArcAscentInstalled || typeof window.WebSocket !== 'function') return;
  window.__idleArcAscentInstalled = true;
  const original=window.WebSocket;
  let current=null;
  function publish(obj) { try { window.IdleArcAscent.postMessage(JSON.stringify(obj)); } catch (_) {} }
  function number(value) {
    if (typeof value==='number') {
      if(!Number.isFinite(value) || value<0 || (Number.isInteger(value) && !Number.isSafeInteger(value))) return null;
      value=String(value);
    }
    if(typeof value !== 'string' || value.length>64 || !/^\d+(?:\.\d+)?$/.test(value)) return null;
    return value;
  }
  window.WebSocket=new Proxy(original, {
    construct(target,args,newTarget) {
      const socket=Reflect.construct(target,args,newTarget);
      let valid=false;
      try { const url=new URL(String(args[0]),location.href); valid=url.protocol==='wss:' && url.hostname.toLowerCase()==='ws.idlearc.com'; } catch (_) {}
      if(!valid) return socket;
      current=socket; publish({kind:'connecting'});
      socket.addEventListener('open',()=>{if(current===socket) publish({kind:'open'});});
      socket.addEventListener('close',()=>{if(current===socket) publish({kind:'closed'});});
      socket.addEventListener('error',()=>{if(current===socket) publish({kind:'error'});});
      socket.addEventListener('message',function(ev) {
        if(current!==socket || typeof ev.data!=='string' || ev.data.length>1000000) return;
        try {
          const envelope=JSON.parse(ev.data);
          if(!envelope || envelope.channel!=='arcstone-ascent') return;
          if(envelope.event==='pusher_internal:subscription_succeeded' || envelope.event==='pusher:subscription_succeeded') { publish({kind:'subscribed'}); return; }
          if(typeof envelope.event!=='string' || envelope.event.split('\\').filter(Boolean).join('\\')!=='App\\Events\\AscentPoolUpdated') return;
          let d=envelope.data;
          if(typeof d==='string') d=JSON.parse(d);
          if(!d || typeof d!=='object' || Array.isArray(d)) return;
          const pool=number(d.pool), tier=number(d.tier);
          if(pool===null || tier===null || tier.includes('.')) return;
          publish({kind:'ascent',pool:pool,tier:tier,overflow:number(d.pool_overflow_pct),
            contributors:number(d.leaderboard && d.leaderboard.total_contributors),
            lifetime:number(d.leaderboard_lifetime && d.leaderboard_lifetime.total_contributors)});
        } catch (_) { }
      });
      return socket;
    }
  });
})();
