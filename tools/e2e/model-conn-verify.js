const WS=require('ws'),http=require('http');
let ws,id=0;const p=new Map();
function ev(e){return new Promise((res,rej)=>{const i=++id;p.set(i,m=>{if(m.result&&m.result.exceptionDetails)rej(new Error(JSON.stringify(m.result.exceptionDetails).slice(0,200)));else res(m.result&&m.result.result?m.result.result.value:undefined);});ws.send(JSON.stringify({id:i,method:'Runtime.evaluate',params:{expression:e,returnByValue:true,awaitPromise:true}}));});}
http.get('http://localhost:9222/json',res=>{let d='';res.on('data',c=>d+=c);res.on('end',()=>{
const page=JSON.parse(d).find(t=>t.url.includes('hermes-shell'));
ws=new WS(page.webSocketDebuggerUrl);ws.on('open',async()=>{
ws.on('message',raw=>{const m=JSON.parse(raw);if(m.id&&p.has(m.id)){p.get(m.id)(m);p.delete(m.id);}});
const models=await ev(`B.listModels()`);
console.log('共 '+models.length+' 个模型\n');
for(const m of models){
  const t0=Date.now();
  const expr='new Promise(function(res){B.aiChatWithModel("只回复两个字: 收到",'+JSON.stringify(m.id)+',function(x){res(x);});})';
  const r=await ev(expr).catch(e=>({ok:false,content:String(e)}));
  const ms=((Date.now()-t0)/1000).toFixed(1);
  console.log((r&&r.ok?'✅ 通  ':'❌ 不通')+'  '+m.name+(m.isDefault?' [默认]':''));
  console.log('   '+ms+'s · '+String(r&&r.content||'').slice(0,70).replace(/\n/g,' '));
}
process.exit(0);});});});
