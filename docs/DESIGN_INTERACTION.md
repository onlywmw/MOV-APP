# DESIGN: MOV 交互系统 v3.0

版本: v1.0
日期: 2026-07-21
状态: ✅ 已实施

---

## 1. 设计理念

三条铁律:

1. **每操作必有反馈** — 用户做任何事情，必须有 toast 或视觉变化回应
2. **每对象必有归宿** — 能创建就能删除，能打开就能关闭
3. **触屏优先** — 长按 = 右键菜单，sheet = 对话框，拒绝 `prompt()` 弹窗

---

## 2. 交互模式

### 2.1 长按 → 操作条

> 触屏设备没有右键菜单，长按替代

```
触发: 手指按住目标 500ms，移动超过 10px 取消
效果: 目标高亮 (金色边框) + 底部浮出操作条 (黑底白字)
点击: 执行操作，toast 反馈，操作条消失
点击外部: 操作条消失
```

适用对象: 消息气泡 / 技能卡片 / 房间卡片

### 2.2 Sheet 面板

> 替代浏览器 `prompt()` / `confirm()`，和系统 UI 风格一致

```
触发: 点击按钮 (⋮ / + / ✕)
出现: 遮罩 + 底部面板从下滑入
关闭: 点击遮罩 / 点击 ✕ / 操作完成
内部可切换子面板: 菜单 → 确认态 / 菜单 → 输入态
```

### 2.3 开关切换

```
触发: 点击开关组件
效果: 即时切换状态 (无二次确认) + 200ms 后刷新数据
适用: Cron 任务启用/禁用
```

---

## 3. 操作对应总表

### 3.1 房间

| 操作 | 入口 | 确认方式 | 反馈 | 限制 |
|------|------|---------|------|------|
| 创建房间 | FAB "+" → sheet | 填写名称 + 选模式 → "创建房间" | 直接进入新房间 | — |
| 进入房间 | 点击房间卡片 | 无 | 页面切换到聊天详情 | — |
| 重命名 | ⋮ / 长按 → sheet → "重命名" | sheet 切换输入态 → 确认 | 标题刷新 + 列表刷新 | desk 不可操作 |
| 归档 | ⋮ / 长按 → sheet → "归档" | 无二次确认 | toast "已归档" + 列表刷新 | desk 不可操作 |
| 清空聊天 | ⋮ / 长按 → sheet → "清空" | sheet 切换确认态 → 确认 | toast "聊天记录已清空" + 聊天区清零 | — |
| 删除房间 | ⋮ / 长按 → sheet → "删除" | sheet 切换确认态 → 确认 | toast "房间已删除" + 返回列表 | desk 不可操作 |
| 返回列表 | ← / 系统返回键 | 无 | 回到房间列表 | — |

### 3.2 消息

| 操作 | 入口 | 确认方式 | 反馈 |
|------|------|---------|------|
| 发送消息 | 输入框 + 回车/↑ | 无 | 消息出现在聊天区 |
| 删除消息 | 长按气泡 → 操作条 | 点击"删除这条消息"直接执行 | toast "已删除" + 消息消失 + 自动持久化 |
| 查看工具输出 | 点击工具卡片 | 无 | 卡片展开/折叠 |

### 3.3 技能

| 操作 | 入口 | 确认方式 | 反馈 |
|------|------|---------|------|
| 浏览技能 | 底部"技能" tab | 无 | 渲染技能列表 |
| 搜索技能 | 搜索框输入 | 即时过滤 | 匹配项保留 |
| 触发技能 | 点击技能卡片 | 无 | toast "技能已触发" + 调用计数 +1 |
| 移除技能 | 长按卡片 → 操作条 | 点击"移除技能"直接执行 | toast "技能已移除" + 列表重渲染 |

### 3.4 Cron 任务

| 操作 | 入口 | 确认方式 | 反馈 |
|------|------|---------|------|
| 创建任务 | 自然语言输入 + "创建" | 无 | toast "任务已创建" + 任务卡片出现 |
| 启用/禁用 | 点击开关 | 无 | 开关变色 + 200ms 后刷新 |
| 删除任务 | 卡片底部"删除" | `confirm("删除此任务?")` | toast "任务已删除" + 卡片消失 |

### 3.5 AI 设置

| 操作 | 入口 | 确认方式 | 反馈 |
|------|------|---------|------|
| 打开设置 | 房间列表 ≡ | 无 | 跳转设置 Activity |
| 切换 Provider | Spinner 选择 | 无 | URL/Model 自动填入预设值 |
| 填写 Key | 输入框 | 无 | — |
| 清除 Key | 输入框右侧 ✕ | 无 | 输入框清空 |
| 保存 | "保存" 按钮 | 无 | toast "已保存" |
| 测试连接 | "测试连接" 按钮 | 无 | toast "✅ 连接成功" 或 "❌ 错误" |
| 返回 | "返回" 按钮 / 系统返回 | 无 | 回到 MOV 主界面 |

### 3.6 桌面小组件

| 操作 | 入口 | 确认方式 | 反馈 |
|------|------|---------|------|
| 执行指令 | 点击列表项 | 无 | toast ✅/❌ + 结果 |
| 刷新列表 | 刷新按钮 | 无 | toast "已刷新" |
| 打开应用 | 应用图标 | 无 | 启动 MOVActivity |

### 3.7 全局导航

| 操作 | 入口 | 目标 |
|------|------|------|
| 会话 tab | 底部导航 "会话" | 房间列表 / 聊天详情 |
| 技能 tab | 底部导航 "技能" | 技能列表 + 搜索 |
| 运行 tab | 底部导航 "运行" | 进程仪表 + 通道 + Cron + 模型 |
| 帮助 | 房间列表 "?" | 进入 desk 房间 + 自动发 "帮助" |
| 新建房间 sheet 关闭 | ✕ / 遮罩 | sheet 滑下 |
| 房间操作 sheet 关闭 | ✕ / 遮罩 | sheet 滑下 |

---

## 4. 运行页信息架构

> 运行页不是垃圾桶——只放运行时数据

```
运行 tab
  PROCESS      (pid / uptime / JVM 内存 / 指令计数 / 最近指令)
  CHANNELS     (APP SHELL / WIDGET / AI GATEWAY / NOTIFY)
  CRON         (创建输入框 + 任务列表)
  MODEL        (NATIVE ENGINE / AI GATEWAY 状态行)
  PERMISSIONS  (横滚权限标签, 点击跳系统设置)
```

已删除的冗余:
- ❌ DEVICE STATE 区 (电量/WiFi 顶栏已有)
- ❌ SKILLS 区 (独立 tab)
- ❌ runStrip 运行条 (底栏已有运行按钮)

---

## 5. 数据流

### 5.1 消息删除 (含索引同步)

```
长按消息气泡
  → bindLongPress(node, action) — touchstart + mousedown 双事件
  → 500ms 后 triggerLongPress()
    → node 加 .longpress-hl (金色边框)
    → showMsgActions("删除这条消息", callback)
      → #msgActions 从底部浮出
  → 用户点击操作条
    → deleteMessage(roomId, idx)
      → room.msgs.splice(idx, 1)
      → room.msgData.splice(idx, 1) — 索引一一对应
      → persistRooms() — 写 localStorage
      → 重渲染聊天区
      → toast "已删除"
```

### 5.2 房间操作 Sheet 状态机

```
openRoomOpsSheet(id)
  → [菜单态] 显示 4 个操作行

菜单态:
  重命名 → [输入态] 输入框 + 确认/取消
  归档   → 直接执行 → toast → 关闭
  清空   → [确认态] "确定清空？" + 确认/取消
  删除   → [确认态] "确定删除？" + 确认/取消

确认态: 确认 → 执行 → toast → 关闭
         取消 → 退回菜单态

输入态: 确认 → 执行 → toast → 关闭
         取消 → 退回菜单态

关闭: 遮罩 / ✕ → 全部重置
```

### 5.3 技能删除

```
长按技能卡片
  → triggerLongPress()
  → showMsgActions("移除技能", callback)
  → 用户点击
    → B.deleteSkill(skillId)
      → MOVBridge.deleteSkill(id)
        → SkillStore.deleteSkill(id)
        → 从 SharedPreferences JSONArray 移除
        → 返回 {"ok":true}
    → toast "技能已移除"
    → renderSkillPage() 重渲染
```

---

## 6. 组件规范

### 6.1 Sheet (底部面板)

```html
<!-- 遮罩 -->
<div class="sheet-mask" id="xxxMask"></div>

<!-- 面板 -->
<div class="sheet" id="sheetXxx">
  <!-- 标题栏: 标题 + ✕ -->
  <div style="display:flex;align-items:center;justify-content:space-between">
    <h5>标题</h5>
    <span class="sheet-close" id="btnXxxClose">✕</span>
  </div>
  <!-- 内容 -->
  ...
</div>
```

- `.sheet-mask` — 半透明黑底, z-index:9
- `.sheet` — 白色面板, 底部滑入, z-index:10
- `.sheet-close` — ✕ 按钮, 点击 = 关闭
- 打开: `mask.classList.add('open')` + `sheet.classList.add('open')`
- 关闭: `.remove('open')` 两个同时

### 6.2 操作行 (Sheet 内菜单项)

```html
<div class="sheet-row">普通操作</div>
<div class="sheet-row danger">危险操作</div>
```

- `.sheet-row` — 等宽字体, 12px, 粗体, 底部分隔线
- `.sheet-row.danger` — 红色

### 6.3 长按操作条

```html
<div class="msg-actions" id="msgActions">
  <span id="msgActionText"></span>
</div>
```

- 绝对定位, 底部 70px, 左右 12px
- 黑底白字, 圆角, 阴影
- 默认 `opacity:0` / `visibility:hidden`
- `.show` → 浮出

### 6.4 长按高亮

```css
.msg.longpress-hl .bubble   { 消息气泡金色边框 }
.skill.longpress-hl          { 技能卡片金色边框 }
```

---

## 7. i18n 覆盖

所有面向用户的文字必须通过 `t(key)` 调用。新增交互的 key:

| Key | 中文 | 英文 |
|-----|------|------|
| `ops.title` | 房间操作 | Room Actions |
| `ops.rename` | 重命名 | Rename |
| `ops.archive` | 归档 | Archive |
| `ops.clear` | 清空聊天记录 | Clear chat history |
| `ops.delete` | 删除房间 | Delete room |
| `ops.confirm` | 确认 | Confirm |
| `ops.cancel` | 取消 | Cancel |
| `ops.confirmClear` | 确定清空所有聊天记录？此操作不可撤销。 | Clear all messages? This cannot be undone. |
| `ops.confirmDelete` | 确定删除此房间？此操作不可撤销。 | Delete this room? This cannot be undone. |
| `ops.cleared` | 聊天记录已清空 | Chat history cleared |
| `ops.archived` | 已归档 | Archived |
| `ops.deleted` | 房间已删除 | Room deleted |
| `msg.delete` | 删除这条消息 | Delete this message |
| `msg.deleted` | 已删除 | Deleted |
| `skill.remove` | 移除技能 | Remove skill |
| `skill.removed` | 技能已移除 | Skill removed |
| `cron.deleted` | 任务已删除 | Task deleted |

---

## 8. 涉及文件

### 前端 (9 JS + 1 CSS + 1 HTML)

| 文件 | 职责 |
|------|------|
| `hermes-shell.html` | 全 UI 骨架, 4 个 view + 2 个 sheet + 操作条 |
| `css/shell.css` | 设计系统 + `.sheet-row` + `.msg-actions` + `.longpress-hl` |
| `js/store.js` | 房间持久化, AV/PHASE_BADGE 常量, $/ev/esc 工具 |
| `js/i18n.js` | 中英双语字典 + t() 函数 |
| `js/bridge.js` | MOVBridge 封装 + 异步回调 + 全局错误边界 |
| `js/render.js` | DOM 渲染: 房间列表 / 消息 / 视图切换 / 阶段变更 |
| `js/council.js` | fit 房间硬编码演示剧本 (不参与真实 AI 调用) |
| `js/chat.js` | 消息路由 + 长按基础设施 + 消息删除 + 清空历史 |
| `js/skills.js` | 技能列表 + 搜索过滤 + 长按移除 |
| `js/runtime.js` | 运行页: 进程/通道/模型/Cron/权限 |
| `js/app.js` | 事件绑定 + 房间操作 sheet + 新建房间 sheet + Cron 创建 + 初始化 |

### Java (17 文件)

| 文件 | 职责 |
|------|------|
| `MOVActivity.java` | WebView 壳 + JS 桥 (22 个 @JavascriptInterface 方法) |
| `MOVApplication.java` | 启动清理弃用模型名 |
| `MOVSettingsActivity.java` | AI 设置页 (Spinner + 输入框 + ✕ + 保存 + 测试连接) |
| `CapabilityExecutor.java` | 30 个原生设备能力 |
| `IntentParser.java` | 中英文自然语言 → ParsedCommand |
| `ParsedCommand.java` / `CommandResult.java` | 数据类 |
| `AiClient.java` | OpenAI 兼容客户端 (支持临时 system prompt) |
| `AiProviderConfig.java` | AI 配置 SharedPreferences 持久化 |
| `CouncilClient.java` | 真实 AI 多角色讨论 (3 system prompt + 汇总) |
| `CronManager.java` | WorkManager 调度引擎 |
| `MOVCronWorker.java` | Cron 任务执行 Worker |
| `SkillStore.java` | 技能 CRUD (list/recordUse/deleteSkill) |
| `MOVWidgetProvider.java` | 桌面小组件 Provider |
| `MOVWidgetService.java` | 小组件数据源 (14 快捷指令) |

---

## 9. 设计决策记录

| 决策 | 结论 | 原因 |
|------|------|------|
| 删除 runStrip | ✅ 删 | 底栏已有运行 tab, 无需重复入口 |
| 删除 DEVICE STATE 区 | ✅ 删 | 电量/WiFi 顶栏已有, IP 从未真实填充 |
| 附件托盘假数据 | ✅ 改通用按钮 | 假文件名误导用户 |
| 房间操作用 sheet 替代 prompt | ✅ sheet | prompt 弹窗在 WebView 体验差 |
| 消息删除: 直接执行 vs confirm | ✅ 直接执行 | 有 toast 反馈 + 可手动重发, confirm 多余 |
| 技能删除: 直接执行 vs confirm | ✅ 直接执行 | 同消息 |
| Cron 删除保留 confirm | ✅ 保留 confirm | 定时任务影响大, 误删成本高 |
| 权限保留在运行页 | ⚠️ 暂留 | 移到设置页需改 Java Activity, 下个版本做 |
| 不实现撤销 | ✅ 不做 | 基础设施太重, toast 反馈足够 |
