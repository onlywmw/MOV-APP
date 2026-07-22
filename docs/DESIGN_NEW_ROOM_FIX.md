# DESIGN: 新建房间 — 问题诊断 & 修复

版本: v1.0
日期: 2026-07-22
status: design-ready

---

## 发现的 Bug（5个）

### B1: `_pickedModels` 不重置

`_pickedModels` 是全局数组。用户第一次打开 Sheet → 选议会 → 勾了两个模型 → 关闭 Sheet → 再打开 → 上次勾选的模型还在 `_pickedModels` 里，但 UI 已经重新渲染了（默认只勾默认模型）。数组和 UI 不同步。

**改法**: `openSheet` 时重置 `_pickedModels=[]`。

### B2: 单聊模式 members 格式不一致

```javascript
// council: {human:[...], ai:[...]}
// single:  ['mov']   ← 旧格式数组
members: ['mov'];
```

store.js 的 `roomAiMembers()` 兼容了新旧格式，但新代码不应该产生旧格式。统一用 `{human, ai}`。

**改法**: 单聊模式也用 `{human:[{who:'you',role:'owner'}], ai:['mov']}`。

### B3: fit 房间还是硬编码旧 AI 名字

`store.js` DEFAULT_ROOMS 的 fit 房间:

```javascript
members: ['claude','gpt-5','gemini']
```

新用户打开 fit 房间会看到三个不存在于 ModelRegistry 的 AI。而且 `avstack()` 渲染时找不到对应的 AV 颜色会走默认值。

**改法**: fit 房间改为 `{human, ai:[]}` 格式，ai 数组清空（因为没有配置模型可以选默认），或者直接删除 fit 房间。

### B4: `renderModelPicker` 没有 empty state

`B.listModels()` 返回空数组时，picker 渲染空白。用户点了"拉 AI 团队"，下面空空如也，不知道怎么选。

**改法**: 空列表时显示"暂无模型，去 ≡ 设置添加" + 跳转按钮。

### B5: 第二步有两个返回按钮

HTML 里有 `btnStep2Back`（← 箭头）和 `btnStep2Prev`（"上一步"按钮）。两个都做同一件事。多余且占空间。

**改法**: 只保留 ← 箭头，删掉 `btnStep2Prev`。

---

## 体验问题（3个）

### E1: 第一步"下一步"按钮在键盘弹起时被遮挡

输入框在 Sheet 底部，"下一步"按钮在输入框下面。键盘弹起 → Sheet 被顶上去 → 按钮被推出屏幕。

**改法**: 把按钮固定在 Sheet 底部（`position:sticky;bottom:0;background:var(--panel)`），键盘弹起时按钮始终可见。

### E2: 选"议会"后模型列表突然出现，视觉跳跃

第二步两个模式卡片下面是空的。点了"拉 AI 团队"→ 模型勾选列表突然撑开，视觉跳跃。

**改法**: 模型列表始终渲染，但用 `max-height` 过渡动画收起/展开，不用 `display:none`。

### E3: 创建后 seed 消息用 `who:'mov'` 但 AV 表里 mov 的颜色是默认金黄

`seed` 里 `{t:'agent',who:'mov',h:...}` —— 渲染时 `mkMsg` 用 `AV[m.who]` 找头像。`AV.mov` 存在，颜色 `#D97706`。但如果房间是 council 模式，用户期待看到的是默认 AI 模型的名字（如"DeepSeek V4"）而不是"mov"。

**改法**: seed 消息的 who 用默认模型的名字或"MOV"，保持一致。

---

## 改动清单

| # | 文件 | 改什么 |
|---|------|--------|
| B1 | `js/app.js` | openSheet 时重置 `_pickedModels=[]`; 删 `btnStep2Prev` 绑定 |
| B2 | `js/app.js` | 单聊 mode 的 members 改为 `{human, ai}` 格式 |
| B3 | `js/store.js` | fit 房间 members 改新格式或删除 fit 房间 |
| B4 | `js/app.js` | `renderModelPicker` 加 empty state |
| B5 | `hermes-shell.html` | 删 `btnStep2Prev` 按钮；模型列表始终渲染 |
| E1 | `hermes-shell.html` | 创建按钮加 sticky bottom |
| E2 | `css/shell.css` | 模型列表 max-height 过渡 |
| E3 | `js/app.js` | seed 消息 who 保持 `'mov'`（已有 MOV 头像） |

---

## 不改的

- 两步结构保留。第一步填信息 → 第二步选模式 + 模型。
- `?` 和 `≡` 已移除，设置入口在运行页。
- 顶栏去留问题——那是另一个独立改动，不混在这次修。
