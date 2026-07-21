/* ============================================================
   bridge.js — 原生桥 + 全局错误边界
   ============================================================ */

/* P0-1: 异步回调注册表 */
var _cbId=0,_cbMap={};
window._hermesCb=function(id,data){var fn=_cbMap[id];if(fn){delete _cbMap[id];fn(data);}};
function nextCbId(){return 'cb'+(++_cbId)+'_'+Date.now();}

var B=(function(){
  var b=window.HermesBridge||null;
  return {
    present:b||null,
    cmd:function(c){try{return b?b.execCommand(c):'';}catch(e){return '';}},
    parse:function(t){try{return b?JSON.parse(b.parseIntent(t)):{};}catch(e){return {};}},
    ai:function(t){try{return b?b.aiChat(t):'';}catch(e){return '';}},
    /* P0-1: 异步 AI */
    aiAsync:function(t,cb){if(!b){cb({ok:false,content:'浏览器演示模式'});return;}var id=nextCbId();_cbMap[id]=cb;b.aiChatAsync(t,id);},
    /* P1-5: 异步 Council */
    councilAsync:function(topic,cb){if(!b){cb({ok:false,error:'浏览器演示模式'});return;}var id=nextCbId();_cbMap[id]=cb;b.councilAsync(topic,id);},
    aiInfo:function(){try{return b?JSON.parse(b.getAiInfo()):{enabled:false,configured:false};}catch(e){return {enabled:false,configured:false};}},
    device:function(){try{return b?JSON.parse(b.getDeviceInfo()):{};}catch(e){return {};}},
    toast:function(m){try{if(b)b.toast(m);}catch(e){}},
    openSettings:function(){try{if(b)b.openAiSettings();}catch(e){}},
    /* P1-6: Cron */
    listCron:function(){try{return b?JSON.parse(b.listCronJobs()):[];}catch(e){return [];}},
    createCron:function(n,c,cmd){try{return b?JSON.parse(b.createCronJob(n,c,cmd)):{ok:false};}catch(e){return {ok:false};}},
    toggleCron:function(id,en){try{return b?JSON.parse(b.toggleCronJob(id,en)):{ok:false};}catch(e){return {ok:false};}},
    deleteCron:function(id){try{return b?JSON.parse(b.deleteCronJob(id)):{ok:false};}catch(e){return {ok:false};}},
    /* P1-7: 技能 */
    listSkills:function(){try{return b?JSON.parse(b.listSkills()):[];}catch(e){return [];}},
    recordSkill:function(id){try{if(b)b.recordSkillUse(id);}catch(e){}},
    deleteSkill:function(id){try{return b?JSON.parse(b.deleteSkill(id)):{ok:false};}catch(e){return {ok:false};}},
    /* 房间文件操作 */
    writeFile:function(rid,path,content){try{return b?JSON.parse(b.writeFile(rid,path,content)):{ok:false};}catch(e){return {ok:false};}},
    readFile:function(rid,path){try{return b?JSON.parse(b.readFile(rid,path)):{ok:false};}catch(e){return {ok:false};}},
    deleteFile:function(rid,path){try{return b?JSON.parse(b.deleteFile(rid,path)):{ok:false};}catch(e){return {ok:false};}},
    listRoomFiles:function(rid,sub){try{return b?JSON.parse(b.listRoomFiles(rid,sub||'')):{ok:false,files:[]};}catch(e){return {ok:false,files:[]};}},
    initRoom:function(rid,name,desc,members){try{return b?JSON.parse(b.initRoom(rid,name,desc,JSON.stringify(members))):{ok:false};}catch(e){return {ok:false};}},
    /* P1-8: 文件选择 */
    pickFile:function(cb){if(!b){cb(null);return;}var id=nextCbId();_cbMap[id]=cb;b.pickFile(id);},
    /* RUNTIME 真数据 */
    runtimeStats:function(){try{return b?JSON.parse(b.getRuntimeStats()):{};}catch(e){return {};}},
    permState:function(){try{return b?JSON.parse(b.getPermissionState()):{};}catch(e){return {};}},
    widgetInfo:function(){try{return b?JSON.parse(b.getWidgetInfo()):{count:0};}catch(e){return {count:0};}},
    openAppSettings:function(){try{if(b)b.openAppSettings();}catch(e){}}
  };
})();

/* ============ 全局错误边界 ============ */
window.onerror=function(msg,url,line,col,err){
  var info='SHELL ERROR: '+msg+' @ line '+line;
  try{if(window.HermesBridge)HermesBridge.log(info);}catch(e){}
  try{
    var b=document.getElementById('chatBody');
    if(b){
      b.innerHTML+='<div class="sysline" style="color:var(--err);border-color:var(--err-dot);">'+esc(String(msg)).slice(0,200)+' (行 '+line+')</div>';
      b.scrollTop=b.scrollHeight;
    }
  }catch(e2){}
  return true;
};
