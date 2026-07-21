# REDESIGN: 重设计硬约束

版本: v1.0
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
