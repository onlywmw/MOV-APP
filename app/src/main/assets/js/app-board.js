/* ============================================================
   app-board.js — 看板事件绑定
   (从 app.js 拆出, DESIGN_POLISH #2)
   ============================================================ */
$('boardTrigger').addEventListener('click',openBoardPanel);
$('boardPanelMask').addEventListener('click',closeBoardPanel);
$('boardPanelClose').addEventListener('click',closeBoardPanel);
$('btnBoardAddClose').addEventListener('click',closeBoardAddSheet);
$('boardAddMask').addEventListener('click',closeBoardAddSheet);
$('btnBoardAddOk').addEventListener('click',confirmBoardAdd);
