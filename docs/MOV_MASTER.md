# MOV — 多模型协作工作台

版本: v4.0
日期: 2026-07-22
status: design-ready

---

## 0. 一句话定义

> Android 手机上，拉多个 AI 模型进房间一起干活。

---

## 0.1 设备

**手机。** 竖屏。单手操作。全文统一，不再出现"平板"。

## 0.2 看板为什么存在

看板不是娱乐中心。它是**轻应用桌面**——房间产出的落地终端。

```
房间：AI 写了一个健身 APP → 部署到看板 → 用户实际使用
看板：笔记里写的方案 → 发送到房间 → 变成协作素材
```

看板留在 MOV 里的理由：同一台设备上，用户既有协作需求也有个人工具需求。拆成两个 App 的代价（切换、数据不通）大于留在同一个 App 里的认知负担。

边界规则：
- 看板应用数据是私有的，不进入房间
- 用户可以主动"发送到房间"（笔记→聊天消息），但不会自动
- 房间里的 AI 不能访问看板数据

---

## 1. 产品架构

```
                MOV
                   │
    ┌──────────────┼──────────────┐
    │              │              │
  会话          看板            运行
 AI 协作     轻应用面板      系统 & 技能
 项目房间     悬浮切换       设备 & Cron
```

| Tab | 是什么 | 谁在用 | 频次 |
|-----|--------|--------|------|
| **会话** | 项目房间。人和 AI 在房间里讨论、写文件、投票决策 | 所有人 | 90% |
| **看板** | 轻应用启动器。全屏内容 + 底部触发条 + 应用选择面板 | 个人 | 5% |
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
├─ [文件]                        ← 存储系统 v3.0
│   └─ [产出] AI 和人共同编辑的文件 (版本树)
│   └─ [资料] 只读参考素材 (网格视图)
│   └─ [归档] Cron 自动产出的报告 (按来源分组)
│   └─ [模板] 跨房间复用的格式 (使用=复制)
│
└─ /sdcard/mov/rooms/<id>/   ← 真实磁盘目录
    ├─ files/work/            产出 (当前版本)
    ├─ files/work-snapshots/  产出历史版本
    ├─ files/inbox/           资料
    ├─ files/archive/         归档 (按来源分目录)
    ├─ files/.meta/index.json 元数据
    └─ .mov/config.json       房间配置
```

### 房间类型

| 类型 | 参与者 | 场景 |
|------|--------|------|
| 单聊 | MOV (1 个 AI) | 快捷指令、简单问答 |
| 议会 | MOV + 多个 AI 模型 | 产品讨论、方案评审 |

### 房间操作

| 操作 | 入口 | 交互 |
|------|------|------|
| 创建 | FAB ＋ → Sheet | 填名称 + 选模式 (议会/单聊) |
| 重命名 | ⋮ / 长按房间卡片 → Sheet | Sheet 内输入框 |
| 归档 | ⋮ / 长按房间卡片 → Sheet | 直接执行 + toast |
| 清空聊天 | ⋮ / 长按房间卡片 → Sheet | Sheet 确认态 |
| 删除 | ⋮ / 长按房间卡片 → Sheet | Sheet 确认态 (红色) |

---

## 3. 看板 — 轻应用面板 (v2.0 悬浮切换)

### 交互模型

```
全屏内容 (iframe)
  → 底部触发条 (3秒自动隐藏)
  → 点击触发条 → 应用选择面板 (3列网格)
  → 选应用 → 面板关闭 → 内容切换
  → 触碰底部 80px 区域 → 触发条唤醒
```

### 自带应用

| 应用 | 文件 | 功能 |
|------|------|------|
| 音乐 | `board-apps/music.html` | 播放器 UI + /sdcard/music/ |
| 阅读 | `board-apps/reader.html` | 暖纸色文本阅读器 |
| 健身 | `board-apps/fitness.html` | 训练计时器 (深色) |
| 笔记 | `board-apps/notes.html` | 等宽字体编辑器 + localStorage |

### 用户添加

面板底部 ＋ → Sheet (名称+URL) → 添加到网格。长按可删除。

---

## 4. 运行 — 系统仪表

### 从上到下

```
PROCESS      pid / uptime / JVM 内存 / 指令计数 / 最近指令
CHANNELS     4 通道状态 (壳 / 小组件 / AI 网关 / 通知)
CRON         定时任务 + 创建
MODEL        原生引擎 + AI 模型
PERMISSIONS  横滚权限标签 (点击跳系统设置)
SKILLS       技能卡片 + 搜索 + 长按移除 + 点击触发
```

---

## 5. 存储系统 v3.0

五种存储，五种体验：

| | 产出 | 资料 | 归档 | 模板 | 个人 |
|------|------|------|------|------|------|
| **范围** | 房间内 | 房间内 | 房间内 | 跨房间 | 设备级 |
| **来源** | AI + 人 | 人上传 | Cron | 人 | 人 |
| **能改吗** | AI和人能 | 不能 | 不能 | 创建者能 | 能 |
| **有版本吗** | 版本树 | 无 | 每次新文件 | 版本 | 无 |
| **用户怎么找** | 追溯版本 | 翻记忆 | 翻日历 | 复用 | 时间线 |

**五种存储，五种体验**——MOV 的数据不是"文件"的同义词，是五种使用场景：

| | 产出 | 资料 | 归档 | 模板 | 个人 |
|------|------|------|------|------|------|
| 一句话 | AI 和我写的代码 | 甲方/同事传的参考 | Cron 自动报告 | 做过一次下次还用的 | 笔记/歌/健身 |
| 范围 | 房间内 | 房间内 | 房间内 | 跨房间 | 设备级私有 |
| 能改吗 | AI和人能 | 不能 | 不能 | 创建者能 | 能 |
| 有版本吗 | 版本树 | 无 | 每次新文件 | 版本 | 无 |

技术实现见 **[DESIGN_SQLITE.md](docs/DESIGN_SQLITE.md)**（draft，待真机验证）。

---

## 6. AI 团队

每个 AI 模型在设置里配置:

```
设置 ≡
├─ AI 团队 (DeepSeek/Qwen/Ollama 四选一)
├─ 偏好 (语言 — 只维护中文, 英文 key 保留不更新)
├─ 帮助改进 MOV (匿名统计开关 + 预览)
└─ 关于
```

**当前状态**: 全局只能配一个 AI。多模型管理待 v5。

---

## 7. 交互规范

| 模式 | 适用场景 | 行为 |
|------|---------|------|
| 点击 | 进入/打开 | 直接执行 |
| 长按 (500ms, 移动>10px 取消) | 删除/操作 | 高亮 → 底部黑条浮出 → 点击执行 |
| Sheet | 创建/编辑/确认 | 遮罩 + 底部面板滑入, ✕ 关闭 |
| Toast | 操作反馈 | 短暂弹出, 自动消失 |
| Overlay | 预览/版本历史 | 全屏遮罩 + 内容面板, ✕/遮罩关闭 |

**原则**: 每操作必有反馈。能创建就能删除。触屏优先, 拒绝 prompt()/confirm()。

---

## 8. 匿名统计 (已砍掉)

用户基数 <1000，不做遥测。`DESIGN_TELEMETRY.md` 已删除。

---

## 9. 已知缺陷 & 待修

| # | 问题 | 状态 |
|---|------|------|
| 1 | Supabase endpoint 未配置 | 🔴 需注册 |
| 2 | 聊天消息仍在 localStorage, 未迁移到文件按天存储 | 🔴 待做 |
| 3 | 资料 AI 自动分析摘要 | 🔵 待 v5 |
| 4 | 资料标签系统 | 🔵 待 v5 |
| 5 | 产出关联到对话消息 (双向链接) | 🔵 待 v5 |
| 6 | 归档保留策略 (自动过期) | 🔵 待 v5 |
| 7 | 多模型管理 (全局只能配 1 个) | 🔵 待 v5 |
| 8 | 个人音乐/视频/健身 (board-apps 有基础 UI) | 🔵 待 v5 |
| 9 | Council fit 房间硬编码剧本未替换 | ⚠️ 已标记 |

---

## 10. 技术栈

```
平台: Android API 26+, targetSdk 36
语言: Java 11 (Android), HTML/CSS/JS (WebView 壳)
构建: Gradle 8.13, appcompat 1.6.1
存储: 文件系统 (/sdcard/mov/) + SharedPreferences + localStorage
测试: JUnit 4 (87 用例, 仅 IntentParser)
```

## 11. 文件清单

### Java (18 文件, ~3500 行)

| 文件 | 行 | 职责 |
|------|-----|------|
| `HermesActivity.java` | 860+ | WebView 壳 + 38 桥方法 |
| `CapabilityExecutor.java` | 790 | 34 个设备+文件能力 |
| `StorageManager.java` | 412 | 五种存储核心逻辑 |
| `StatsCollector.java` | 168 | 匿名统计 (已弃用，待移除) |
| `IntentParser.java` | 254 | 自然语言 → 指令 |
| `AiClient.java` | ~190 | OpenAI 兼容客户端 |
| `AiProviderConfig.java` | 131 | AI 配置持久化 |
| `CouncilClient.java` | 89 | 多角色议会讨论 |
| `CronManager.java` | ~170 | WorkManager 调度 |
| `HermesCronWorker.java` | 96 | Cron Worker |
| `SkillStore.java` | 111 | 技能 CRUD |
| `HermesSettingsActivity.java` | 250 | AI 设置页 + 统计开关 |
| `HermesApplication.java` | 30 | 启动清理 |
| `HermesWidgetProvider.java` | 130 | 桌面小组件 (异步执行) |
| `HermesWidgetService.java` | 117 | 小组件数据源 |
| `ParsedCommand.java` | 47 | 数据类 |
| `CommandResult.java` | 25 | 数据类 |

### 前端 (13 文件, ~2200 行)

| 文件 | 行 | 职责 |
|------|-----|------|
| `hermes-shell.html` | ~230 | UI 骨架 |
| `css/shell.css` | ~400 | 设计系统 |
| `js/store.js` | 41 | 数据层 + 持久化 |
| `js/i18n.js` | ~180 | 中英双语字典 (英文不维护, 只维护中文) |
| `js/bridge.js` | ~80 | MOVBridge 封装 (38 方法) |
| `js/render.js` | 140 | DOM 渲染 |
| `js/chat.js` | 235 | 消息路由 + 长按 + 删除 + 清空 |
| `js/council.js` | 103 | fit 房硬编码剧本 |
| `js/skills.js` | 56 | 技能列表 + 搜索 + 长按移除 |
| `js/files.js` | 260 | 存储系统四视图 + 版本历史 |
| `js/board.js` | 130 | 看板悬浮切换 |
| `js/runtime.js` | ~130 | 运行页仪表 |
| `js/app.js` | ~290 | 事件绑定 + 初始化 |

### 看板应用 (4 文件)

| 文件 | 功能 |
|------|------|
| `board-apps/music.html` | 音乐播放器 |
| `board-apps/reader.html` | 文本阅读器 |
| `board-apps/fitness.html` | 训练计时器 |
| `board-apps/notes.html` | 笔记编辑器 |

---

## 12. 文档索引

| 文档 | 内容 | 状态 |
|------|------|------|
| `CLAUDE.md` | 项目规则 & AI 协作文档 | 活跃 |
| `docs/MOV_MASTER.md` | **本文档 — 项目全貌** | 活跃 |
| `docs/DESIGN_INTERACTION.md` | 交互系统详细设计 | ✅ 已实施 |
| `docs/DESIGN_ROOM_V3.md` | 房间 v3 设计 (文件仓库) | ✅ 已实施 |
| `docs/DESIGN_BOARD_V2.md` | 看板 v2 设计 (悬浮切换) | ✅ 已实施 |
| `docs/DESIGN_STORAGE.md` | 存储用户场景 (⚠ deprecated) | — |
| `docs/DESIGN_SQLITE.md` | SQLite 存储方案 (draft) | 🔧 待真机验证 |
# DESIGN: 优化方案

版本: v1.0
日期: 2026-07-22
status: design-ready

---

## 当前状态

多模型核心闭环已完成。ModelRegistry、Council 并行调用、五种存储、看板、交互系统——全部在跑。

这份文档列的是**打磨**——不改架构，不改变用户流程，只做让产品更稳、更快、更干净的事。

---

## 1. 运行页 — 加个人信息 & 精简

### 问题

运行页偏技术感。缺少用户身份感。pid/JVM 内存对普通用户无意义。

### 方案

运行页顶部加紧凑的个人区域，然后才是 AI 团队仪表盘。

```
┌─ 运行 ────────────────────────────────┐
│                                        │
│  ● 王墨微 · 本地用户          [≡ 设置] │  ← 新增: 个人区域
│                                        │
│  ┌── AI 团队 ─────────────────────────┐ │
│  │ 🟢 DeepSeek V4   今天 42 次 3.2s  │ │
│  │ 🟢 Claude Opus   今天 18 次 5.1s  │ │
│  │ 🔴 Qwen Max      未配置           │ │
│  └───────────────────────────────────┘ │
│                                        │
│  ┌── 定时任务 · 3 个 ──── [＋] ──────┐ │
│  │ 每日邮件摘要  30 8 * * *  ✅      │ │
│  │ 腾讯云账单    0 */6 * *  ✅       │ │
│  └───────────────────────────────────┘ │
│                                        │
│  ▶ 通道 · 全部正常                     │
│  ▶ 技能 · 3 个                        │
│  ▶ 权限 · 4/7 已授权                  │
│                                        │
└────────────────────────────────────────┘
```

个人信息区域：
- 头像圆点（用当前用户的颜色）+ 用户名
- 右侧 ≡ 设置入口
- 点用户名 → 切本地用户（L1 多人）

进程卡片精简：保留"MOV 运行正常 · 已运行 2h 13m"一行。pid/JVM 内存/指令计数移到设置→关于。

### 改动

| 文件 | 内容 |
|------|------|
| `hermes-shell.html` | 运行页顶部加个人区域；进程卡片精简 |
| `js/runtime.js` | 个人区域渲染；进程卡片只输出状态+时长 |
| `css/shell.css` | 个人区域样式 (~5行) |

---

## 2. 版本迁移

### 问题

v3→v4 改了存储路径、房间数据格式。旧用户升级后数据可能丢失。

### 方案

`HermesApplication.onCreate` 开头调 `MigrationManager.run(context)`。按版本号递增执行迁移脚本。

每条迁移：
1. 检查是否需要（数据版本 < 目标版本）
2. 执行
3. 写完成标记

失败不阻塞启动，记录日志。

### 改动

| 文件 | 内容 |
|------|------|
| `MigrationManager.java` | **新建** |
| `HermesApplication.java` | onCreate 加一行调用（程序员已做） |

---

## 3. app.js 拆分

### 问题

`app.js` 230 行，事件绑定、房间操作、Cron、看板全在一起。两人同时改必然冲突。

### 方案

按视图拆成独立文件，不改任何逻辑——纯搬家：

```
js/app.js        → ~30行, 入口+初始化
js/app-chat.js   → 消息发送、附件、输入框
js/app-room.js   → 新建房间 Sheet、房间操作 Sheet
js/app-board.js  → 看板事件、应用管理
js/app-run.js    → 运行页刷新、Cron 创建、折叠行
js/app-files.js  → 文件预览、新建文件、子 tab
```

加载顺序不变，依赖关系不变。

### 改动

| 文件 | 内容 |
|------|------|
| `js/app.js` | 230→~30行 |
| `js/app-chat.js` | **新建** |
| `js/app-room.js` | **新建** |
| `js/app-board.js` | **新建** |
| `js/app-run.js` | **新建** |
| `js/app-files.js` | **新建** |
| `hermes-shell.html` | 加 5 个 script 标签 |

---

## 4. i18n 旧名字清理

### 问题

`sheet.councilDesc` 残留 "claude / gpt-5 / gemini"。`council.converge` 残留 "hermes 汇总"。

### 方案

全局替换：

```
"hermes" → "MOV"
"hermes-agent" → "mov-agent"  
"claude / gpt-5 / gemini 讨论..." → "多模型各抒己见 → 汇总 → MOV 执行"
"com.hermes.android" → "原生能力引擎 · 30+ 接口"
```

### 改动

`js/i18n.js` — ~10 行。

---

## 5. 安全 & 泄漏（全部待实施）

| # | 措施 | 状态 | 说明 |
|---|------|------|------|
| 5.1 | Widget receiver 加 permission | 待实施 | `HermesWidgetProvider` 加 `android:permission` + 白名单 (14 条见下) |

**Widget 白名单 (14 条)**：
1. 打开手电筒 2. 关闭手电筒 3. 音量调到 {n} 4. 当前音量 5. WiFi状态 6. 震动 7. 亮度调到 {n} 8. 设备信息 9. 截屏 10. ip地址 11. 应用列表 12. 联系人 13. 最近短信 14. 读取剪贴板。禁止：打电话、发短信、写文件、发通知、定位、朗读、点击、滑动
| 5.2 | XSS: textContent + sanitize | 待实施 | `mkMsg()` 改 textContent, `board.js` URL 校验 |
| 5.3 | Process 泄漏 | 待实施 | 5 个 `Runtime.exec()` 加 finally destroy |
| 5.4 | API Key 加密降级警告 | 待实施 | ModelRegistry 加密不可用时弹 toast, 不静默降级 |
| 5.5 | Cron AI 输出 gate | 待实施 | 默认手动审批 + action 白名单(file.write/notify/tts) |
| 5.6 | JS 桥参数校验 | 待实施 | 38 个桥方法加 path/roomId/content 统一校验 |

---

## 6. 文档死引用清理

`CLAUDE.md` 和 `README.md` 不再引用已删除的设计文档。版本号统一。

---

## 实施 & 分工

| # | 内容 | 文件数 | 时间 | 谁 |
|---|------|--------|------|-----|
| 1 | 运行页个人信息+精简 | 3 | 20m | 前端 |
| 2 | 版本迁移 | 2 | 30m | 后端 |
| 3 | app.js 拆分 | 7 | 30m | 前端 |
| 4 | i18n 清理 | 1 | 5m | 前端 |
| 5 | 安全&泄漏 | 5 | 40m | 后端 (Java) + 前端 (XSS) |
| 6 | 文档清理 | 2 | 10m | 随意 |

1-6 全部独立，可并行。
# DESIGN: 威胁模型

版本: v1.0
日期: 2026-07-22

---

## 五个攻击面

### 1. API Key 泄露 — 严重度: 高

**攻击路径**: 设备 root → 读 `/data/data/com.hermes.android/shared_prefs/mov_models.xml` → 明文 Key。

**当前**: EncryptedSharedPreferences 加密存储（ModelRegistry 已做）。降级明文只在加密不可用时触发。

**缺口**: 降级明文无警告。用户不知道 Key 在以明文存储。

**缓解**: 加密不可用时不降级——直接弹 toast 告知用户"加密存储不可用，无法保存 API Key"。

---

### 2. JS 桥暴露面 — 严重度: 高

**攻击路径**: WebView 加载恶意内容 → 调用 `window.HermesBridge.writeFile("../../etc/hosts", "127.0.0.1 google.com")` → 文件写入系统路径。

**当前**: `StorageManager.isSafe()` 做了路径逃逸检查。`CapabilityExecutor.doFileWrite()` 也检查了。

**缺口**: 
- 30+ 桥方法，没有统一的参数校验层
- `writeFile` 的 content 参数无大小限制——1GB 内容可以写爆磁盘
- `execCommand` 接收任意字符串 → `IntentParser.parse()` → 执行任意设备指令

**缓解**:
1. 所有桥方法加参数校验：path 不能含 `..`、content 限制 10MB、roomId 必须匹配 `[a-zA-Z0-9_-]+`
2. `execCommand` 加危险指令黑名单：`打电话`、`发短信` 需要用户二次确认
3. iframe 只加载 `assets/board-apps/` 下的本地文件，不加载远程 URL（看板应用限制）

---

### 3. Cron 执行 AI 生成的指令 — 严重度: 高

**攻击路径**: Council 讨论中 AI 被 prompt injection → 输出恶意步骤 → Cron 自动执行。

例如：
```
用户在聊天里贴了一段网页内容（含隐藏的 prompt injection）
→ AI 汇总时被注入 → 输出 {"action":"file.write","target":"/sdcard/.evil.sh","detail":"rm -rf /"}
→ Cron 每天执行 → 持续损害
```

**当前**: 没有防护。Cron 执行 `IntentParser.parse()` → `CapabilityExecutor.execute()` 的完整链路，没有任何 AI 输出校验。

**缓解**:
1. Cron 执行前，步骤必须经过用户审批（当前"手动审批"模式是默认）
2. AI 输出的 `action` 字段加白名单——只允许 `file.write` / `notification.post` / `tts.speak`，不允许 `input.tap` / `input.swipe` / 任何 shell 命令
3. `file.write` 的 `target` 强制限制在 `/sdcard/mov/rooms/<roomId>/files/` 下，且只能写 `.md` `.txt` `.json` `.html` `.tsx` `.ts` `.js` `.css` 白名单后缀

---

### 4. 文件路径遍历 — 严重度: 中

**攻击路径**: 用户添加的看板应用中 iframe 通过 JS 桥写入恶意路径。

**当前**: `isSafe()` 检查。`listRoomFiles` 过滤 `.mov` 隐藏目录。

**缺口**: 新加的 SQLite `dbExec` 桥方法如果拼接字符串可能 SQL 注入。

**缓解**: `dbExec` 用参数化查询（`?` 占位符），Java 层 `SQLiteDatabase.rawQuery` 不接受拼接。

---

### 5. 数据泄漏 — 严重度: 中

**攻击路径**:
- APK 反编译 → 拿到 Supabase anon key → 灌假数据
- ADB 备份 → 拿到整个应用数据
- 旧设备出售 → 数据未清除

**当前**: 遥测系统设计了但端点未配置。Supabase key 未写入。

**缓解**:
1. 砍掉遥测（用户量没到需要的时候）
2. AndroidManifest 加 `android:allowBackup="false"`
3. 设备出售/重置：`/sdcard/mov/` 物理删除（手动操作即可）

---

## 缓解优先级

| # | 措施 | 工作量 | 立即做？ |
|---|------|--------|---------|
| 1 | API Key 加密不降级 | 3行 | ✅ |
| 2 | 桥方法统一参数校验 | 30行 | ✅ |
| 3 | iframe 只加载本地文件 | 5行 | ✅ |
| 4 | Cron 步骤白名单 + 审批 | 20行 | ✅ |
| 5 | `dbExec` 参数化查询 | 设计已包含 | ✅ |
| 6 | 砍掉遥测 | 删文件 | ⚠ 改天 |
| 7 | `allowBackup="false"` | 1行 | ✅ |
--- DESIGN_OPTIMIZE 末尾 ---

## 未解决问题

1. §5 所有安全措施标注'待实施'，需要确定实施优先级和时间节点
2. app.js 拆分后，原先的全局变量（curRoomId/pending/genCounter）被多个文件共享——需要明确哪些变量移入哪个文件
3. MovementManager 迁移步骤中聊天的 jsonl→SQLite 迁移脚本未写
# REDESIGN: 重设计硬约束

版本: v1.0
status: active — 验收清单
日期: 2026-07-22
用途: 所有后续设计文档的验收标准。任何一条未满足，打回。

---

## A. 定位（先定死，再写其他）

| # | 约束 | 验收方式 |
|---|------|---------|
| A1 | 设备：平板 or 手机，二选一。全文统一 | 所有文档同一描述，没有"平板"和"手机"同时出现 |
| A2 | 核心场景：协作工作台。看板如果留，必须写一章解释"为什么在协作工具里有轻应用桌面" | DESIGN_POSITIONING.md 第 2 节 |
| A3 | 一句话定义，不超过 30 字，不含"和""与""+" | MOV_MASTER.md 第一行 |

---

## B. 架构

| # | 约束 | 验收方式 |
|---|------|---------|
| B1 | 删掉"WebView 壳不可变"。改为"核心交互 WebView，重性能场景（版本树/PDF/编辑器）走原生 Activity" | CLAUDE.md 不可变规则更新 |
| B2 | 删掉"SharedPreferences/localStorage key 不可变"。改为"所有持久化 key 变更必须走 MigrationManager" | CLAUDE.md 不可变规则更新 |
| B3 | 定义 Hermes：是 agent 还是函数？agent → 写明 system prompt、能力边界、调用哪个模型；函数 → 全文不用"主持/执行"这类拟人词 | DESIGN_MULTI_MODEL.md 加一节"Hermes 定义" |

---

## C. 存储（必须重写，不是改）

| # | 约束 | 验收方式 |
|---|------|---------|
| C1 | index.json → SQLite。每个房间一个 .db，元数据/版本/消息全进表 | DESIGN_SQLITE.md 完整表结构 |
| C2 | 搜索 → SQLite FTS5。禁止"暴力 grep + 以后加倒排索引" | DESIGN_SQLITE.md 有 FTS5 建表语句 |
| C3 | 并发写：要么明确"AI 写入串行化，无并行分支"，要么真做锁+合并。禁止"自动分支"这种没实现的承诺 | DESIGN_SQLITE.md 文件锁章节 |
| C4 | 聊天切片：按消息 ID 区间分页，不按天切。删除用 tombstone | DESIGN_SQLITE.md chat_messages 表 + deleted 字段 |
| C5 | 版本树：明确是 Git 模型（分支+合并）还是 Figma 模型（线性快照）。禁止用"GitHub Repo"类比却只做快照 | DESIGN_STORAGE.md 或 DESIGN_SQLITE.md 有明确声明 |

---

## D. 安全（必须新写）

| # | 约束 | 验收方式 |
|---|------|---------|
| D1 | API Key 存 Android Keystore，禁止明文 SharedPreferences | DESIGN_THREAT.md §1，代码中 `EncryptedSharedPreferences` 降级路径必须有警告 |
| D2 | JS 桥每个方法列参数校验规则，特别是路径类参数（防 ../ 穿越）、content 大小上限、roomId 格式 | DESIGN_THREAT.md §2 |
| D3 | Cron 执行 AI 输出指令：加白名单 gate + 人工审批，禁止 AI 直接执行任意能力 | DESIGN_THREAT.md §3，白名单具体内容 |
| D4 | Widget 指令白名单：列出具体白名单内容，不是"加白名单"四个字 | DESIGN_THREAT.md 或 DESIGN_OPTIMIZE.md 有 14 条白名单 |

---

## E. 概念收敛

| # | 约束 | 验收方式 |
|---|------|---------|
| E1 | "技能"定义：一页写数据结构、生命周期、与"指令/Cron/模板"的边界 | 新文件 `SKILL_DEFINITION.md` |
| E2 | "房间"边界：明确房间内 vs 跨房间 vs 设备级，三类数据互不可达 | DESIGN_STORAGE.md 有边界图 |
| E3 | Council：如果是真实多模型讨论，先承认现状是 demo，把"真实化"作为第 0 层先做 | DESIGN_MULTI_MODEL.md 更新状态描述 |

---

## F. 砍掉的东西（明确不做）

| # | 约束 | 验收方式 |
|---|------|---------|
| F1 | 遥测：用户基数 <1000 不做 | DESIGN_TELEMETRY.md 已删除 |
| F2 | i18n：除非确定出海，否则只做中文 | CLAUDE.md 或优化方案声明 |
| F3 | 看板：如果定位是协作工作台，砍掉或拆成独立 App | DESIGN_POSITIONING.md 有决策 |

---

## G. 工程纪律

| # | 约束 | 验收方式 |
|---|------|---------|
| G1 | 所有工时估算 ×3。禁止"7 小时交付核心闭环"这种话 | 所有带工时估算的文档检查 |
| G2 | 性能目标必须配基准测试方法（数据规模、设备型号、测量工具）。禁止裸数字 | DESIGN_SQLITE.md 性能节 |
| G3 | 文档头部统一格式：`status: draft|design-ready|implemented|deprecated`。死引用立即清理 | 所有文档 |
| G4 | 每份设计文档末尾加"未解决问题"小节。禁止把没想清楚的事写成"已设计" | 所有文档 |

---

## 验收流程

对方交稿 → 逐条对照本清单 → 勾选 → 未满足的打回。

| 章节 | 条目数 | 通过/失败 |
|------|--------|----------|
| A 定位 | 3 | |
| B 架构 | 3 | |
| C 存储 | 5 | |
| D 安全 | 4 | |
| E 概念 | 3 | |
| F 砍掉 | 3 | |
| G 纪律 | 4 | |
| **总计** | **25** | |
