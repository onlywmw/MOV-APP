# SKILL: 技能定义

版本: v1.0
日期: 2026-07-22
status: draft

---

## 定义

技能 = 一个**可复用的 prompt 模板 + 触发词**。用户说触发词 → MOV 自动填入 prompt 模板 → 发给指定 AI 模型 → 返回结果。

不是快捷指令（那走 IntentParser）。不是 Cron（那走定时调度）。不是模板（那是文档格式）。

---

## 数据结构

```json
{
  "id": "proxy-test",
  "name": "代理节点连通性测试",
  "desc": "用本地内核测试代理节点真实连通性",
  "trigger": "/test-proxy",          // 用户在聊天里输入这个 → 触发
  "prompt": "请帮我测试以下代理节点...", // 发给 AI 的 prompt 模板
  "model": "deepseek_v4",            // 用哪个模型
  "source": "auto",                  // "auto"(自动生成) | "manual"(用户创建)
  "status": "stable",                // "stable" | "revising"
  "revisions": 0,
  "uses": 0,
  "lastUsed": 0
}
```

---

## 生命周期

```
用户说触发词 → IntentParser 匹配 → 路由到 SkillStore → 取 prompt 模板 → 发给指定 AI → 返回结果
                   ↑ 如果同时命中设备指令，设备指令优先
```

---

## 与相邻概念的边界

| 概念 | 是什么 | 边界 |
|------|--------|------|
| **指令** | IntentParser 关键词匹配，走 CapabilityExecutor | 不调用 AI，纯本地执行 |
| **技能** | 触发词 → prompt 模板 → AI 调用 | 调用 AI，不执行本地设备操作 |
| **Cron** | 定时触发，可触发指令或技能 | 是调度层，不是执行层 |
| **模板** | 文档格式，被人复制使用 | 不自动执行，不调用 AI |

---

## 未解决问题

1. 当前 SkillStore 只有 3 个种子技能（硬编码），没有创建/编辑 UI
2. "自动生成"技能的触发条件未定义——什么情况下 MOV 会自动沉淀技能？
3. 技能和 Council 的关系——技能可以用在议会讨论中吗？如果可以，多个模型怎么分配？
4. 技能需要独立 tab 还是合并到运行页——当前代码两者都存在，需收敛
