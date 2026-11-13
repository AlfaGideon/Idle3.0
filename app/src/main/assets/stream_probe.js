/* Diagnostic only: never sends URLs with queries, cookies, headers or message values. */
(function () {
  'use strict';
  if (window.__idleArcStreamProbeInstalled) return;
  window.__idleArcStreamProbeInstalled = true;
  const safeSegments = new Set(['api','app','ws','websocket','socket','socket.io','events','event','stream','sse','broadcasting','channels','chat','dashboard','realtime','live','subscribe','notifications','updates','connect']);
  const safeKeys = new Set(['type','event','data','payload','action','channel','timestamp','state','status','message']);
  const dataKeys = new Set(['character','characterId','character_id','level','experience','exp','gold','goldAmount','ruby','rubies','rubyAmount','arcstone','arcstones','arcstoneAmount','currentHealth','maxHealth','health','damage','attackDamage','monsterLevel','monster','inventory','items','equipment','pets','skills','progress','dashboard','balance','currency','amount','player','user','text','message','sender','channel','type','event','updated','status','data','payload']);
  function safeLocation(raw) {
    try {
      const u = new URL(String(raw), location.href);
      const host = u.hostname.toLowerCase();
      const origin = host === 'idlearc.com' ? 'idlearc.com' : host === 'www.idlearc.com' ? 'www.idlearc.com' : /^ws-[a-z]{2}[0-9]{0,2}\.pusher\.com$/.test(host) ? host : 'external';
      const parts = u.pathname.split('/').filter(Boolean).slice(0, 8).map(function (p) {
        const part = p.toLowerCase();
        return safeSegments.has(part) ? part : '_';
      });
      return origin + '/' + parts.join('/');
    } catch (_) { return 'unknown/'; }
  }
  function send(kind, stage, raw, shape, metadata) {
    try { window.IdleArcStreamProbe.postMessage(JSON.stringify({kind:kind, stage:stage, location:safeLocation(raw), shape:shape || '', meta:metadata || ''})); } catch (_) { }
  }
  // Only predefined protocol names and channel categories leave the page; no event values or channel IDs.
  function metadata(data) {
    if(typeof data !== 'string' || data.length > 65536) return '';
    try {
      const obj=JSON.parse(data); if(!obj || typeof obj!=='object' || Array.isArray(obj)) return '';
      const eventMap={
        'pusher:connection_established':'connected', 'pusher_internal:subscription_succeeded':'subscribed',
        'pusher:subscription_succeeded':'subscribed', 'pusher:subscribe':'subscribe', 'pusher:unsubscribe':'unsubscribe',
        'pusher:ping':'ping', 'pusher:pong':'pong', 'pusher:error':'error',
        'pusher:subscription_error':'subscription-error'
      };
      let event=''; if(typeof obj.event==='string') event=eventMap[obj.event] || 'custom';
      let inner=obj.data;
      if(typeof inner==='string' && inner.length<=65536) { try { inner=JSON.parse(inner); } catch (_) { inner=null; } }
      const channel=typeof obj.channel==='string' ? obj.channel : inner && typeof inner.channel==='string' ? inner.channel : '';
      let scope='', topic='';
      if(channel) {
        scope=channel.startsWith('private-')?'private':channel.startsWith('presence-')?'presence':'public';
        const c=channel.toLowerCase();
        topic=c.includes('chat')?'chat':c.includes('guild')?'guild':c.includes('dashboard')?'dashboard':c.includes('character')?'character':c.includes('combat')?'combat':c.includes('boss')?'boss':'other';
      }
      const fields=inner && typeof inner==='object' && !Array.isArray(inner)
        ? Object.keys(inner).filter(k=>dataKeys.has(k)).sort().slice(0,16).join(',') : '';
      return [event,scope,topic,fields].join('|');
    } catch (_) { return ''; }
  }
  function describe(data) {
    if (typeof data !== 'string') return 'binary';
    if (data.length > 65536) return 'large-text';
    try {
      const o = JSON.parse(data);
      if (o === null) return 'json-null';
      if (Array.isArray(o)) return 'json-array';
      if (typeof o !== 'object') return 'json-scalar';
      const keys = Object.keys(o).filter(k => safeKeys.has(k)).sort().slice(0, 10);
      return keys.length ? 'json-object:' + keys.join(',') : 'json-object';
    } catch (_) { return 'text'; }
  }
  function wrap(name, kind) {
    const Original=window[name]; if (typeof Original !== 'function') return;
    window[name]=new Proxy(Original, {
      construct(target,args,newTarget) {
        const instance=Reflect.construct(target,args,newTarget);
        const url=args[0]; send(kind,'created',url);
        instance.addEventListener('open',()=>send(kind,'open',url));
        instance.addEventListener('error',()=>send(kind,'error',url));
        if(kind==='ws') instance.addEventListener('close',()=>send(kind,'close',url));
        // Repeated shapes must reappear in the timeline so they can be correlated with user actions.
        // Bound diagnostic noise to one entry per signature per second, with at most 40 signatures.
        const seen=new Map();
        function report(stage,data) {
          const shape=describe(data), meta=metadata(data), signature=stage+'|'+shape+'|'+meta, now=Date.now();
          if(!seen.has(signature) && seen.size>=40) return;
          if(seen.has(signature) && now-seen.get(signature)<1000) return;
          seen.set(signature,now); send(kind,stage,url,shape,meta);
        }
        instance.addEventListener('message',function(event) { report('message',event.data); });
        if(kind==='ws') {
          const originalSend=instance.send;
          instance.send=function(data) {
            try { report('sent',data); } catch (_) { }
            return Reflect.apply(originalSend,this,arguments);
          };
        }
        return instance;
      }
    });
  }
  wrap('WebSocket','ws'); wrap('EventSource','sse');
  const fetchOriginal=window.fetch;
  if (typeof fetchOriginal === 'function') window.fetch=function() {
    const args=arguments, result=Reflect.apply(fetchOriginal,this,args);
    result.then(function(response) {
      try {
        const mime=response.headers.get('content-type') || '';
        if(mime.toLowerCase().includes('text/event-stream')) send('fetch','stream', response.url || (args[0] && args[0].url) || args[0]);
      } catch (_) { }
    },function(){});
    return result;
  };
  const XHR=window.XMLHttpRequest;
  if (XHR && XHR.prototype) {
    const originalOpen=XHR.prototype.open, originalSend=XHR.prototype.send;
    const addresses=new WeakMap();
    XHR.prototype.open=function(method,url) { addresses.set(this,url); return Reflect.apply(originalOpen,this,arguments); };
    XHR.prototype.send=function() {
      const xhr=this; let reported=false;
      xhr.addEventListener('readystatechange',function(){
        if(reported || xhr.readyState<2) return;
        try { if((xhr.getResponseHeader('content-type')||'').toLowerCase().includes('text/event-stream')) { reported=true; send('xhr','stream',addresses.get(xhr)); } } catch (_) { }
      });
      return Reflect.apply(originalSend,this,arguments);
    };
  }
  send('probe','ready',location.href);
})();
