/* Diagnostic only: never sends URLs with queries, cookies, headers or message values. */
(function () {
  'use strict';
  if (window.__idleArcStreamProbeInstalled) return;
  window.__idleArcStreamProbeInstalled = true;
  const safeSegments = new Set(['api','app','ws','websocket','socket','socket.io','events','event','stream','sse','broadcasting','channels','chat','dashboard','realtime','live','subscribe','notifications','updates','connect']);
  const safeKeys = new Set(['type','event','data','payload','action','channel','timestamp','state','status','message']);
  function safeLocation(raw) {
    try {
      const u = new URL(String(raw), location.href);
      const host = u.hostname.toLowerCase();
      const origin = host === 'idlearc.com' ? 'idlearc.com' : host === 'www.idlearc.com' ? 'www.idlearc.com' : 'external';
      const parts = u.pathname.split('/').filter(Boolean).slice(0, 8).map(function (p) {
        const part = p.toLowerCase();
        return safeSegments.has(part) ? part : '_';
      });
      return origin + '/' + parts.join('/');
    } catch (_) { return 'unknown/'; }
  }
  function send(kind, stage, raw, shape) {
    try { window.IdleArcStreamProbe.postMessage(JSON.stringify({kind:kind, stage:stage, location:safeLocation(raw), shape:shape || ''})); } catch (_) { }
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
        const seen=new Set();
        instance.addEventListener('message',function(event) {
          const shape=describe(event.data);
          if(!seen.has(shape) && seen.size<8) { seen.add(shape); send(kind,'message',url,shape); }
        });
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
