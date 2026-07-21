package com.hermes.android;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hermes intent parser.
 * P2-11: 增加否定词检测 + 关键词精确化, 修复误触发。
 */
public class IntentParser {

    private static final Pattern NUMBER_RE = Pattern.compile("(\\d+)");
    private static final Pattern COORDS_RE = Pattern.compile("(\\d+)\\s*[,，x×]\\s*(\\d+)");
    private static final Pattern PKG_RE = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+");
    private static final Pattern PHONE_RE = Pattern.compile("1[3-9]\\d{9}");

    /** 否定前缀 — 匹配到则视为非指令 (交给 AI 处理) */
    private static final String[] NEGATION_PREFIXES = {
        "别", "不要", "不用", "取消", "停止", "别给我", "不需要"
    };

    public ParsedCommand parse(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        text = text.trim();
        String lower = text.toLowerCase();
        String compact = lower.replace(" ", "");

        // P2-11: 否定检测 — "别打开手电筒" 不是指令
        for (String neg : NEGATION_PREFIXES) {
            if (compact.startsWith(neg)) return null;
        }

        // Help
        if (containsAny(compact, "help", "帮助", "你能做什么", "功能列表", "能力")) {
            return new ParsedCommand("help");
        }

        // Torch — 精确匹配, 避免 "别打开手电筒" 误触发 (已被否定检测拦截)
        if (containsAny(compact, "打开手电筒", "开手电筒", "手电筒开", "torch on", "开灯")) {
            return new ParsedCommand("torch.on");
        }
        if (containsAny(compact, "关闭手电筒", "关手电筒", "手电筒关", "torch off", "关灯")) {
            return new ParsedCommand("torch.off");
        }

        // Battery
        if (containsAny(compact, "电量", "电池", "battery", "还剩多少电")) {
            return new ParsedCommand("battery.status");
        }

        // System info
        if (containsAny(compact, "设备信息", "手机信息", "平板信息", "系统信息", "型号")) {
            return new ParsedCommand("system.info");
        }

        // Brightness
        if (containsAny(compact, "当前亮度", "亮度多少")) {
            return new ParsedCommand("brightness.get");
        }
        if (containsAny(compact, "亮度调到", "调亮度", "设置亮度", "调亮", "调暗")) {
            Integer val = extractNumber(text);
            if (val == null) return ParsedCommand.error("需要亮度值 0-255，如: 亮度调到 120");
            return new ParsedCommand("brightness.set").arg("value", val);
        }

        // Volume
        if (containsAny(compact, "当前音量", "音量多少", "声音多大")) {
            return new ParsedCommand("volume.get");
        }
        if (containsAny(compact, "音量调到", "调音量", "设置音量")) {
            Integer val = extractNumber(text);
            if (val == null) return ParsedCommand.error("需要音量值 0-15，如: 音量调到 10");
            return new ParsedCommand("volume.set").arg("value", val);
        }

        // WiFi
        if (containsAny(compact, "wifi状态", "wifi信息", "当前wifi", "网络状态", "连的哪个wifi")) {
            return new ParsedCommand("wifi.status");
        }
        if (containsAny(compact, "打开wifi", "开wifi", "wifi开")) {
            return new ParsedCommand("wifi.toggle").arg("state", "enable");
        }
        if (containsAny(compact, "关闭wifi", "关wifi", "wifi关")) {
            return new ParsedCommand("wifi.toggle").arg("state", "disable");
        }

        // Vibrate
        if (containsAny(compact, "震动", "振动", "vibrate")) {
            Integer dur = extractNumber(text);
            return new ParsedCommand("vibrate").arg("duration", dur != null ? dur : 500);
        }

        // TTS
        if (containsAny(compact, "朗读", "念出来", "语音播报", "tts", "念给我听")) {
            String content = extractAfter(text, "朗读", "念出来", "念出", "语音播报", "tts", "说");
            if (content.isEmpty()) content = text;
            return new ParsedCommand("tts.speak").arg("text", content);
        }

        // Clipboard: set 优先 (包含更具体的前缀 "复制到"/"写入", 必须排在 get 的泛化 "剪贴板" 之前)
        if (containsAny(compact, "复制到剪贴板", "写入剪贴板")) {
            String content = extractAfter(text, "复制到剪贴板", "写入剪贴板", "复制");
            return new ParsedCommand("clipboard.set").arg("text", content);
        }
        if (containsAny(compact, "读取剪贴板", "剪贴板", "粘贴板")) {
            return new ParsedCommand("clipboard.get");
        }

        // Toast
        if (containsAny(compact, "toast", "弹窗提示", "弹出消息")) {
            String msg = extractAfter(text, "toast", "弹窗提示", "弹出消息");
            if (msg.isEmpty()) msg = "Hermes";
            return new ParsedCommand("toast").arg("message", msg);
        }

        // P2-11: Notification — 去掉裸 "通知", 只保留明确动词前缀, 避免 "收到通知" 误触发
        if (containsAny(compact, "发通知", "推送通知", "notification", "发个通知")) {
            String content = extractAfter(text, "发通知", "推送通知", "notification", "发个通知");
            if (content.isEmpty()) content = text;
            return new ParsedCommand("notification.post").arg("content", content);
        }

        // Location
        if (containsAny(compact, "我在哪", "定位", "位置", "gps", "location")) {
            return new ParsedCommand("location.get");
        }

        // Camera
        if (containsAny(compact, "拍照", "拍一张", "照相", "takephoto")) {
            return new ParsedCommand("camera.photo");
        }

        // SMS
        if (containsAny(compact, "最近短信", "短信", "sms")) {
            Integer limit = extractNumber(text);
            return new ParsedCommand("sms.recent").arg("limit", limit != null ? limit : 5);
        }

        // Contacts
        if (containsAny(compact, "联系人", "通讯录", "contacts")) {
            return new ParsedCommand("contacts.list");
        }

        // Phone call
        if (containsAny(compact, "打电话", "拨打", "拨号")) {
            Matcher m = PHONE_RE.matcher(text);
            if (m.find()) {
                return new ParsedCommand("telephony.call").arg("number", m.group());
            }
            return ParsedCommand.error("需要电话号码，如: 打电话 13800138000");
        }

        // Screen capture
        if (containsAny(compact, "截屏", "截图", "screenshot")) {
            return new ParsedCommand("screen.capture");
        }

        // App list
        if (containsAny(compact, "应用列表", "装了哪些app", "安装了什么", "applist")) {
            return new ParsedCommand("app.list");
        }

        // App launch
        if (containsAny(compact, "打开应用", "启动应用", "打开app")) {
            String pkg = extractPackage(text);
            if (pkg.isEmpty()) return ParsedCommand.error("需要包名，如: 打开应用 com.android.settings");
            return new ParsedCommand("app.launch").arg("package", pkg);
        }

        // Tap
        if (containsAny(compact, "点击", "tap", "触摸")) {
            Matcher m = COORDS_RE.matcher(text);
            if (m.find()) {
                return new ParsedCommand("input.tap")
                    .arg("x", Integer.parseInt(m.group(1)))
                    .arg("y", Integer.parseInt(m.group(2)));
            }
            List<Integer> nums = extractAllNumbers(text);
            if (nums.size() >= 2) {
                return new ParsedCommand("input.tap")
                    .arg("x", nums.get(0))
                    .arg("y", nums.get(1));
            }
            return ParsedCommand.error("需要坐标，如: 点击 500,800");
        }

        // Swipe
        if (containsAny(compact, "滑动", "swipe", "划动")) {
            List<Integer> nums = extractAllNumbers(text);
            if (nums.size() >= 4) {
                return new ParsedCommand("input.swipe")
                    .arg("x1", nums.get(0)).arg("y1", nums.get(1))
                    .arg("x2", nums.get(2)).arg("y2", nums.get(3));
            }
            return ParsedCommand.error("需要4个坐标，如: 滑动 500 1500 500 500");
        }

        // IP address
        if (containsAny(compact, "ip地址", "我的ip", "网络接口")) {
            return new ParsedCommand("network.info");
        }

        // Process list
        if (containsAny(compact, "进程", "process", "任务列表")) {
            return new ParsedCommand("process.list");
        }

        // File list
        if (containsAny(compact, "文件列表", "ls", "列出文件", "看看文件", "目录")) {
            String path = extractAfter(text, "文件列表", "ls", "列出文件", "看看文件", "目录");
            if (path.isEmpty()) path = "/sdcard/";
            return new ParsedCommand("file.ls").arg("path", path);
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase().replace(" ", ""))) return true;
        }
        return false;
    }

    private Integer extractNumber(String text) {
        Matcher m = NUMBER_RE.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private List<Integer> extractAllNumbers(String text) {
        List<Integer> nums = new ArrayList<>();
        Matcher m = NUMBER_RE.matcher(text);
        while (m.find()) nums.add(Integer.parseInt(m.group(1)));
        return nums;
    }

    private String extractAfter(String text, String... markers) {
        for (String marker : markers) {
            int idx = text.toLowerCase().indexOf(marker);
            if (idx >= 0) {
                String after = text.substring(idx + marker.length()).trim();
                return after.replaceAll("^[:：，,\\s]+", "").trim();
            }
        }
        return "";
    }

    private String extractPackage(String text) {
        Matcher m = PKG_RE.matcher(text);
        return m.find() ? m.group() : "";
    }
}
