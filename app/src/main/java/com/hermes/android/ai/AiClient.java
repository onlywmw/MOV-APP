package com.hermes.android.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal OpenAI-compatible chat client.
 * Works with OpenAI, DeepSeek, Qwen (compatible-mode), Ollama (/v1/chat/completions).
 */
public class AiClient {

    public static class Message {
        public final String role;
        public final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class AiResponse {
        public final boolean success;
        public final String content;

        public AiResponse(boolean success, String content) {
            this.success = success;
            this.content = content;
        }
    }

    private final AiProviderConfig config;

    /** 临时覆盖 system prompt（仅本次实例，不持久化）。
     *  用于 Council 等多角色场景，避免并发写 SharedPreferences。 */
    private String overrideSystemPrompt = null;

    public AiClient(AiProviderConfig config) {
        this.config = config;
    }

    /** 构造时指定临时 system prompt，不修改 SharedPreferences */
    public AiClient(AiProviderConfig config, String systemPrompt) {
        this.config = config;
        this.overrideSystemPrompt = systemPrompt;
    }

    public AiResponse chat(String userText) {
        return chat(userText, new ArrayList<>());
    }

    /**
     * Send user text with optional recent history (user/assistant messages).
     */
    public AiResponse chat(String userText, List<Message> history) {
        HttpURLConnection conn = null;
        try {
            String baseUrl = normalizeBaseUrl(config.getBaseUrl());
            URL url = new URL(baseUrl + "/chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            String apiKey = config.getApiKey();
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            }

            JSONObject body = new JSONObject();
            body.put("model", config.getModel());
            body.put("stream", false);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", overrideSystemPrompt != null
                            ? overrideSystemPrompt : config.getSystemPrompt()));

            if (history != null) {
                for (Message m : history) {
                    if (m == null || m.content == null) continue;
                    if (!"user".equals(m.role) && !"assistant".equals(m.role)) continue;
                    messages.put(new JSONObject()
                            .put("role", m.role)
                            .put("content", m.content));
                }
            }

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", userText));

            body.put("messages", messages);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }

            int code = conn.getResponseCode();
            String responseBody;
            if (code >= 200 && code < 300) {
                responseBody = readStream(conn);
            } else {
                String err = readErrorStream(conn);
                return new AiResponse(false, "AI 请求失败 (" + code + "): " + firstLine(err));
            }

            JSONObject json = new JSONObject(responseBody);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return new AiResponse(false, "AI 返回为空");
            }
            // OpenAI 格式: choices[0].message.content
            JSONObject choice = choices.optJSONObject(0);
            if (choice == null) return new AiResponse(false, "AI 返回格式错误");
            JSONObject msgObj = choice.optJSONObject("message");
            if (msgObj == null) return new AiResponse(false, "AI 返回缺少 message 字段");
            String content = msgObj.optString("content", "").trim();
            // 兼容 reasoning 模型（deepseek-reasoner 等）：content 可能为空但有 reasoning_content
            if (content.isEmpty()) {
                content = msgObj.optString("reasoning_content", "").trim();
            }
            if (content.isEmpty()) return new AiResponse(false, "AI 没有返回内容");
            return new AiResponse(true, content);
        } catch (java.net.SocketTimeoutException e) {
            return new AiResponse(false, "AI 请求超时，请检查网络或 API 地址");
        } catch (Exception e) {
            return new AiResponse(false, "AI 调用异常: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return "";
        String s = baseUrl.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String readStream(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String readErrorStream(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() == null) return "";
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String firstLine(String text) {
        if (text == null || text.trim().isEmpty()) return "无详细信息";
        int idx = text.indexOf('\n');
        if (idx > 0) return text.substring(0, idx).trim();
        return text.trim();
    }
}
