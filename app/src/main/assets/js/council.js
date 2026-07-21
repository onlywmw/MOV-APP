/* ============================================================
   council.js — 仅 fit 房间首次进入的硬编码演示剧本。
   真实议会逻辑在 chat.js runCouncil() → CouncilClient.java。
   本文件不参与任何真实 AI 调用。
   ============================================================ */
function sleep(ms){return new Promise(function(res){setTimeout(res,ms);});}

async function runFitCouncil(id){
  var gen=genCounter;
  var alive=function(){return genCounter===gen&&curRoomId===id;};
  var room=ROOMS.find(function(r){return r.id===id;});
  if(!room.seeded){room.seeded=true;(room.seed||[]).forEach(function(m){push(id,mkMsg(m));});}
  await sleep(600);if(!alive())return;

  var typing=mkMsg({t:'agent',who:'claude',caret:true});
  showTyping(id,typing);await sleep(1100);if(!alive())return;killTyping(typing);
  push(id,mkMsg({t:'agent',who:'claude',role:'产品',h:'看了参考图——密度太高,照抄必死。MVP 只留三件: <code>训练记录</code> · <code>周报</code> · <code>连击日历</code>。'}));
  await sleep(700);if(!alive())return;

  typing=mkMsg({t:'agent',who:'gpt-5',caret:true});showTyping(id,typing);await sleep(1000);if(!alive())return;killTyping(typing);
  push(id,mkMsg({t:'agent',who:'gpt-5',role:'技术',h:'技术栈建议本地优先: 浏览器内置存储, 离线可用, 两周可交付。'}));
  await sleep(700);if(!alive())return;

  typing=mkMsg({t:'agent',who:'gemini',caret:true});showTyping(id,typing);await sleep(1000);if(!alive())return;killTyping(typing);
  push(id,mkMsg({t:'agent',who:'gemini',role:'数据',h:'留存靠习惯回路。连击数放首屏第一视觉, 比功能列表重要。'}));
  await sleep(700);if(!alive())return;

  setPhase(id,'收敛中');
  var v=document.createElement('div');v.className='msg wide';
  var vc=document.createElement('div');vc.className='vote-card';
  var vh=document.createElement('div');vh.className='vh';vh.textContent='COUNCIL VOTE · 收敛投票';
  vc.appendChild(vh);
  [['CL','#52525B','claude','MVP 三项'],['G5','#27272A','gpt-5','本地优先技术栈'],['GM','#71717A','gemini','连击机制前置']].forEach(function(row){
    var vr=document.createElement('div');vr.className='vote-row';
    var av=document.createElement('span');av.className='av';av.style.background=row[1];av.textContent=row[0];
    var nm=document.createElement('span');nm.className='nm';nm.textContent=row[2];
    var op=document.createElement('span');op.style.color='var(--ink-3)';op.textContent=row[3];
    var st=document.createElement('span');st.className='st';
    var okc=document.createElement('span');okc.className='ok';okc.textContent='✓ 赞成';st.appendChild(okc);
    vr.appendChild(av);vr.appendChild(nm);vr.appendChild(op);vr.appendChild(st);
    vc.appendChild(vr);
  });
  var vt=document.createElement('div');vt.className='vote-tally';vt.textContent='3 / 3 一致通过 → 交付 hermes 执行';
  vc.appendChild(vt);v.appendChild(vc);
  push(id,v);
  await sleep(800);if(!alive())return;

  setPhase(id,'待确认');
  var p=document.createElement('div');p.className='msg wide';
  var pc=document.createElement('div');pc.className='plan-card';
  var phd=document.createElement('div');phd.className='ph';phd.textContent='PLAN V1 · 待确认';pc.appendChild(phd);
  var pb=document.createElement('div');pb.className='pb';
  ['MVP 三项: 训练记录 / 周报 / 连击日历','连击数为首屏第一视觉','本地优先技术栈, 离线可用, 两周交付'].forEach(function(t){
    var li=document.createElement('li');li.textContent=t;pb.appendChild(li);
  });
  pc.appendChild(pb);
  var pf=document.createElement('div');pf.className='pf';
  var bAp=document.createElement('button');bAp.className='btn btn-acc';bAp.textContent='批准并执行';
  var bRj=document.createElement('button');bRj.className='btn btn-ghost';bRj.textContent='驳回再议';
  bAp.addEventListener('click',function(){
    bAp.disabled=true;bRj.disabled=true;bAp.textContent='已批准 ✓';
    ev('批准方案 → 移交执行');approveFit(id,gen);
  });
  bRj.addEventListener('click',function(){
    bAp.disabled=true;bRj.disabled=true;bRj.textContent='已驳回, 待重议';
    ev('驳回方案 → 议会重新讨论');setPhase(id,'讨论中');
  });
  pf.appendChild(bAp);pf.appendChild(bRj);pc.appendChild(pf);
  p.appendChild(pc);
  push(id,p);
  ev('议会收敛 3/3 全票 → 等待批准');
}

/* ---------- 批准 → 执行 → 交付 ---------- */
async function approveFit(id,gen){
  var alive=function(){return genCounter===gen&&curRoomId===id;};
  setPhase(id,'执行中');
  push(id,mkMsg({t:'sys',h:'已批准 → hermes 独家执行'}));
  await sleep(500);if(!alive())return;
  var n1=toolNode('bash','npx create-app fitness-app','1.2s','scaffold · vite + 本地存储\n<span class="ok-line">exit 0</span>');
  push(id,n1);
  await sleep(700);if(!alive())return;
  var n2=toolNode('write','src/views/Home.vue · 连击首屏','0.4s','streak counter · 3 MVP cards\n<span class="ok-line">exit 0</span>');
  push(id,n2);
  await sleep(700);if(!alive())return;
  push(id,mkMsg({t:'agent',who:'hermes',h:'构建完成, 正在打包交付到桌面。'}));
  await sleep(600);if(!alive())return;
  var d=document.createElement('div');d.className='msg wide';
  d.innerHTML='<div class="deliver-card"><span class="ic">▣</span><div><div class="tt">fitness-app · v1.0</div><div class="ss">已送达桌面 · 可在启动器打开</div></div><button class="btn btn-acc">打开</button>';
  d.querySelector('.btn').addEventListener('click',function(){B.toast('演示交付物 · fitness-app');ev('打开交付物 fitness-app');});
  push(id,d);
  setPhase(id,'已交付');
  ev('交付完成 → 桌面已生成 fitness-app');
  await sleep(700);if(!alive())return;
  push(id,mkMsg({t:'sys',h:'待评审 · 不满意可直接说, 议会 R2 再议'}));
  setPhase(id,'待评审');
  var room=ROOMS.find(function(r){return r.id===id;});room.last='已交付 fitness-app v1.0 · 待评审';renderRooms();persistRooms();
}
