const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const src=fs.readFileSync('app/src/main/assets/ascent_live.js','utf8');
const events=[];
class Socket {
  static OPEN=1;
  constructor(url) {this.url=url;this.listeners={};}
  addEventListener(name,fn){(this.listeners[name] ||= []).push(fn);}
  emit(name,data){for(const fn of this.listeners[name]||[]) fn({data});}
}
const window={WebSocket:Socket,IdleArcAscent:{postMessage:x=>events.push(JSON.parse(x))}};
vm.runInNewContext(src,{window,location:{href:'https://idlearc.com/app/'},URL,Proxy,Reflect,JSON,Number,String,Array});
assert.equal(window.WebSocket.OPEN,1);
const unrelated=new window.WebSocket('wss://elsewhere.example/app/key'); unrelated.emit('open');
assert.equal(events.length,0);
const ws=new window.WebSocket('wss://ws.idlearc.com/app/key?secret=not-logged');
ws.emit('open');
ws.emit('message',JSON.stringify({event:'pusher_internal:subscription_succeeded',channel:'arcstone-ascent',data:{}}));
const envelope={event:'App\\Events\\AscentPoolUpdated',channel:'arcstone-ascent',data:JSON.stringify({pool:38626,tier:22,pool_overflow_pct:0,leaderboard:{total_contributors:1929,top:[{name:'secret-name'}]},leaderboard_lifetime:{total_contributors:18720}})};
ws.emit('message',JSON.stringify(envelope));
assert(events.some(e=>e.kind==='subscribed'));
const update=events.find(e=>e.kind==='ascent');
assert.equal(update.pool,'38626'); assert.equal(update.tier,'22'); assert.equal(update.contributors,'1929'); assert.equal(update.lifetime,'18720');
assert(!JSON.stringify(events).includes('secret'));
const before=events.length;
ws.emit('message',JSON.stringify({...envelope,channel:'private-character'}));
ws.emit('message',JSON.stringify({...envelope,event:'Other'}));
ws.emit('message',JSON.stringify({...envelope,data:JSON.stringify({pool:9007199254740992,tier:22})}));
assert.equal(events.length,before);
ws.emit('close'); assert.equal(events.at(-1).kind,'closed');
console.log('ascent live tests passed');
