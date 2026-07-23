/* AgentLoop e2e 验收: council房 → 贪吃蛇 → 计划卡 → 批准 → 日志流 → 交付卡 → 落盘 */
const WS = require('ws');
const http = require('http');
const fs = require('fs');
const { execSync } = require('child_process');
const ADB = process.env.LOCALAPPDATA + '\\Android\\Sdk\\platform-tools\\adb.exe';
function shot(n) { fs.writeFileSync('shots/' + n + '.png', execSync('"' + ADB + '" exec-out screencap -p', { encoding: 'buffer', maxBuffer: 64 * 1024 * 1024 })); }

let ws, msgId = 0; const pending = new Map();
function connect() {
  return new Promise((resolve, reject) => {
    http.get('http://localhost:9222/json', res => {
      let d = ''; res.on('data', c => d += c);
      res.on('end', () => {
        const page = JSON.parse(d).find(t => t.url.includes('hermes-shell'));
        ws = new WS(page.webSocketDebuggerUrl, { perMessageDeflate: false });
        ws.on('open', resolve); ws.on('error', reject);
        ws.on('message', raw => { const m = JSON.parse(raw); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } });
      });
    }).on('error', reject);
  });
}
function evaljs(expression) {
  return new Promise((resolve, reject) => {
    const id = ++msgId;
    pending.set(id, m => {
      if (m.error) return reject(new Error(m.error.message));
      const r = m.result;
      if (r && r.exceptionDetails) return reject(new Error('页面异常: ' + (r.exceptionDetails.exception && r.exceptionDetails.exception.description || r.exceptionDetails.text).slice(0, 300)));
      resolve(r && r.result ? r.result.value : undefined);
    });
    ws.send(JSON.stringify({ id, method: 'Runtime.evaluate', params: { expression, returnByValue: true, awaitPromise: true } }));
  });
}
const sleep = ms => new Promise(r => setTimeout(r, ms));
function assert(c, m) { if (!c) throw new Error(m || '断言失败'); }

(async () => {
  await connect();
  // 建 council 验收房
  const rid = await evaljs(`(function(){
    closeAllSheets();genCounter++;curRoomId=null;setTab('chat');showView('view-rooms');
    var models=B.listModels();var ids=models.length?[models[0].id]:[];
    var id='agente2e';
    var old=ROOMS.find(function(r){return r.id===id;});
    if(old){enterRoom(id);return id;}
    ROOMS.splice(1,0,{id:id,name:'Agent验收',mode:'council',
      members:{human:[{who:'you',role:'owner'}],ai:ids},
      phase:'讨论中',last:'',time:'现在',unread:0,played:false,msgs:[],seed:[]});
    B.initRoomStorage(id);renderRooms();persistRooms();enterRoom(id);
    return id;
  })()`);
  console.log('房间:', rid);

  // 发任务
  await evaljs(`$('msgInput').value='做一个贪吃蛇网页游戏, 单文件 HTML';$('btnSend').click()`);
  console.log('已发送任务, 等待计划卡...');

  // 等计划卡 (≤90s)
  let planOk = false, brain401 = false;
  const t0 = Date.now();
  while (Date.now() - t0 < 90000) {
    await sleep(3000);
    const st = await evaljs(`(function(){
      var cards=document.querySelectorAll('#chatBody .plan-card');
      var txt=$('chatBody').textContent;
      return {plan:cards.length, fail:txt.indexOf('失败')>=0, err401:txt.indexOf('401')>=0, sub:$('roomSub').textContent};
    })()`);
    if (st.err401) { brain401 = true; console.log('⚠ 大脑 401 (key 失效), 错误卡已渲染:', st.sub); break; }
    if (st.plan > 0) { planOk = true; break; }
    if (st.fail) { console.log('✗ 任务失败:', st.sub); break; }
  }
  if (brain401) { console.log('链路结论: 计划前的错误路径正常渲染 (key 需配置)'); process.exit(2); }
  assert(planOk, '90s 内未出计划卡');

  // 验证计划卡内容 (预估计量)
  const est = await evaljs(`document.querySelector('#chatBody .plan-card .ph').textContent`);
  console.log('计划卡:', est);
  assert(/预计.*tokens/.test(est), '计划卡缺预估计量');
  shot('agent-plan');

  // 批准
  await evaljs(`(function(){var b=document.querySelector('#chatBody .plan-card .btn-acc');b.click();return true;})()`);
  console.log('已批准, 等待执行与交付 (≤300s)...');

  const t1 = Date.now();
  let delivered = false, failed = '';
  while (Date.now() - t1 < 300000) {
    await sleep(4000);
    const st = await evaljs(`(function(){
      var txt=$('chatBody').textContent;
      return {
        steps:document.querySelectorAll('#chatBody .toolcall').length,
        deliver:document.querySelectorAll('#chatBody .deliver-card').length,
        fail:txt.indexOf('任务失败')>=0?txt.slice(txt.indexOf('任务失败'),txt.indexOf('任务失败')+60):'',
        sub:$('roomSub').textContent
      };
    })()`);
    process.stdout.write('\r  步骤卡=' + st.steps + ' 交付=' + st.deliver + ' ' + st.sub.slice(0, 50) + '    ');
    if (st.deliver > 0) { delivered = true; break; }
    if (st.fail) { failed = st.fail; break; }
  }
  console.log('');
  if (failed) { console.log('✗', failed); shot('agent-fail'); process.exit(3); }
  assert(delivered, '300s 内未交付');
  shot('agent-deliver');

  // 验证交付卡 + 文件落盘
  const metric = await evaljs(`(function(){var s=$('chatBody').querySelectorAll('.sysline');var t='';for(var i=0;i<s.length;i++){if(s[i].textContent.indexOf('实际')>=0)t=s[i].textContent;}return t;})()`);
  console.log('计量行:', metric);
  const files = await evaljs(`B.listWorkFiles('${rid}').files.map(function(f){return f.name;})`);
  console.log('落盘文件:', JSON.stringify(files));
  assert((files || []).some(f => /html?/i.test(f)), '无 HTML 产出');
  console.log('\n===== e2e 验收通过 =====');
  process.exit(0);
})().catch(e => { console.error('\n✗', e.message); process.exit(1); });
