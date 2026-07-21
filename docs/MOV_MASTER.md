# MOV — 多模型协作工作台

版本: v3.1
日期: 2026-07-21

---

## 0. 一句话定义

> MOV 是一个运行在 Android 平板上的**多模型协作工作台**。
> 人和 AI 大模型在项目房间里共同工作，产出文件和决策。
> 有一个看板 Dock 运行轻应用（音乐、阅读、健身…），
> 可以直接操控设备（手电筒、音量、截屏…）。

---

## 1. 产品架构

```
                MOV
                   │
    ┌──────────────┼──────────────┐
    │              │              │
  会话          看板            运行
 AI 协作     轻应用面板      系统 & 技能
 项目房间     Dock + Web     设备 & Cron
```

| Tab | 是什么 | 谁在用 | 频次 |
|-----|--------|--------|------|
| **会话** | 项目房间。人和 AI 在房间里讨论、写文件、投票决策 | 所有人 | 90% |
| **看板** | 轻应用启动器。音乐、阅读、健身… Dock 栏 + 内容区 | 个人 | 5% |
| **运行** | 系统仪表。设备控制 + 技能库 + Cron + 通道状态 | 管理员 | 5% |

---

## 2. 会话 — 项目房间

### 房间 = GitHub Repo 的样子

```
房间: 产品 V2.0
├─ [讨论]                        ← 聊天。人说话，AI 说话。MOV 主持。
│   └─ 消息长按删除
│   └─ Council 投票卡
│   └─ 文件卡片 (AI 产出直接嵌入讨论流)
│
├─ [文件]                        ← 文件仓库。
│   └─ 文件树 + 面包屑导航
│   └─ 点击查看 (overlay 预览)
│   └─ 长按删除
│   └─ ＋: 上传 / 新建文件
│
└─ /sdcard/mov/rooms/<id>/   ← 真实磁盘目录
    ├─ README.md
    ├─ src/  docs/  assets/
    └─ .hermes/config.json
```

### 房间类型

| 类型 | 参与者 | 场景 |
|------|--------|------|
| 单聊 | MOV (1 个 AI) | 快捷指令、简单问答 |
| 议会 | MOV + 多个 AI 模型 | 产品讨论、方案评审 |

### 房间操作

| 操作 | 入口 | 交互 |
|------|------|------|
| 创建 | FAB ＋ → Sheet | 填名称 + 提示词 + 选 AI 成员 |
| 重命名 | ⋮ / 长按房间卡片 → Sheet | Sheet 内输入框 |
| 归档 | ⋮ / 长按房间卡片 → Sheet | 直接执行 + toast |
| 清空聊天 | ⋮ / 长按房间卡片 → Sheet | Sheet 确认态 |
| 删除 | ⋮ / 长按房间卡片 → Sheet | Sheet 确认态 |

---

## 3. 看板 — 轻应用面板

### 结构

```
┌─ Dock ──────────────────────────┐
│ 🎵  📖  🏃  📝  🌐  [＋]       │  ← 应用图标, 水平排列
└─────────────────────────────────┘
┌─ 内容区 ────────────────────────┐
│ iframe: 当前应用的内容           │
└─────────────────────────────────┘
```

### 应用

| 来源 | 说明 |
|------|------|
| 系统自带 | 音乐 (🎵)、阅读 (📖)、健身 (🏃)、笔记 (📝) — 可删除 |
| 用户添加 | ＋ Sheet: 填名称 + emoji + URL (远程或本地) |
| 存储 | localStorage `mov_legacy_board_apps_v1` |

### 自带应用

| 应用 | 文件 | 功能 |
|------|------|------|
| 音乐 | `board-apps/music.html` | audio 标签播放 `/sdcard/music/` |
| 阅读 | `board-apps/reader.html` | 文本阅读器 |
| 健身 | `board-apps/fitness.html` | 训练计时器 |
| 笔记 | `board-apps/notes.html` | Markdown 编辑器 |

---

## 4. 运行 — 系统仪表

### 从上到下

```
PROCESS      pid / uptime / JVM 内存 / 指令计数 / 最近指令
DEVICE       设备工具网格 (手电筒 / 截屏 / 音量…)
SKILLS       技能卡片 + 搜索 (学习闭环产物)
CRON         定时任务 + 创建
CHANNELS     4 通道状态 (壳 / 小组件 / AI 网关 / 通知)
MODEL        原生引擎 + AI 模型 (2 行, 可交互)
PERMISSIONS  横滚权限标签 (点击跳系统设置)
```

---

## 5. AI 团队

每个 AI 模型在设置里配置:

```
设置 ≡
├─ AI 团队
│   ├─ DeepSeek V4 Flash  [默认 · MOV 本体]
│   ├─ Claude Opus        [已配置]
│   └─ ＋ 添加模型
├─ 偏好 (语言 / MOV 人设)
├─ 权限管理
└─ 关于
```

**当前状态**: 全局只能配一个 AI (DeepSeek/Qwen/Ollama 四选一)。多模型管理待 v4。

---

## 6. 交互规范

| 模式 | 适用场景 | 行为 |
|------|---------|------|
| 点击 | 进入/打开 | 直接执行 |
| 长按 (500ms, 移动>10px 取消) | 删除/操作 | 高亮 → 底部黑条浮出 → 点击执行 |
| Sheet | 创建/编辑/确认 | 遮罩 + 底部面板滑入, ✕ 关闭 |
| Toast | 操作反馈 | 短暂弹出, 自动消失 |

**原则**: 每操作必有反馈。能创建就能删除。触屏优先, 拒绝 `prompt()`。

---

## 7. 文件能力

| 能力 | 指令 | 用途 |
|------|------|------|
| `file.write` | 写文件到房间 | AI 生成代码/文档 → 存入房间 |
| `file.read` | 读房间文件 | 预览、分析 |
| `file.delete` | 删房间文件 | 清理 |
| `file.mkdir` | 创建目录 | 组织文件 |
| `file.ls` | 列出文件 | 文件树 |

所有文件操作限制在 `/sdcard/mov/rooms/<id>/` 内, 有路径逃逸检查。

---

## 8. 已知缺陷 & 待修

| # | 问题 | 状态 |
|---|------|------|
| 1 | 文件上传不真正复制文件到房间 | 🔴 待修 (见 DESIGN_FILE_FIXES) |
| 2 | 文件预览在隐藏 DOM 里不可见 | 🔴 待修 |
| 3 | `_filesPath` 切换房间不重置 | 🔴 待修 |
| 4 | `doHelp` 未列出文件能力 | ⚠️ 待修 |
| 5 | 缺新建文件 UI 入口 | ⚠️ 待修 |
| 6 | 多模型管理 (全局只能配 1 个) | 🔵 待 v4 |
| 7 | 本地多用户 (L1) | 🔵 待 v4 |
| 8 | 看板 tab 未实现 | 🔵 待 v4 |
| 9 | 技能 tab 未合并到运行 | 🔵 待 v4 |
| 10 | Council fit 房间硬编码剧本未替换 | ⚠️ 已标记 |

---

## 9. 技术栈

```
平台: Android API 26+, targetSdk 36
语言: Java 11 (Android), HTML/CSS/JS (WebView 壳)
构建: Gradle 8.13, appcompat 1.6.1
测试: JUnit 4 (87 用例, 仅 IntentParser)
```

## 10. 文件清单

### Java (17 文件, ~3000 行)

| 文件 | 行 | 职责 |
|------|-----|------|
| `MOVActivity.java` | 700+ | WebView 壳 + 22 桥方法 |
| `CapabilityExecutor.java` | 779 | 34 个设备+文件能力 |
| `IntentParser.java` | 254 | 自然语言 → 指令 |
| `AiClient.java` | ~190 | OpenAI 兼容客户端 |
| `AiProviderConfig.java` | 131 | AI 配置持久化 |
| `CouncilClient.java` | 89 | 多角色议会讨论 |
| `CronManager.java` | ~170 | WorkManager 调度 |
| `MOVCronWorker.java` | 96 | Cron Worker |
| `SkillStore.java` | 111 | 技能 CRUD |
| `MOVSettingsActivity.java` | 211 | AI 设置页 |
| `MOVApplication.java` | 30 | 启动清理 |
| `MOVWidgetProvider.java` | 125 | 桌面小组件 |
| `MOVWidgetService.java` | 117 | 小组件数据源 |
| `ParsedCommand.java` | 47 | 数据类 |
| `CommandResult.java` | 25 | 数据类 |

### 前端 (11 文件, ~1766 行)

| 文件 | 行 | 职责 |
|------|-----|------|
| `hermes-shell.html` | ~155 | UI 骨架 |
| `css/shell.css` | ~345 | 设计系统 |
| `js/store.js` | 41 | 数据层 + 持久化 |
| `js/i18n.js` | ~140 | 中英双语字典 |
| `js/bridge.js` | 63 | MOVBridge 封装 |
| `js/render.js` | 140 | DOM 渲染 |
| `js/chat.js` | 233 | 消息路由 + 长按 + 删除 + 清空 |
| `js/council.js` | 103 | fit 房硬编码剧本 |
| `js/skills.js` | 49 | 技能列表 + 搜索 + 长按移除 |
| `js/files.js` | 100 | 文件树 + 预览 |
| `js/runtime.js` | ~130 | 运行页仪表 |
| `js/app.js` | ~190 | 事件绑定 + 初始化 |

---

## 11. 文档索引

| 文档 | 内容 |
|------|------|
| `CLAUDE.md` | 项目规则 & AI 协作文档 |
| `docs/HERMES_MASTER.md` | **本文档 — 项目全貌** |
| `docs/DESIGN_INTERACTION.md` | 交互系统详细设计 |
| `docs/DESIGN_ROOM_V3.md` | 房间 v3 设计 (文件仓库) |
| `docs/PLAN_ROOM_V3.md` | 房间 v3 实施计划 (已完成) |
| `docs/DESIGN_BOARD_V1.md` | 看板 v1 设计 (待实施) |
| `docs/DESIGN_FILE_FIXES.md` | 文件系统 5 个修复 (待实施) |
