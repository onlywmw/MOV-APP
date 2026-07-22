package com.hermes.android;

import android.app.Application;

import com.hermes.android.ai.AiProviderConfig;

public class HermesApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 仅 debuggable 包开启 WebView 远程调试 (chrome://inspect / CDP), release 关闭
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            android.webkit.WebView.setWebContentsDebuggingEnabled(true);
        }
        MigrationManager.run(this);
        ensureDefaultsAndCleanup();
    }

    /**
     * 清理历史遗留的已弃用模型名。
     * getProvider() 默认返回 "deepseek"，无需显式写入。
     * 不再预置 API Key——用户需在 AI 设置中自行填写。
     */
    private void ensureDefaultsAndCleanup() {
        AiProviderConfig config = new AiProviderConfig(this);
        // 清理历史遗留的已弃用模型名（deepseek-chat / deepseek-reasoner 已于 7/24 弃用）
        String m = config.getModel();
        if ("deepseek-chat".equals(m) || "deepseek-reasoner".equals(m)) {
            config.setModel("");
        }
    }
}
