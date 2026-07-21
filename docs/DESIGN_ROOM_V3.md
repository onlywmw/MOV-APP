# DESIGN: MOV 房间 v3.0 — 项目仓库模型

版本: v1.0
日期: 2026-07-21
状态: 📐 design-ready

---

## 核心定义

> **房间 = 聊天 + 文件仓库。人和 AI 共同在一个项目库里协作，对话围绕文件展开。**

参考模型: GitHub Repo — Issues 里有讨论，Code 里有文件，二者围绕同一个项目。

---

## 1. 产品模型

```
一个房间 (Room)
  = 一个项目仓库
  = 讨论 (Discussion) + 文件 (Files)
  = 人类成员 + AI 成员 + MOV 主持

磁盘:
  /sdcard/mov/rooms/<room-id>/
    README.md          ← 房间概要, 自动生成
    src/               ← AI 生成或人上传的代码
    docs/              ← 文档
    assets/            ← 图片/附件
    .hermes/           ← 房间元数据 (不显示在文件树)
      config.json      ← 成员列表 / 协作模式
```

---

## 2. 房间 UI

### 2.1 顶栏

```
┌──────────────────────────────────────────┐
│ ← 产品 V2.0                    [⋮]      │
│ 你 · 张三 · DeepSeek · Claude · 讨论中   │
│ ┌──────┬──────┐                          │
│ │ 讨论 │ 文件 │                          │  ← 房间内子 tab
│ └──────┴──────┘                          │
└──────────────────────────────────────────┘
```

- 返回按钮 (←) 回到房间列表
- 房间名 + 成员头像行 (人类用名字缩写圆点, AI 用首字母+颜色)
- 两个子 tab: 讨论 / 文件
- 右上角 ⋮ → 房间操作 sheet

### 2.2 讨论 tab (= 现在的聊天区, 不变)

```
┌─ 讨论 ─────────────────────────────┐
│                                     │
│ [你] 首页需要减负                   │
│ [DS] 同意。我先出一版               │
│ [DS] ⬆ src/HomeV2.tsx              │  ← 文件引用, 可点击
│     │ 已提交到项目文件              │
│ ┌─────────────────────────────┐     │
│ │ src/HomeV2.tsx · 2.1KB      │     │  ← 内联文件卡片
│ │ DeepSeek · 刚刚              │     │
│ │ [查看] [下载]                │     │
│ └─────────────────────────────┘     │
│ [张] 用懒加载, 别全量渲染           │
│ [CL] 同意。我 review 了结构没问题    │
│ ── HERMES 小结 ──                   │
│ 结论: 首页减负, DeepSeek 已提交初版  │
│                                     │
│ ┌─────────────────────────────┐     │
│ │ 输入...                      │  ↑  │
│ │ [+]                          │     │
│ └─────────────────────────────┘     │
└─────────────────────────────────────┘
```

关键: AI 产出文件时, 消息不再只是文本——而是带文件引用的卡片。

### 2.3 文件 tab (新增)

```
┌─ 文件 ─────────────────────────────┐
│ [目录: /src ▼]              [＋]   │  ← 路径导航 + 添加按钮
│                                     │
│ 📁 src/                            │
│    📄 HomeV2.tsx     2.1KB  Claude │  ← 文件名 · 大小 · 提交者
│    📄 utils.ts       0.8KB  DeepSeek│
│ 📁 docs/                           │
│    📄 PRD-v2.md      3.2KB  你     │
│    📄 notes.md       1.5KB  张三   │
│ 📁 assets/                         │
│    🖼 home-mockup.png 428KB 张三   │
│    🖼 logo-draft.png   92KB  DeepSeek│
│                                     │
│ 4 个文件 · 共 7.6KB                │
└─────────────────────────────────────┘
```

- 文件树: 目录折叠/展开, 文件名 + 大小 + 提交者 + 时间
- 点击文件 → 预览 (文本/Markdown 直接渲染, 图片显示缩略图)
- 长按 → 操作菜单 (打开/下载/重命名/删除)
- 右上角 ＋ → 新建文件 (AI 生成) 或上传 (从设备选择)
- 路径栏支持快速跳转目录

---

## 3. 新增操作

### 3.1 讨论中引用文件

当 AI 执行 `file.write` 时:
1. `CapabilityExecutor.doFileWrite(roomId, path, content)` 写入磁盘
2. `MOVBridge` 回调 JS: `_fileWritten(roomId, path, size, author)`
3. JS 在聊天区插入文件卡片消息 + 自动持久化到 msgData
4. 文件 tab 的树刷新

### 3.2 文件操作

| 操作 | 入口 | 行为 | 反馈 |
|------|------|------|------|
| 查看 | 点击文件名 | 文本: 弹窗或新 view 显示内容; 图片: 全屏查看 | — |
| 下载/导出 | 长按 → "导出" | 复制到 `/sdcard/Download/` | toast "已导出到下载目录" |
| 重命名 | 长按 → "重命名" | sheet 输入框 → 确认 | toast + 树刷新 |
| 删除 | 长按 → "删除" | sheet 确认 → `file.delete` | toast "已删除" |
| 新建文件 | 右上角 ＋ | sheet: 输入文件名 + 内容 | toast + 树刷新 + 讨论区通知 |
| 上传 | 右上角 ＋ → "从设备选择" | `B.pickFile` → 复制到房间目录 | toast + 树刷新 |

### 3.3 新建房间时的文件初始化

创建房间时自动生成:
```
<room-id>/
  README.md    ← "项目: <房间名>\n描述: <房间描述>\n成员: ..."
  .hermes/
    config.json ← {name, members, mode, created, ...}
```

---

## 4. 需要改的 Java 层

| 改动 | 文件 | 说明 |
|------|------|------|
| `doFileWrite` | `CapabilityExecutor.java` | 新增能力: `file.write` — 写内容到文件 |
| `doFileRead` | `CapabilityExecutor.java` | 新增能力: `file.read` — 读文件内容 |
| `doFileDelete` | `CapabilityExecutor.java` | 新增能力: `file.delete` — 删文件 |
| `doFileMkdir` | `CapabilityExecutor.java` | 新增能力: `file.mkdir` — 创建目录 |
| `initRoomDir` | `MOVActivity.java` 或新建 `RoomManager.java` | 创建房间时在磁盘建目录 + README |
| 桥方法 | `MOVActivity.java` MOVBridge | `writeFile(roomId, path, content)`, `readFile(roomId, path)`, `deleteFile(roomId, path)`, `listRoomFiles(roomId, path)` |

### file.write 能力定义

```java
// 添加到 IntentParser
"写文件", "创建文件", "写入到src/xxx.tsx 内容..."

// 添加到 CapabilityExecutor
case "file.write":
  return doFileWrite(ctx, cmd.getStringArg("roomId", ""),
                     cmd.getStringArg("path", ""),
                     cmd.getStringArg("content", ""));

// 路径规范: 所有房间文件路径以 roomId 为根
// 实际磁盘: /sdcard/mov/rooms/<roomId>/<path>
```

---

## 5. 需要改的前端层

| 改动 | 文件 | 说明 |
|------|------|------|
| 房间内子 tab | `hermes-shell.html` | `view-room` 内嵌 `[讨论] [文件]` 双 tab |
| 文件树视图 | `hermes-shell.html` + 新 `js/files.js` | 文件 tab HTML + JS 渲染 |
| 文件卡片消息 | `js/render.js` | 新增 `mkFileCard()` — 讨论区文件引用卡片 |
| 房间数据模型 | `js/store.js` | `ROOM` 加 `files` 字段 |
| 桥方法 | `js/bridge.js` | 新增 `writeFile` / `readFile` / `listRoomFiles` / `deleteFile` |
| 新建房间 | `js/app.js` | 创建后初始化房间目录 |

---

## 6. 建房间流程 (更新)

```
用户点击 FAB → 新建 sheet
  → 填项目名 + 描述
  → 选 AI 成员 (多选)
  → 选人类成员 (多选, L1 本地用户)
  → 点击"创建房间"
    → JS: ROOMS.push(room)
    → B.createRoom(id, name, members) ← 新桥方法
      → Java: 在 /sdcard/mov/rooms/<id>/ 下创建目录
        → 写入 README.md
        → 写入 .hermes/config.json
    → 进入房间, 显示讨论 tab
```

---

## 7. 小结: 改前 vs 改后

```
改前                              改后
────────────────────────────────────────────
房间 = 聊天记录                    房间 = 文件仓库 + 讨论
AI 输出 = 纯文本                   AI 输出 = 文件 (代码/文档/图片)
"交付物" = 假卡片                  "交付物" = 磁盘里真实的文件
聊天和文件无关                     聊天里引用文件, 文件有提交记录
```

这个架构一旦建立, MOV 就从"带 AI 的聊天工具"变成了"AI 参与的项目仓库"。聊天、Council、Cron 产出的东西都有地方存放——在房间的文件库里。
