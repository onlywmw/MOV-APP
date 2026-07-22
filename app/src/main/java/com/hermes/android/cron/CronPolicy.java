package com.hermes.android.cron;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * P2: Cron 白名单策略 — 纯 Java, 可单测。
 * 只允许查询类/轻量设备操作; 禁止 input/shell/call/screen/app/contacts/sms/location。
 * 白名单与 IntentParser 可产出的 capability 对齐 (CONTRACT_SECURITY 约束3)。
 */
public final class CronPolicy {

    /** Cron 允许的 action 白名单 */
    public static final Set<String> ALLOWED_ACTIONS = new HashSet<>(Arrays.asList(
        "help", "torch.on", "torch.off",
        "battery.status", "system.info",
        "brightness.get", "brightness.set",
        "volume.get", "volume.set",
        "wifi.status", "vibrate",
        "tts.speak", "toast", "notification.post",
        "clipboard.get", "clipboard.set",
        "network.info", "process.list",
        "file.ls"
    ));

    private CronPolicy() {}

    public static boolean isAllowed(String capability) {
        return capability != null && ALLOWED_ACTIONS.contains(capability);
    }
}
