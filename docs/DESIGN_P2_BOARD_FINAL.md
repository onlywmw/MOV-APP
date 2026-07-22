# DESIGN: P2 — 看板彻底删除

版本: v1.0
日期: 2026-07-22
status: design-ready

---

## 问题

FIX1 的方案是"隐藏底部入口，代码保留"。这是半吊子——代码库里有 `board.js`（131行）、`board-apps/`（4个HTML文件）、`js/app-board.js`（事件绑定）、`view-board` HTML 区块、看板相关 CSS（~80行）。任何新开发者看到这些文件都会困惑："这些是干什么的？为什么还留着？"

"代码保留但不暴露入口" = 未完成的技术债 = V5 重构时的双倍代价。

---

## 方案：彻底删除

### 删文件

```
rm app/src/main/assets/js/board.js
rm app/src/main/assets/js/app-board.js
rm app/src/main/assets/board-apps/music.html
rm app/src/main/assets/board-apps/reader.html
rm app/src/main/assets/board-apps/fitness.html
rm app/src/main/assets/board-apps/notes.html
rmdir app/src/main/assets/board-apps/
```

### 删 HTML

`hermes-shell.html`：
- 删除 `#view-board` 的整个 div（约 30 行，包括 iframe、触发条、面板、添加应用 sheet）
- 底部导航删除 `<button data-tab="board">`
- `<script src="js/board.js">` 删除
- `<script src="js/app-board.js">` 删除

### 删 CSS

`shell.css`：
- `.board-trigger` `.board-trigger.hidden` `.board-panel-mask` `.board-panel` `.board-grid` `.board-app-card` `.board-app-card.active` `.board-app-card .ba-icon` `.board-app-card .ba-name` — 约 50 行

### 删 JS 引用

`render.js` setTab：删除 `if(t==='board')` 分支。  
局部变量：`_boardApps`、`_boardActive`、`_boardHideTimer`、`_boardInited` — 都在已删除的 board.js 里，不影响。

---

## 影响

| 维度 | 改前 | 改后 |
|------|------|------|
| 底部导航 | 会话 / 看板 / 运行 | 会话 / 运行 |
| 源文件数 | +6 个 board 相关文件 | 0 |
| 代码行数 | ~350 行 board 相关 | 0 |
| 用户困惑 | "为什么有音乐播放器" | 不再困惑 |
| 以后想恢复 | 从 git history 找回 | — |

---

## 如果以后想做看板

不是回到现在的"简陋桌面"方案。而是实现 `MOV_MASTER.md` 0.2 节写的"房间产出部署到看板"——当时这只是口号，现在砍掉看板意味着我们承认：**那个口号在当前阶段没有实现路径。**

以后真想做了，设计成"轻应用插件系统"：看板只有一个"部署"入口，应用来自房间产出（AI 写的 HTML），不是用户自己添加的 URL。那是独立的设计文档，不放在这里。

---

## 验收

- [ ] `board.js`、`app-board.js` 不存在
- [ ] `board-apps/` 目录不存在
- [ ] `hermes-shell.html` 无看板相关 HTML
- [ ] `shell.css` 无看板相关样式
- [ ] 底部导航只有"会话"和"运行"
- [ ] 构建通过，测试通过
- [ ] 运行页和聊天功能不受影响
