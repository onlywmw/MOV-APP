# DESIGN: 看板 v1.0 — 轻应用桌面

版本: v1.0
日期: 2026-07-21
状态: 📐 design-ready

---

## 核心定义

> **看板 = macOS 式的 Dock 启动器 + 轻应用容器。**
> 不是浏览器，不是 WebView 套壳。是桌面小组件级别的微应用面板。

参考: macOS Dock + Dashboard Widgets，不是 Chrome 浏览器。

---

## 1. Tab 结构

```
会话  │  看板  │  运行
──────┼────────┼──────
AI 协作│轻应用  │设备 + 技能 + Cron
项目  │面板    │
房间  │        │
```

- **会话** (不变): 项目房间、AI 协作
- **看板** (新增): Dock 应用栏 + 当前应用的内容区。轻量、单任务。
- **运行** (扩大): 进程仪表 + 设备工具 + 技能卡片 + Cron 任务。原技能 tab 合并进来。

---

## 2. 看板 UI

```
┌─ 看板 ───────────────────────────────┐
│                                       │
│  ┌── Dock ──────────────────────────┐ │
│  │ 🎵  📖  🏃  📺  📝  [＋]       │ │  ← 应用图标行, 水平排列
│  └──────────────────────────────────┘ │
│                                       │
│  ┌── 应用内容区 ────────────────────┐ │
│  │                                   │ │
│  │   当前选中应用的内容               │ │  ← 微应用运行区
│  │   轻量 HTML / 小组件              │ │
│  │                                   │ │
│  └───────────────────────────────────┘ │
│                                       │
└───────────────────────────────────────┘
```

### Dock 栏

- 水平滚动一行，每个应用一个小图标按钮
- 选中态: 高亮（金色底或金色边框）
- 最右侧 "+" 按钮: 打开添加应用 sheet
- 长按应用图标: 弹出"移除应用"
- 类 macOS 但更轻——没有弹跳动画、没有指示器圆点、没有最小化

### 应用内容区

- 占据 Dock 下方的全部剩余空间
- 加载当前选中应用的 HTML 内容
- 可以是 iframe (远程 URL) 或内联 HTML (本地组件)

---

## 3. 应用模型

### 应用 = 一个有图标 + 名称 + 内容源的入口

```json
{
  "id": "music-player",
  "name": "音乐",
  "icon": "🎵",
  "type": "local",       // local | url
  "source": "board-apps/music.html",  // 本地路径 或 https://...
  "builtin": true,       // 系统自带? true = 不可删除
  "addedAt": 1721404800000
}
```

### 系统自带应用 (可删除)

| id | 名称 | 图标 | 内容 |
|----|------|------|------|
| `music-player` | 音乐 | 🎵 | 简单音频播放器 (本地 HTML) |
| `reader` | 阅读 | 📖 | 文本/PDF 阅读器 |
| `fitness` | 健身 | 🏃 | 训练计时器 (之前 Council 的产物) |
| `notes` | 笔记 | 📝 | 简易备忘录 |

### 用户自行添加

在设置里: "添加应用" → 填名称 + 图标 emoji + URL (可选本地 HTML 路径或远程 URL)。

---

## 4. 数据存储

### 应用的配置存在哪里

- **本地配置**: `SharedPreferences` → `mov_legacy_board_apps` key → JSONArray
- **自带应用的 HTML**: `assets/board-apps/<app-id>.html`
- **用户添加的应用**: 如果 type=url → 直接加载远程 URL。如果 type=local → 用户需先把 HTML 放到指定目录。

简化方案: 第一版只支持两种来源——
1. 内置: `assets/board-apps/` 下的 HTML 文件
2. 远程: 用户填入的 HTTPS URL (iframe 加载)

---

## 5. 看板 JS 组件

### 看板 = 纯 JS 前端，不需要 Java 改动

现有架构已经支持: WebView 可以加载 iframe / innerHTML。看板完全在 `hermes-shell.html` 已有的 WebView 内实现。

### 新增文件: `js/board.js`

```javascript
/* ============================================================
   board.js — 看板 tab: Dock + 应用容器
   ============================================================ */

var _boardApps = [];      // 应用列表
var _boardActive = null;  // 当前选中的应用 ID

function initBoard() {
    _boardApps = B.listBoardApps();  // 从 Java 读应用列表
    if (!_boardApps.length) {
        // 种子自带应用
        _boardApps = [
            {id:'music-player',name:'音乐',icon:'🎵',type:'local',source:'board-apps/music.html',builtin:true},
            {id:'reader',name:'阅读',icon:'📖',type:'local',source:'board-apps/reader.html',builtin:true},
            {id:'fitness',name:'健身',icon:'🏃',type:'local',source:'board-apps/fitness.html',builtin:true},
            {id:'notes',name:'笔记',icon:'📝',type:'local',source:'board-apps/notes.html',builtin:true}
        ];
    }
}

function renderBoardDock() {
    var h = '';
    _boardApps.forEach(function(app) {
        var sel = (_boardActive === app.id) ? ' sel' : '';
        h += '<span class="dock-item' + sel + '" data-app="' + esc(app.id) + '" title="' + esc(app.name) + '">'
            + app.icon + '</span>';
    });
    h += '<span class="dock-item dock-add" id="dockAdd">＋</span>';
    $('boardDock').innerHTML = h;

    // 绑定点击: 切换应用
    document.querySelectorAll('#boardDock .dock-item[data-app]').forEach(function(el) {
        el.addEventListener('click', function() {
            _boardActive = el.getAttribute('data-app');
            loadBoardApp(_boardActive);
            renderBoardDock();
        });
    });
    // 长按: 删除 (内置应用不删)
    document.querySelectorAll('#boardDock .dock-item[data-app]').forEach(function(el) {
        var appId = el.getAttribute('data-app');
        var app = _boardApps.find(function(a) { return a.id === appId; });
        if (app && !app.builtin) {
            bindLongPress(el, {
                text: t('board.remove'),
                exec: function() {
                    _boardApps = _boardApps.filter(function(a) { return a.id !== appId; });
                    B.saveBoardApps(_boardApps);
                    if (_boardActive === appId) { _boardActive = null; $('boardContent').innerHTML = ''; }
                    renderBoardDock();
                }
            });
        }
    });
}

function loadBoardApp(appId) {
    var app = _boardApps.find(function(a) { return a.id === appId; });
    if (!app) return;
    if (app.type === 'local') {
        // 加载本地 HTML 文件到 iframe
        $('boardContent').innerHTML = '<iframe src="' + app.source + '" '
            + 'style="width:100%;height:100%;border:none;background:#fff;border-radius:var(--r-lg)"></iframe>';
    } else if (app.type === 'url') {
        // 加载远程 URL
        $('boardContent').innerHTML = '<iframe src="' + esc(app.source) + '" '
            + 'sandbox="allow-scripts allow-same-origin" '
            + 'style="width:100%;height:100%;border:none;background:#fff;border-radius:var(--r-lg)"></iframe>';
    }
}
```

### 打开看板 tab 时

```javascript
if (t === 'board') {
    renderBoardDock();
    if (_boardActive) loadBoardApp(_boardActive);
}
```

---

## 6. 自带应用示例: 音乐播放器

### 文件: `assets/board-apps/music.html`

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  *{margin:0;padding:0;box-sizing:border-box;}
  body{font-family:-apple-system,sans-serif;background:#F6F6F7;padding:16px;
       display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100%;}
  h3{margin-bottom:12px;font-size:16px;}
  audio{width:100%;max-width:320px;margin:8px 0;}
  .note{font-size:11px;color:#71717A;margin-top:16px;text-align:center;}
</style>
</head>
<body>
  <h3>音乐播放器</h3>
  <p style="font-size:12px;color:#71717A;margin-bottom:12px">将音乐文件放入 /sdcard/music/ 目录</p>
  <audio controls>
    <source src="file:///sdcard/music/current.mp3" type="audio/mpeg">
  </audio>
  <div class="note">支持 MP3 / M4A / WAV 格式<br>可通过 Cron 定时下载新歌</div>
</body>
</html>
```

### 其他自带应用

| 应用 | 功能 | 实现方式 |
|------|------|---------|
| 阅读 | 显示 `/sdcard/books/` 下的 txt 文件列表 + 阅读器 | 本地 HTML + JS 读文件列表 |
| 健身 | 训练计时器 + 动作列表 | 纯 JS 倒计时 + localStorage 记录 |
| 笔记 | 简易 Markdown 编辑器 | textarea + 本地存储 |

---

## 7. 应用管理

### 设置页新增"看板应用管理"

在 `≡` 设置中新增一个区域:

```
设置
├─ AI 团队
├─ 看板应用          ← 新增
│   ├─ 🎵 音乐 (内置)
│   ├─ 📖 阅读 (内置)
│   ├─ 🏃 健身 (内置)
│   ├─ 📝 笔记 (内置)
│   └─ ＋ 添加应用
├─ 偏好
├─ 权限管理
└─ 关于
```

但设置目前是 Java Activity，改起来重。**第一版用前端 Sheet**:

在看板 tab 的 Dock 右侧 "＋" 按钮 → 弹出 Sheet:

```
┌─────────────────────────────┐
│ 添加应用               [✕]  │
├─────────────────────────────┤
│ 名称                        │
│ ┌───────────────────────┐   │
│ │ 我的博客               │   │
│ └───────────────────────┘   │
│                             │
│ 图标 (emoji)                │
│ ┌───────────────────────┐   │
│ │ 🌐                     │   │
│ └───────────────────────┘   │
│                             │
│ 来源                        │
│ ┌─────────────────────┐     │
│ │ ● 远程 URL          │     │
│ │ ○ 本地 HTML 文件    │     │
│ └─────────────────────┘     │
│                             │
│ URL                         │
│ ┌───────────────────────┐   │
│ │ https://myblog.com    │   │
│ └───────────────────────┘   │
│                             │
│ [      添加应用      ]       │
└─────────────────────────────┘
```

---

## 8. 技能并入运行 tab

### 运行 tab 新结构

```
运行 tab
  PROCESS      (pid / uptime / mem / cmds)
  DEVICE       (设备工具网格: 手电筒/截屏/音量...)
  SKILLS       (技能列表 + 搜索, 从技能 tab 移入)
  CRON         (定时任务 + 创建)
  CHANNELS     (4 通道状态)
  MODEL        (2 行模型)
  PERMISSIONS  (横滚权限)
```

### HTML: view-run 里加技能区

在 CRON 区后面 / PERMISSIONS 区前面:

```html
<!-- SKILLS (v4: 从独立 tab 合并到运行) -->
<div class="sub-head" id="skillHead">SKILLS</div>
<div class="auto-input"><input class="field" id="skillSearch" data-i18n-ph="skill.search" placeholder="搜索技能…"></div>
<div id="skillList"></div>
```

### JS: render.js setTab

```javascript
if (t === 'run') {
    refreshRuntime();
    renderSkillPage();  // ← 新增, 进运行 tab 也渲染技能
}
```

---

## 9. 底部导航

```
┌──────────┬──────────┬──────────┐
│   ▤      │   ◈      │   ▣     │
│  会话    │  看板    │  运行    │
└──────────┴──────────┴──────────┘
```

---

## 10. 需要改的文件

| 文件 | 改动 | 复杂度 |
|------|------|--------|
| `hermes-shell.html` | 删 view-skill; 增 view-board (Dock + content iframe); view-run 加 SKILLS 区 | 中 |
| `css/shell.css` | 增 `.dock-item` + `.board-content` 样式 | 低 |
| `js/board.js` | **新建** — Dock 渲染 + 应用切换 + 添加/删除 + 种子数据 | 中 |
| `js/render.js` | setTab: `skill`→`board`, 运行 tab 加 `renderSkillPage()` | 低 |
| `js/skills.js` | 技能列表改用 `#skillSearch` 的 ID 适配运行页 | 低 |
| `js/app.js` | 初始化: `board` 替换 `skill`; 绑定 +sheet 事件 | 中 |
| `js/i18n.js` | 新增 ~10 条 key | 低 |
| `js/bridge.js` | 新增 `listBoardApps` / `saveBoardApps` (或纯前端 localStorage) | 低 |
| `assets/board-apps/*.html` | **新建** — 4 个自带应用 HTML | 中 |

### 底部导航按钮

```html
<button data-tab="chat" class="on">会话</button>
<button data-tab="board">看板</button>           ← 原名 skill
<button data-tab="run">运行</button>
```

---

## 11. 应用存储方案

第一版: **纯前端 localStorage**。

应用列表存在 `localStorage` key `mov_legacy_board_apps`。不需要 Java 桥方法。

```javascript
// store.js 或 board.js
var BOARD_KEY = 'mov_legacy_board_apps_v1';
var DEFAULT_BOARD_APPS = [
    {id:'music-player',name:'音乐',icon:'🎵',type:'local',source:'board-apps/music.html',builtin:true},
    {id:'reader',name:'阅读',icon:'📖',type:'local',source:'board-apps/reader.html',builtin:true},
    {id:'fitness',name:'健身',icon:'🏃',type:'local',source:'board-apps/fitness.html',builtin:true},
    {id:'notes',name:'笔记',icon:'📝',type:'local',source:'board-apps/notes.html',builtin:true}
];
```

Java 层零改动。这对 widget 级别的轻应用来说足够了。

---

## 12. 验证清单

- [ ] 底部导航: `会话 | 看板 | 运行`
- [ ] 看板 tab: Dock 栏显示 4 个自带应用 + 添加按钮
- [ ] 点击 Dock 图标: 应用内容区加载对应 HTML
- [ ] 点击 ＋: Sheet 弹出, 填名称+图标+URL → 应用加入 Dock
- [ ] 长按非内置应用: 浮出"移除" → 应用消失
- [ ] 内置应用长按: 不浮出操作条
- [ ] 运行 tab: 首屏出现 SKILLS 区, 技能卡片可搜索可长按删除
- [ ] 会话 tab 不变
- [ ] 自带音乐应用: audio 标签可播放本地音乐
- [ ] 自带健身应用: 计时器可启动/暂停
