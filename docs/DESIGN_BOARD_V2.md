# DESIGN: 看板 v2.0 — 悬浮切换

版本: v2.0
日期: 2026-07-21
状态: 📐 design-ready
取代: DESIGN_BOARD_V1.md

---

## 交互模型

```
┌─ 看板 (全屏) ──────────────────────────┐
│                                         │
│                                         │
│       当前应用内容 (全屏 iframe)          │
│       音乐 / 视频 / 阅读 / 健身          │
│                                         │
│                                         │
│  ┌──────────────────────────┐           │
│  │      底部触发条 (悬浮)     │           │
│  │   ··· 🎵 音乐 ···        │  ← 半透明  │
│  └──────────────────────────┘  3秒不动自动隐藏
│                                         │
└─────────────────────────────────────────┘
```

### 核心交互

```
用户看到全屏内容 (比如音乐正在放)
  → 手指触碰底部区域
    → 底部触发条浮现 (如果已隐藏)
    → 点击触发条
      → 应用选择面板从底部滑入，覆盖屏幕下半部分
      → 网格展示所有应用
      → 点击一个应用
        → 面板滑下消失
        → 内容区切换到新应用
      → 点击面板外部 / ✕
        → 面板滑下消失
        → 内容保持不变
```

---

## 底部触发条

```
┌─────────────────────────────────────────┐
│                    ┌──────────────────┐ │
│                    │  ··· 🎵 音乐 ···  │ │  ← 半透明黑底, 白色文字
│                    └──────────────────┘ │
│                    ↑                    │
│                    40px 高, 居中        │
│                    圆角胶囊形状          │
│                    opacity: 0.7         │
│                    3秒无操作 → 滑出屏幕  │
└─────────────────────────────────────────┘
```

- **常驻模式**: 应用刚切换完的 3 秒内，触发条半透明显示
- **隐藏模式**: 3 秒无操作 → 向下滑出屏幕 (`transform: translateY(120%)`)
- **唤醒**: 手指触碰屏幕底部 80px 区域 → 触发条滑回来
- **内容**: 显示当前应用图标 + 名称。"···"是可拖动的指示点（不做拖动功能，纯视觉提示）

---

## 应用选择面板

```
┌─ 面板 (从底部滑入, 覆盖屏幕下 55%) ─────────┐
│                                             │
│  ┌ 应用 ──────────────────────────── [✕] ┐  │  ← 标题栏
│  ├────────────────────────────────────────┤  │
│  │                                        │  │
│  │  🎵        📖        🏃        📝      │  │  ← 3 列网格
│  │ 音乐      阅读      健身      笔记      │  │
│  │                                        │  │
│  │  🌐        ＋                          │  │
│  │ 我的博客   添加应用                     │  │
│  │                                        │  │
│  └────────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

- **滑入**: 从屏幕底部滑入，覆盖屏幕下半部分（约 55%）
- **关闭**: 点 ✕ / 点面板上方遮罩区 / 点面板外任意位置
- **选择**: 点击应用 → 面板关闭 → 内容区切换
- **网格**: 3 列，每格 = 图标(36px) + 名称(10px)
- **高亮**: 当前选中的应用有金色边框

---

## 状态流转

```
[全屏内容] ←→ [底部触发条浮现] ←→ [应用选择面板]
   ↑                                    │
   └──── 选中应用, 面板关闭 ←───────────┘
```

---

## 内容区规格

- 全屏 iframe，无内边距
- `position: absolute; inset: 0;`
- 应用内容自己管理边距和安全区（`env(safe-area-inset-bottom)`）

---

## CSS 骨架

```css
/* 底部触发条 */
.board-trigger {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  height: 40px;
  padding: 0 16px;
  background: rgba(24,24,27,0.78);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: #fff;
  cursor: pointer;
  z-index: 20;
  transition: transform 0.35s cubic-bezier(.22,1,.36,1),
              opacity 0.35s ease;
}
.board-trigger.hidden {
  transform: translateX(-50%) translateY(140%);
  opacity: 0;
}

/* 应用选择面板 */
.board-panel-mask {
  position: absolute;
  inset: 0;
  background: rgba(9,9,11,0.35);
  z-index: 29;
  opacity: 0; visibility: hidden;
  transition: opacity 0.25s ease, visibility 0s 0.25s;
}
.board-panel-mask.open { opacity: 1; visibility: visible; transition: opacity 0.25s ease; }

.board-panel {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  height: 55%;
  background: var(--panel);
  border-radius: 18px 18px 0 0;
  z-index: 30;
  transform: translateY(100%);
  transition: transform 0.33s cubic-bezier(.22,1,.36,1);
  display: flex;
  flex-direction: column;
}
.board-panel.open { transform: none; }

/* 面板网格 */
.board-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  padding: 16px;
  overflow-y: auto;
}
.board-app-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  border-radius: var(--r-lg);
  cursor: pointer;
  transition: all var(--t-fast);
  border: 1px solid transparent;
}
.board-app-card:hover { background: var(--panel-2); border-color: var(--line); }
.board-app-card.active { border-color: var(--acc-live); background: var(--acc-tint-2); }
.board-app-card .ba-icon { font-size: 28px; }
.board-app-card .ba-name { font-family: var(--font-mono); font-size: 10px; color: var(--ink-2); text-align: center; }
```

---

## HTML 结构

```html
<!-- 看板 tab -->
<div class="view" id="view-board">
  <!-- 应用内容全屏 iframe -->
  <iframe id="boardFrame" src=""
    style="position:absolute;inset:0;width:100%;height:100%;border:none;background:#fff;">
  </iframe>

  <!-- 底部触发条 -->
  <div class="board-trigger" id="boardTrigger">
    <span id="boardTriggerIcon">🎵</span>
    <span id="boardTriggerName">音乐</span>
  </div>

  <!-- 面板遮罩 -->
  <div class="board-panel-mask" id="boardPanelMask"></div>

  <!-- 应用选择面板 -->
  <div class="board-panel" id="boardPanel">
    <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 16px 6px">
      <h5 data-i18n="board.title">应用</h5>
      <span class="sheet-close" id="boardPanelClose">✕</span>
    </div>
    <div class="board-grid" id="boardGrid"></div>
  </div>
</div>
```

---

## JS 逻辑 (board.js)

```javascript
var _boardApps = [];
var _boardActive = null;
var _boardHideTimer = null;

// 种子应用
var DEFAULT_BOARD_APPS = [
  {id:'music-player', name:'音乐', icon:'', type:'local', source:'board-apps/music.html', builtin:true},
  {id:'reader', name:'阅读', icon:'', type:'local', source:'board-apps/reader.html', builtin:true},
  {id:'fitness', name:'健身', icon:'', type:'local', source:'board-apps/fitness.html', builtin:true},
  {id:'notes', name:'笔记', icon:'', type:'local', source:'board-apps/notes.html', builtin:true}
];

function initBoard() {
  // 从 localStorage 加载
  var saved = localStorage.getItem('mov_board_apps_v1');
  _boardApps = saved ? JSON.parse(saved) : DEFAULT_BOARD_APPS;
  if (_boardApps.length > 0 && !_boardActive) _boardActive = _boardApps[0].id;
}

function renderBoardPanel() {
  var h = '';
  _boardApps.forEach(function(app) {
    var active = _boardActive === app.id ? ' active' : '';
    h += '<div class="board-app-card' + active + '" data-app="' + esc(app.id) + '">'
      + '<span class="ba-icon">' + app.icon + '</span>'
      + '<span class="ba-name">' + esc(app.name) + '</span></div>';
  });
  h += '<div class="board-app-card" id="boardPanelAdd">'
    + '<span class="ba-icon">＋</span>'
    + '<span class="ba-name">添加应用</span></div>';
  $('boardGrid').innerHTML = h;

  // 绑定点击
  document.querySelectorAll('.board-app-card[data-app]').forEach(function(el) {
    el.addEventListener('click', function() {
      var id = el.getAttribute('data-app');
      _boardActive = id;
      loadBoardApp(id);
      closeBoardPanel();
      showBoardTrigger();
    });
    // 长按删除
    var app = _boardApps.find(function(a) { return a.id === el.getAttribute('data-app'); });
    if (app && !app.builtin) {
      bindLongPress(el, {
        text: t('board.remove'),
        exec: function() {
          _boardApps = _boardApps.filter(function(a) { return a.id !== app.id; });
          saveBoardApps();
          if (_boardActive === app.id) { _boardActive = null; $('boardFrame').src = ''; }
          renderBoardPanel();
          closeBoardPanel();
        }
      });
    }
  });
  $('boardPanelAdd').addEventListener('click', openBoardAddSheet);
}

function loadBoardApp(id) {
  var app = _boardApps.find(function(a) { return a.id === id; });
  if (!app) return;
  var src = app.type === 'url' ? app.source : app.source; // 本地路径相对于 assets/
  $('boardFrame').src = src;
  $('boardTriggerIcon').textContent = app.icon;
  $('boardTriggerName').textContent = app.name;
}

function showBoardTrigger() {
  var el = $('boardTrigger');
  el.classList.remove('hidden');
  clearTimeout(_boardHideTimer);
  _boardHideTimer = setTimeout(function() { el.classList.add('hidden'); }, 3000);
}

function openBoardPanel() {
  $('boardPanelMask').classList.add('open');
  $('boardPanel').classList.add('open');
  renderBoardPanel();
  clearTimeout(_boardHideTimer);
}

function closeBoardPanel() {
  $('boardPanelMask').classList.remove('open');
  $('boardPanel').classList.remove('open');
  showBoardTrigger();
}

// 事件绑定 (在 app.js 中)
$('boardTrigger').addEventListener('click', openBoardPanel);
$('boardPanelMask').addEventListener('click', closeBoardPanel);
$('boardPanelClose').addEventListener('click', closeBoardPanel);

// 触碰底部区域唤醒触发条
document.addEventListener('touchstart', function(e) {
  if (curTab !== 'board') return;
  var y = e.touches[0].clientY;
  var h = window.innerHeight;
  if (y > h - 80) showBoardTrigger();
});
```

---

## 对比 v1.0

| 方面 | v1.0 (旧) | v2.0 (新) |
|------|----------|----------|
| Dock 位置 | 顶部固定一行 | 不存在 |
| 应用选择 | 点 Dock 图标直接切 | 底部触发条 → 展开面板 → 选应用 |
| 内容占比 | Dock 占了 50px | 全屏, 触发条自动隐藏 |
| 风格 | 2001 年 macOS | 2024 年 iOS/Android 浮层 |
| 首次体验 | 看到一排图标 | 看到全屏内容, 底部隐约有个条 |
