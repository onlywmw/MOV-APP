# DESIGN: 伪流式 Council — 端侧并发 + 先到先显

版本: v2.0
日期: 2026-07-22
status: design-ready
取代: v1.0 (云端编排方案)

---

## 不用云函数。手机端自己并发。

```
用户发议题 "如何提升首页加载速度"
  ↓
手机端开 3 个异步线程, 分别调 DeepSeek / Claude / Qwen 的 API
  ↓
Claude 先回来 (3.2s) → evalJs → 聊天区立刻出现 Claude 的回复  ← 用户看到第一条思考
DeepSeek 回来 (5.1s) → evalJs → 追加
Qwen 回来 (7.8s)   → evalJs → 追加
  ↓
全部收齐 → 默认模型汇总 (2s) → evalJs → 追加汇总卡片
```

**用户不用等。谁的先好就先看谁的。** 这是流式体验——不需要 SSE、不需要云函数、不需要改 API。

---

## 和现在的区别

| | 现在 | 改后 |
|------|------|------|
| 调用方式 | 并行, 但等全部收齐才返回 | 并行, 每个模型完成就推给 JS |
| 用户体验 | 空白 → 3 条一起出现 | Claude 先到先看 → DeepSeek 追加 → Qwen 追加 |
| 汇总 | 同一次返回 | 全部收齐后单独调一次 |
| 云函数 | 无 | 无 — 全在手机端 |
| Java 改动 | — | CouncilClient 拆为增量回调 |

---

## CouncilClient 改造

### 当前

```java
// 收集所有结果 → 汇总 → 一次返回 JSON
for (Future<ModelReply> f : futures) {
    replies.add(f.get(30, TimeUnit.SECONDS));
}
String summary = summarize(topic, replies);
return buildResult(replies, summary); // 一次 JSON 返回
```

### 改后

```java
public void discussAsync(String topic, List<String> modelIds, String context,
                          String callbackId) {
    
    List<ModelConfig> models = resolveAndValidate(modelIds);
    ExecutorService exec = Executors.newFixedThreadPool(3);
    List<Future<ModelReply>> futures = new ArrayList<>();
    
    // 发起并行调用
    for (ModelConfig mc : models) {
        futures.add(exec.submit(() -> {
            AiClient client = new AiClient(mc, buildRolePrompt(mc));
            AiResponse resp = client.chat(topic);
            return new ModelReply(mc.id, mc.name, mc.role, mc.color,
                                   resp.success ? resp.content : "调用失败",
                                   resp.success);
        }));
    }
    
    // 每个模型完成就回调 JS (谁先谁先显)
    for (Future<ModelReply> f : futures) {
        aiExecutor.execute(() -> {
            try {
                ModelReply reply = f.get(30, TimeUnit.SECONDS);
                JSONObject msg = replyToJson(reply);
                evalJs("window._councilReply('" + callbackId + "'," + msg + ")");
            } catch (Exception e) {
                // 单模型失败不影响其他
            }
        });
    }
    
    // 全部收齐后汇总
    aiExecutor.execute(() -> {
        try {
            // 等所有 future 完成
            List<ModelReply> replies = new ArrayList<>();
            for (Future<ModelReply> f : futures) {
                replies.add(f.get(30, TimeUnit.SECONDS));
            }
            
            // 汇总
            String summary = summarize(topic, replies);
            String nextSteps = extractNextSteps(summary);
            
            JSONObject result = new JSONObject();
            result.put("type", "summary");
            result.put("summary", summary);
            result.put("nextSteps", nextSteps);
            evalJs("window._councilReply('" + callbackId + "'," + result + ")");
            
        } catch (Exception e) {
            evalJs("window._councilReply('" + callbackId + 
                    "',{\"type\":\"error\",\"content\":\"" + e.getMessage() + "\"})");
        }
        exec.shutdown();
    });
}
```

---

## JS 侧改动

### 当前

```javascript
// chat.js runCouncil()
B.councilAsync(topic, function(resp) {
    // 一次性收到所有消息 + 汇总
    resp.messages.forEach(function(m) {
        push(id, mkMsg({t:'agent', who:m.who, role:m.role, h:esc(m.content)}));
    });
    push(id, mkMsg({t:'sys', h:'COUNCIL 汇总...'}));
    push(id, mkMsg({t:'agent', who:'hermes', h:esc(resp.summary)}));
});
```

### 改后

```javascript
// chat.js runCouncil()
setPhase(id, '讨论中');
var replyCount = 0;

window._councilReply = function(callbackId, data) {
    if (data.type === 'summary') {
        // 汇总
        setPhase(id, '收敛中');
        push(id, mkMsg({t:'sys', h:'COUNCIL 汇总'}));
        push(id, mkMsg({t:'agent', who:'hermes', h:esc(data.summary)}));
        setPhase(id, '待评审');
        return;
    }
    
    if (data.type === 'error') {
        push(id, mkMsg({t:'sys', h:'Council 调用失败: ' + esc(data.content)}));
        return;
    }
    
    // 单个模型回复 — 先到先显
    replyCount++;
    push(id, mkMsg({
        t:'agent',
        who: data.who,
        name: data.name,
        role: data.role,
        h: esc(data.content)
    }));
    ev('Council 收到 #' + replyCount + ' ' + data.name);
};
```

**关键变化**：不再等全部收齐。每个模型的回复作为独立消息推入聊天流。最后一条是汇总。

---

## 视觉体验

```
用户发议题
  ↓ 1s
[typing: Claude 正在思考...]        ← 显示等待状态
  ↓ 3.2s
[CL] Claude · 技术                   ← 第一个回复出现
建议从 CDN + SSR 入手解决...
  ↓ 5.1s
[DS] DeepSeek · 通用                 ← 第二个追加
建议优化图片懒加载...
  ↓ 7.8s
[QW] Qwen · 数据                     ← 第三个追加
从用户行为看, 首页跳出率...
  ↓ 9s
── COUNCIL 汇总 ──                   ← 汇总卡片
共识: 懒加载和 CDN 优先...
[批准并执行] [驳回再议]
```

**这就是流式体验。没写一行 SSE 代码。**

---

## 改动量

| 文件 | 改动 |
|------|------|
| `CouncilClient.java` | 新增 `discussAsync()` 方法 (~60行)。旧 `discuss()` 保留兼容 |
| `HermesActivity.java` | 桥方法 `councilAsync` 改为调 `discussAsync` |
| `js/chat.js` | `runCouncil()` 改为增量接收 (~40行) |
| `js/bridge.js` | `councilAsync` 签名不变 |

**零云函数。零服务器。全在手机端。改 ~100 行 Java + ~40 行 JS。**
