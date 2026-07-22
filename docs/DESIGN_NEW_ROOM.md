# DESIGN: 新建房间流程 v2

版本: v2.0
日期: 2026-07-22
status: approved
关联: CONTRACT_ROOM.md（随之更新）

---

## 背景：现状的三个硬伤

1. **议会（council）模式没有创建入口。** 弹窗只有一个名字输入框，创建永远是 `mode:'single', members:['mov']`。唯一的 council 房间是 store.js 写死的演示数据。i18n.js 里 `sheet.council*` 一整套文案已存在但未接线。
2. **数据格式违反 CONTRACT_ROOM 约束 2。** 契约要求 `members:{human:[],ai:[modelId]}`，创建代码写的是旧数组格式 `['mov']`。
3. **`B.initRoom(rid,name,desc,members)` 桥接方法是死代码**，房间磁盘元数据（room.json）从未写入。

## 决策记录（与用户确认）

| 问题 | 决策 |
|------|------|
| 核心目标 | 打通议会模式：创建时选「单聊 / AI 团队」+ 选模型 |
| 成员可改性 | 创建后可改（房间操作 sheet 加「AI 成员」入口） |
| 单聊房模型 | 单聊也选模型，统一走 ModelRegistry，废弃房间层面对旧 AiProviderConfig 的依赖（desk 房除外） |
| 空模型兜底 | 引导条「去添加 →」跳运行页；允许跳过先创建 |
| 交互形态 | 单页底部 Sheet（方案 A），替代居中弹窗；CONTRACT_ROOM 约束 1 随之修订 |

## 交互流程

底部 sheet（复用 `.sheet-mask` / `.sheet` 体系与 `openSheetExclusive`）：

```
新建房间              ✕
────────────────────────
名字  [ 产品V2.0            ]   ← 空则默认「新项目」
模式  [ ● 单聊  |  AI 团队  ]   ← segmented，默认单聊
AI 成员
┌────────────────────────┐
│ ○ DeepSeek-V4   (默认)  │   ← 单聊=单选(○)，团队=多选(☑)
│ ○ GPT-5                │
│ ○ 本地 Ollama           │
└────────────────────────┘
── 空态 ──
ⓘ 还没配置模型，去添加 →        ← 点击 setTab('run')
[        创 建        ]
```

行为规则：

1. 模式切「AI 团队」→ 模型列表变多选；未选任何模型时创建按钮置灰。
2. 单聊默认勾选注册表默认模型；团队默认勾选默认模型，用户增删。
3. 创建 → `B.initRoomStorage(id)` → `enterRoom(id)`（顺序遵守 CONTRACT_ROOM 约束 5）。
4. seed 消息按模式区分：单聊沿用现有文案；团队用现成 key `sheet.councilFirst`。
5. 创建同时调用现存的 `B.initRoom(rid, name, '', members)` 落盘 room.json（激活死代码，desc 本期留空）。

## 创建后编辑成员

房间操作 sheet（长按 / ⋮）菜单新增「AI 成员」项，点开复用创建 sheet 的同一组件（模式 segmented + 模型勾选）：

- 模式始终由用户显式选择，不由勾选数反推；唯一例外：勾选数为 0 且模式为团队时，保存自动降级 `mode='single'`。
- 保存即生效：更新 `members` 与 `mode`；旧格式 `members:['mov']` 在保存时迁移为新格式。
- 保存后刷新房间副标题（`enterRoom` 的 roomSub 逻辑）与列表头像栈（`avstack`）。
- 编辑时若房间正在 council 讨论：`genCounter++` 使旧讨论回调失效（复用现有守卫）。

## 数据模型

```js
// 单聊
{ id, name, mode:'single',
  members:{human:[{who:'you',role:'owner'}], ai:['<modelId>']} }
// 团队
{ id, name, mode:'council',
  members:{human:[{who:'you',role:'owner'}], ai:['<modelId>','<modelId>']} }
```

- 旧格式兼容继续由 `roomAiMembers()`（store.js）承担，不改存储 key（`mov_rooms_v2` 不变）。

## 单聊房 AI 路由变更

- 非 desk 单聊房：`routeMessage()` 取 `roomAiMembers(room)[0]`，走新增的
  `B.aiChatWithModel(text, modelId, cbId)` → `BridgeAi.aiChatWithModel`
  （`new AiClient(modelRegistry.get(modelId).toModelConfig())`，~30 行 Java）。
- desk 房：维持现状走全局 AiProviderConfig（设备控制场景不动）。
- 房间未选模型或模型已被删除：回退到注册表默认模型；注册表为空 → 现有「AI 未配置」提示。

## 边界情况

| 场景 | 行为 |
|------|------|
| 模型列表为空 | 显示引导条；创建按钮可用；发消息走「AI 未配置」提示 |
| 团队只选 1 个模型 | 允许，CouncilClient 单成员自然退化 |
| 编辑成员减到 0 | 自动降级 `mode='single'` |
| 编辑时讨论进行中 | `genCounter++` 使旧回调失效 |
| 名字为空 | 默认「新项目」（同现状） |

## 改动文件清单

| 文件 | 改动 |
|------|------|
| `hermes-shell.html` | newRoom dialog → sheet；模型列表容器；操作 sheet 加「AI 成员」面板 |
| `js/app-room.js` | 重写创建逻辑 + 成员编辑逻辑（约 +120 行） |
| `js/chat.js` | `routeMessage` 单聊按房间模型路由（~10 行） |
| `js/bridge.js` | B 封装加 `aiChatWithModel`（~4 行） |
| `js/i18n.js` | 新增 key：模式名 / 成员 / 去添加 / 至少选一个 |
| `css/shell.css` | 尽量复用 V4.0 已有类：`.mopt`/`.mode-opts`（模式选项卡）、`.mpick`/`.mpick-wrap`（模型勾选行）、`.chip-btn`；仅补 segmented 等缺失样式（<30 行） |
| `BridgeAi.java` / `BridgeFactory.java` | 加 `aiChatWithModel`（~30 行） |
| `docs/CONTRACT_ROOM.md` | 约束 1 改为 sheet；TC-R01/R02 重写；新增成员编辑 TC |

## 测试与验收

- `./gradlew test`：Java 侧不回归。
- 手动验收按更新后的 CONTRACT_ROOM TC：创建单聊 / 创建团队 / 空模型引导 / 编辑成员（含降级）/ 杀进程重启持久化 / 单聊房按所选模型回复。

## 明确不做（YAGNI）

- 房间描述（desc）字段：本期不落 UI，仅通过 `B.initRoom` 传空串占位。
- 创建向导多步分页、房间模板套用：不引入。
- desk 房的模型选择：不动。
