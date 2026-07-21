/* ============================================================
   app.js — 入口: 全局事件 + 初始化
   子模块: app-chat / app-room / app-files / app-board / app-run
   (DESIGN_POLISH #2: 从 392 行拆为 ~20 行入口)
   ============================================================ */

/* 返回按钮 */
document.getElementById('btnBack').addEventListener('click',function(){
  genCounter++;curRoomId=null;
  try{if(window.HermesBridge)HermesBridge.setRoomOpen('');}catch(e){}
  setTab('chat');showView('view-rooms');renderRooms();ev('返回房间列表');
});

/* ============ 初始化 ============ */
initLang();
applyI18n();
refreshModelAvatars();
renderRooms();
setTab('chat');
setTimeout(function(){refreshRuntime();},600);
ev('MOV v3.2 '+t('ready')+(B.present?' · '+t('bridge.on'):' · '+t('bridge.off')));
