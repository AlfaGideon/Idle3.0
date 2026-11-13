const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('app/src/main/assets/stream_probe.js','utf8');
const events=[];
class Socket {
  static OPEN=1;
  constructor(url) { this.url=url; this.listeners={}; }
  send(data) { this.sent = data; }
  addEventListener(type,fn) { (this.listeners[type] ||= []).push(fn); }
  emit(type,data) { for (const fn of this.listeners[type] || []) fn({data}); }
}
class Xhr {
  constructor(){this.readyState=0;this.listeners={};}
  open(method,url){this.url=url;}
  addEventListener(type,fn){this.listeners[type]=fn;}
  send(){}
  getResponseHeader(){return 'text/event-stream';}
}
const originalPromise=Promise.resolve({url:'https://idlearc.com/api/events?token=secret',headers:{get:()=> 'text/event-stream'}});
const window={WebSocket:Socket, EventSource:Socket, XMLHttpRequest:Xhr, fetch:()=>originalPromise,
  IdleArcStreamProbe:{postMessage:msg=>events.push(JSON.parse(msg))}};
const context=vm.createContext({window,location:{href:'https://idlearc.com/app/'}, URL, Proxy, Reflect, Set, WeakMap, JSON, String, Array, Object});
vm.runInContext(source,context);
assert.equal(window.WebSocket.OPEN,1);
const ws=new window.WebSocket('wss://idlearc.com/api/stream/secret-user?token=secret');
assert(ws instanceof Socket);
ws.emit('open');
ws.emit('message', JSON.stringify({event:'secret-event',data:{password:'secret-password'},secret:'secret-value'}));
ws.emit('message', JSON.stringify({event:'other',data:{}}));
assert.equal(events.filter(e=>e.stage==='message').length,1);
assert.equal(events.find(e=>e.stage==='message').shape,'json-object:data,event');
assert.equal(events.find(e=>e.kind==='ws').location,'idlearc.com/api/stream/_');
const pusher=new window.WebSocket('wss://ws-eu.pusher.com/app/secret-app-key?token=secret');
pusher.emit('message', JSON.stringify({event:'pusher:connection_established',data:'{\"socket_id\":\"secret\"}'}));
pusher.send(JSON.stringify({event:'pusher:subscribe',data:{channel:'private-chat-secret-user',auth:'secret-auth'}}));
pusher.emit('message', JSON.stringify({event:'SecretUserName',channel:'private-chat-secret-user',data:'secret-data'}));
assert.equal(events.find(e=>e.meta==='connected||').location,'ws-eu.pusher.com/app/_');
assert(events.some(e=>e.stage==='sent' && e.meta==='subscribe|private|chat'));
assert(events.some(e=>e.stage==='message' && e.meta==='custom|private|chat'));
assert(pusher.sent.includes('secret-auth'));
const xhr=new window.XMLHttpRequest(); xhr.open('GET','https://idlearc.com/api/events?token=secret'); xhr.send(); xhr.readyState=2; xhr.listeners.readystatechange();
assert.equal(window.fetch('/api/events'),originalPromise);
setImmediate(()=>{
  assert(events.some(e=>e.kind==='fetch' && e.stage==='stream'));
  assert(events.some(e=>e.kind==='xhr' && e.stage==='stream'));
  assert(!JSON.stringify(events).includes('secret'));
  console.log('stream probe tests passed');
});
