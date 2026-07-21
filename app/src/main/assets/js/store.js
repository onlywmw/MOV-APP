/* ============================================================
   store.js — 数据层
   ROOMS: 本地壳数据, 持久化在 localStorage;
   'desk' 为系统保留 id, 不可删除/覆盖
   ============================================================ */
var STORE_KEY='hermes_rooms_v2';
var DEFAULT_ROOMS=[
  {id:'desk',name:'设备控制 · 单聊',mode:'single',members:['hermes'],phase:'已交付',last:'手电筒 / 电量 / 音量 / 亮度 / 震动 / 截屏… 直接下达指令',time:'现在',unread:0,played:false,
   seed:[{t:'sys',h:'SINGLE · mov-agent 在线 · 30+ 原生能力就绪'},{t:'agent',who:'hermes',h:'我是 MOV。直接下达设备指令即可执行, 例如: <code>打开手电筒</code> · <code>电量多少</code> · <code>最大音量</code> · <code>亮度调高</code> · <code>震动</code> · <code>截屏</code>。非指令类问题会转交 AI 回答(右上角 ≡ 可配置 API)。'}]},
  {id:'fit',name:'健身 APP · 设计提案',mode:'council',members:['claude','gpt-5','gemini'],phase:'讨论中',last:'claude: 看了参考图——密度太高,照抄必死',time:'现在',unread:0,played:false,
   seed:[{t:'sys',h:'COUNCIL 已召开 · claude / gpt-5 / gemini · 主持 hermes'},{t:'agent',who:'YOU',me:true,h:'设计一个健身 APP, 参考这张首页布局, 讨论一下再动手。',att:'img'}]},
  {id:'hainan',name:'HAINAN.WANG · 部署',mode:'single',members:['hermes'],phase:'执行中',last:'EdgeOne 构建巡检通过, 证书链校验中',time:'2h',unread:1,played:false,
   seed:[{t:'agent',who:'hermes',h:'正在腾讯云 EdgeOne 执行部署巡检, 完成后回报。'}]}
];
var ROOMS;
try{
  var saved=JSON.parse(localStorage.getItem(STORE_KEY));
  if(saved&&saved.length){
    ROOMS=saved.map(function(r){r.msgs=[];r.msgData=r.msgData||[];r.seeded=false;r.played=false;return r;});
    if(!ROOMS.some(function(r){return r.id==='desk';}))ROOMS.unshift(DEFAULT_ROOMS[0]);
  }else{ROOMS=DEFAULT_ROOMS;}
}catch(e){ROOMS=DEFAULT_ROOMS;}
function persistRooms(){
  try{
    localStorage.setItem(STORE_KEY,JSON.stringify(ROOMS.map(function(r){
      return {id:r.id,name:r.name,mode:r.mode,members:r.members,phase:r.phase,
              last:r.last,time:r.time,unread:r.unread,seed:r.seed,seeded:r.seeded,played:r.played,
              msgData:r.msgData||[]};
    })));
  }catch(e){}
}
var AV={ 'claude':['CL','#52525B'],'gpt-5':['G5','#27272A'],'gemini':['GM','#71717A'],'hermes':['HE','#D97706'],'YOU':['ME','#09090B'] };
var PHASE_BADGE={'讨论中':'run','收敛中':'run','待确认':'aw','执行中':'run','已交付':'ok','已归档':'off','待评审':'run'};
var ATT={img:{n:'seat-ref.png',m:'1170×2532 · 428KB',ic:'<span class="thumb"></span>'},file:{n:'PRD-v1.md',m:'Markdown · 3.2KB',ic:'<span class="fic">MD</span>'}};

var curTab='chat', curRoomId=null, pending=[], trayOpen=false;
var genCounter=0; /* 房间切换守卫 (HANDOFF §5) */

function $(id){return document.getElementById(id);}
function ev(msg){ if(window.HermesBridge&&HermesBridge.log){try{HermesBridge.log(msg);}catch(e){}} }
function esc(s){return String(s==null?'':s).replace(/[&<>"]/g,function(c){return{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
