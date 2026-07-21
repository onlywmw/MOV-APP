package com.hermes.android.council;

import com.hermes.android.ai.AiClient;
import com.hermes.android.ai.AiProviderConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * P1-5: Council 多角色讨论。
 * 同一 AI 后端, 3 个不同 system prompt 模拟 claude(产品)/gpt-5(技术)/gemini(数据) 角色。
 * 每个角色独立调用, 最后汇总。
 */
public class CouncilClient {

    private static final String[][] ROLES = {
        {"claude", "产品", "你是 Claude, 扮演产品经理角色。你关注用户体验、MVP 范围、产品优先级。回答简洁, 3 句话以内。用中文。"},
        {"gpt-5", "技术", "你是 GPT-5, 扮演技术架构师角色。你关注技术选型、可行性、交付周期。回答简洁, 3 句话以内。用中文。"},
        {"gemini", "数据", "你是 Gemini, 扮演数据分析/增长角色。你关注留存、指标、用户行为。回答简洁, 3 句话以内。用中文。"}
    };

    private final AiProviderConfig config;

    public CouncilClient(AiProviderConfig config) {
        this.config = config;
    }

    /**
     * 执行 Council 讨论, 返回 JSON:
     * {"ok":true, "messages":[{"who":"claude","role":"产品","content":"..."},...], "summary":"..."}
     */
    public String discuss(String topic) {
        if (!config.isAiEnabled() || !config.isConfigured()) {
            return "{\"ok\":false,\"error\":\"AI 未配置, 无法召开 Council。点右上角 ≡ 设置。\"}";
        }

        try {
            JSONArray messages = new JSONArray();
            List<AiClient.Message> emptyHistory = new ArrayList<>();

            for (String[] role : ROLES) {
                String who = role[0];
                String roleName = role[1];
                String systemPrompt = role[2];

                // 用临时 system prompt 构造，不写 SharedPreferences，无竞态
                AiClient client = new AiClient(config, systemPrompt);
                AiClient.AiResponse resp = client.chat(
                        "议题: " + topic + "\n请从你的角色角度给出观点。", emptyHistory);

                JSONObject msg = new JSONObject();
                msg.put("who", who);
                msg.put("role", roleName);
                if (resp.success) {
                    msg.put("content", resp.content);
                } else {
                    msg.put("content", "(调用失败: " + resp.content + ")");
                }
                messages.put(msg);
            }

            // 汇总: 用默认 prompt 让 AI 做收敛
            AiClient summarizer = new AiClient(config);
            StringBuilder councilContext = new StringBuilder("以下是三位专家对「" + topic + "」的讨论:\n\n");
            for (int i = 0; i < messages.length(); i++) {
                JSONObject m = messages.getJSONObject(i);
                councilContext.append(m.getString("who"))
                        .append("(").append(m.getString("role")).append("): ")
                        .append(m.getString("content")).append("\n\n");
            }
            councilContext.append("请用 3 句话总结共识和分歧, 给出推荐方案。");

            AiClient.AiResponse summaryResp = summarizer.chat(councilContext.toString(), emptyHistory);

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("messages", messages);
            result.put("summary", summaryResp.success ? summaryResp.content : "(汇总失败)");
            return result.toString();

        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"Council 调用异常: " + e.getMessage() + "\"}";
        }
    }
}
