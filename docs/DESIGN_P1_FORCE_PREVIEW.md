# DESIGN: P1 — 强制预览, 删除"自动执行"

版本: v1.0
日期: 2026-07-22
status: design-ready

---

## 问题

FIX2 设计了 AI 写文件的预览卡片——但只覆盖"人工触发写入"场景。

`DESIGN_MULTI_MODEL.md` 第 3 层保留了"自动执行"选项：Council 讨论完 → nextSteps 自动调 `CapabilityExecutor.execute()`，文件直接落盘，预览卡片被绕过。

同理：Cron 定时任务如果配置为"自动执行"，也是直接落盘。

**安全规则在左边，自动执行在右边，中间是空的。**

---

## 方案

**删除"自动执行"选项。** 所有 AI 产出（Council nextSteps、Cron、单个文件写入）——不管什么场景——必须走预览卡片。用户不点"保存"，不落盘。

### 具体改动

**1. 删除房间设置中的"自动执行/手动审批"开关。**

只有一种模式：AI 产出 → 预览卡片 → 用户确认 → 执行。

**2. Cron 任务的 action 白名单收窄。**

Cron 只能执行**安全、无副作用的查询类操作**：`battery.status`、`wifi.status`、`system.info`、`network.info`、`file.ls`、`help`、`clipboard.get`。

Cron **不能执行**：`file.write`、`notification.post`、`http.get`、`tts.speak`、`toast`——这些必须有人工确认。

Cron 如果配置了 `file.write` 步骤 → 运行时不执行，而是在聊天区推送一条消息："Cron「每日邮件摘要」想写入文件，请确认" + 预览卡片。用户下次打开 APP 时看到这条消息，点保存才落盘。

**3. Council 的 nextSteps 执行流程。**

```
Council 讨论结束 → nextSteps 提取
  → 对于 file.write 步骤: 聊天区插入预览卡片
  → 对于 notification.post: 聊天区插入确认卡片
  → 对于其他安全操作: 直接执行
  → 所有卡片等待用户逐条确认
  → 用户点"全部执行"或逐条点"保存"
```

---

## 影响

| 改动 | 文件 |
|------|------|
| 删除"自动执行"模式 | `DESIGN_MULTI_MODEL.md` 第 3 层相关描述 |
| Cron 白名单收窄 | `HermesCronWorker.java` ALLOWED_ACTIONS |
| Council 执行管线改为逐条确认 | `js/chat.js` runCouncil 回调 |
| 房间设置去开关 | `hermes-shell.html` + `js/app-room.js`（如果已有） |

---

## 验收

- [ ] Council 讨论结束后，每个 file.write 步骤出现预览卡片
- [ ] 用户不点"保存"，文件不落盘
- [ ] Cron 配置了 file.write 步骤 → 不自动执行，在聊天区推送确认消息
- [ ] 设置中不再有"自动执行/手动审批"开关
