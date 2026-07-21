package com.hermes.android.model;

import org.json.JSONObject;

/**
 * 多模型配置数据类。
 * 每个模型一个实例，存储在 ModelRegistry (SharedPreferences JSON)。
 */
public class ModelConfig {

    public String id = "";
    public String name = "";
    public String provider = "deepseek";  // deepseek | openai | qwen | ollama | custom
    public String baseUrl = "";           // 空走 provider 默认
    public String apiKey = "";
    public String model = "";             // 空走 provider 默认
    public String systemPrompt = "";      // 可覆盖
    public String role = "通用";          // 通用 | 产品 | 技术 | 数据 | 自定义
    public String color = "#D97706";      // 头像色
    public boolean enabled = true;
    public boolean isDefault = false;

    public ModelConfig() {}

    /** 有效 baseUrl: 用户填了就用，否则按 provider 默认 */
    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.trim().isEmpty()) return baseUrl.trim();
        switch (provider) {
            case "deepseek": return "https://api.deepseek.com/v1";
            case "openai":   return "https://api.openai.com/v1";
            case "qwen":     return "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "ollama":   return "http://192.168.1.100:11434/v1";
            default:         return "https://api.deepseek.com/v1";
        }
    }

    /** 有效模型名: 用户填了就用，否则按 provider 默认 */
    public String getEffectiveModel() {
        if (model != null && !model.trim().isEmpty()) return model.trim();
        switch (provider) {
            case "deepseek": return "deepseek-chat";
            case "openai":   return "gpt-4o";
            case "qwen":     return "qwen-max";
            case "ollama":   return "llama3";
            default:         return "deepseek-chat";
        }
    }

    /** Provider 显示名 */
    public String getProviderDisplayName() {
        switch (provider) {
            case "deepseek": return "DeepSeek";
            case "openai":   return "OpenAI";
            case "qwen":     return "Qwen";
            case "ollama":   return "Ollama";
            default:         return provider;
        }
    }

    /** 是否已配置 (有 key 或是 ollama) */
    public boolean isConfigured() {
        return "ollama".equals(provider) || (apiKey != null && !apiKey.trim().isEmpty());
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("id", id)
                    .put("name", name)
                    .put("provider", provider)
                    .put("baseUrl", baseUrl)
                    .put("apiKey", apiKey)
                    .put("model", model)
                    .put("systemPrompt", systemPrompt)
                    .put("role", role)
                    .put("color", color)
                    .put("enabled", enabled)
                    .put("isDefault", isDefault);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static ModelConfig fromJson(JSONObject o) {
        ModelConfig mc = new ModelConfig();
        mc.id = o.optString("id", "");
        mc.name = o.optString("name", "");
        mc.provider = o.optString("provider", "deepseek");
        mc.baseUrl = o.optString("baseUrl", "");
        mc.apiKey = o.optString("apiKey", "");
        mc.model = o.optString("model", "");
        mc.systemPrompt = o.optString("systemPrompt", "");
        mc.role = o.optString("role", "通用");
        mc.color = o.optString("color", "#D97706");
        mc.enabled = o.optBoolean("enabled", true);
        mc.isDefault = o.optBoolean("isDefault", false);
        return mc;
    }
}
