/* ============================================================
   council.js — 已废弃。
   多模型真实讨论逻辑在 chat.js runCouncil() → CouncilClient.java。
   fit 房间硬编码剧本已删除 (DESIGN_MULTI_MODEL 第2层)。
   保留 sleep() 供其他模块使用。
   ============================================================ */
function sleep(ms){return new Promise(function(res){setTimeout(res,ms);});}
