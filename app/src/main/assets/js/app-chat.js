/* ============================================================
   app-chat.js — 消息发送 + 附件系统
   (从 app.js 拆出, DESIGN_POLISH #2)
   ============================================================ */

/* 发送按钮 + 回车 */
$('btnSend').addEventListener('click',sendMsg);
$('msgInput').addEventListener('keydown',function(e){if(e.key==='Enter')sendMsg();});

/* P1-8: 附件按钮 — 真实文件选择器 */
$('plusBtn').addEventListener('click',function(){
  trayOpen=!trayOpen;
  $('attTray').classList.toggle('open',trayOpen);
  $('plusBtn').classList.toggle('open',trayOpen);
  ev(trayOpen?'展开附件托盘':'收起附件托盘');
});
document.querySelectorAll('.tray-item').forEach(function(el){
  el.addEventListener('click',function(){
    closeTray();
    B.pickFile(function(info){
      if(info){pending.push(info);renderPend();ev(t('file.pick')+info.name);}
      else{ev(t('file.cancel'));}
    });
  });
});
