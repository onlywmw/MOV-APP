/* ============================================================
   app-run.js — 运行页刷新 + Cron 创建 + 详情弹层
   (从 app.js 拆出, DESIGN_POLISH #2)
   ============================================================ */

/* 运行页刷新: 切 tab/回前台自动触发 (V5 已移除手动刷新按钮) */

/* Cron 创建 */
$('btnCronCreate').addEventListener('click',function(){
  var text=$('cronInput').value.trim();
  if(!text){B.toast(t('rt.cronInput'));return;}
  var cron='0 8 * * *';
  /* P2: 首个命中生效不再互相覆盖; 优先级 分钟 > 小时 > 定点; 容忍空格 */
  var mMin=text.match(/每\s*(\d+)\s*分钟/);
  var mHour=text.match(/每\s*(\d+)\s*小时/);
  var mTime=text.match(/(\d{1,2}):(\d{2})/);
  if(mMin){cron='*/'+mMin[1]+' * * * *';}
  else if(mHour){cron='0 */'+mHour[1]+' * * *';}
  else if(mTime){cron=mTime[2]+' '+mTime[1]+' * * *';}
  var name=text.length>20?text.slice(0,20)+'…':text;
  var res=B.createCron(name,cron,text);
  if(res.ok){$('cronInput').value='';renderCronJobs();B.toast(t('cron.created'));if(res.notice)B.toast(res.notice);}
  else{B.toast(t('cron.createFail')+(res.error||''));}
  ev('创建 Cron: '+text);
});

/* 三行入口 → 详情弹层 */
$('rowChannels').addEventListener('click',function(){openRunDetail('channels');});
$('rowPerms').addEventListener('click',function(){openRunDetail('perms');});
$('rowSkills').addEventListener('click',function(){openRunDetail('skills');});
$('runDetailClose').addEventListener('click',closeRunDetail);
$('runDetailMask').addEventListener('click',closeRunDetail);

/* 开发者指标折叠 (DESIGN_OPTIMIZE §1) */
$('ssDevToggle').addEventListener('click',function(){
  var m=$('devMetrics');
  var open=m.style.display!=='none';
  m.style.display=open?'none':'';
  this.textContent=open?'▸':'▾';
});

/* 个人信息设置入口 */
$('btnPersonalSettings').addEventListener('click',function(){
  B.openSettings();
  ev('从运行页打开设置');
});

/* V5: 素白 ↔ 墨黑 主题切换 (持久化 mov_theme) */
(function initThemeBtn(){
  var btn=$('btnTheme');
  if(!btn)return;
  btn.textContent=document.documentElement.classList.contains('dark')?'◑':'◐';
  btn.addEventListener('click',function(){
    var dark=document.documentElement.classList.toggle('dark');
    try{localStorage.setItem('mov_theme',dark?'dark':'light');}catch(e){}
    btn.textContent=dark?'◑':'◐';
    ev('切换主题 → '+(dark?'墨黑':'素白'));
  });
})();
