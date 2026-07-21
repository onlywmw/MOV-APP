/* ============================================================
   app-run.js — 运行页刷新 + Cron 创建 + 详情弹层
   (从 app.js 拆出, DESIGN_POLISH #2)
   ============================================================ */

/* 运行页刷新按钮 */
$('btnRunRefresh').addEventListener('click',refreshRuntime);

/* Cron 创建 */
$('btnCronCreate').addEventListener('click',function(){
  var text=$('cronInput').value.trim();
  if(!text){B.toast(t('rt.cronInput'));return;}
  var cron='0 8 * * *';
  var m=text.match(/(\d{1,2}):(\d{2})/);
  if(m){cron=m[2]+' '+m[1]+' * * *';}
  var m2=text.match(/每(\d+)小时/);
  if(m2){cron='0 */'+m2[1]+' * * *';}
  var m3=text.match(/每(\d+)分钟/);
  if(m3){cron='*/'+m3[1]+' * * * *';}
  var name=text.length>20?text.slice(0,20)+'…':text;
  var res=B.createCron(name,cron,text);
  if(res.ok){$('cronInput').value='';renderCronJobs();B.toast(t('cron.created'));}
  else{B.toast(t('cron.createFail')+(res.error||''));}
  ev('创建 Cron: '+text);
});

/* 三行入口 → 详情弹层 */
$('rowChannels').addEventListener('click',function(){openRunDetail('channels');});
$('rowPerms').addEventListener('click',function(){openRunDetail('perms');});
$('rowSkills').addEventListener('click',function(){openRunDetail('skills');});
$('runDetailClose').addEventListener('click',closeRunDetail);
$('runDetailMask').addEventListener('click',closeRunDetail);

/* 开发者指标折叠 (DESIGN_POLISH #4) */
$('devToggle').addEventListener('click',function(){
  var m=$('devMetrics');
  var open=m.style.display!=='none';
  m.style.display=open?'none':'';
  this.textContent=(open?'▸':'▾')+' 开发者信息';
});
