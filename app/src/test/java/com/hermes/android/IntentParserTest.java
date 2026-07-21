package com.hermes.android;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * IntentParser 单元测试 — 覆盖 26 种意图各 2-3 个变体 (~70 用例)。
 * 纯 Java 测试，不依赖 Android 运行时。
 */
public class IntentParserTest {

    private final IntentParser parser = new IntentParser();

    // ==================== HELP ====================
    @Test public void helpZh() {
        ParsedCommand c = parser.parse("帮助");
        assertNotNull(c);
        assertEquals("help", c.getCapability());
    }
    @Test public void helpEn() {
        ParsedCommand c = parser.parse("help");
        assertNotNull(c);
        assertEquals("help", c.getCapability());
    }
    @Test public void helpCapability() {
        ParsedCommand c = parser.parse("你能做什么");
        assertNotNull(c);
        assertEquals("help", c.getCapability());
    }

    // ==================== TORCH ====================
    @Test public void torchOnZh() {
        ParsedCommand c = parser.parse("打开手电筒");
        assertNotNull(c);
        assertEquals("torch.on", c.getCapability());
    }
    @Test public void torchOnShort() {
        ParsedCommand c = parser.parse("开灯");
        assertNotNull(c);
        assertEquals("torch.on", c.getCapability());
    }
    @Test public void torchOffZh() {
        ParsedCommand c = parser.parse("关闭手电筒");
        assertNotNull(c);
        assertEquals("torch.off", c.getCapability());
    }
    @Test public void torchOffShort() {
        ParsedCommand c = parser.parse("关灯");
        assertNotNull(c);
        assertEquals("torch.off", c.getCapability());
    }

    // ==================== BATTERY ====================
    @Test public void batteryZh() {
        ParsedCommand c = parser.parse("电量多少");
        assertNotNull(c);
        assertEquals("battery.status", c.getCapability());
    }
    @Test public void batteryShort() {
        ParsedCommand c = parser.parse("电池");
        assertNotNull(c);
        assertEquals("battery.status", c.getCapability());
    }
    @Test public void batteryEn() {
        ParsedCommand c = parser.parse("battery");
        assertNotNull(c);
        assertEquals("battery.status", c.getCapability());
    }

    // ==================== SYSTEM INFO ====================
    @Test public void systemInfoZh() {
        ParsedCommand c = parser.parse("设备信息");
        assertNotNull(c);
        assertEquals("system.info", c.getCapability());
    }
    @Test public void systemInfoModel() {
        ParsedCommand c = parser.parse("型号");
        assertNotNull(c);
        assertEquals("system.info", c.getCapability());
    }
    @Test public void systemInfoPad() {
        ParsedCommand c = parser.parse("平板信息");
        assertNotNull(c);
        assertEquals("system.info", c.getCapability());
    }

    // ==================== BRIGHTNESS ====================
    @Test public void brightnessGet() {
        ParsedCommand c = parser.parse("当前亮度");
        assertNotNull(c);
        assertEquals("brightness.get", c.getCapability());
    }
    @Test public void brightnessGetAlt() {
        ParsedCommand c = parser.parse("亮度多少");
        assertNotNull(c);
        assertEquals("brightness.get", c.getCapability());
    }
    @Test public void brightnessSet() {
        ParsedCommand c = parser.parse("亮度调到 128");
        assertNotNull(c);
        assertEquals("brightness.set", c.getCapability());
        assertEquals(128, c.getIntArg("value", 0));
    }
    @Test public void brightnessSetAlt() {
        ParsedCommand c = parser.parse("调亮度到 200");
        assertNotNull(c);
        assertEquals("brightness.set", c.getCapability());
        assertEquals(200, c.getIntArg("value", 0));
    }
    @Test public void brightnessSetDark() {
        ParsedCommand c = parser.parse("调暗");
        assertNotNull(c);
        assertTrue(c.isError()); // 需要数值
    }

    // ==================== VOLUME ====================
    @Test public void volumeGet() {
        ParsedCommand c = parser.parse("当前音量");
        assertNotNull(c);
        assertEquals("volume.get", c.getCapability());
    }
    @Test public void volumeGetAlt() {
        ParsedCommand c = parser.parse("音量多少");
        assertNotNull(c);
        assertEquals("volume.get", c.getCapability());
    }
    @Test public void volumeSet() {
        ParsedCommand c = parser.parse("音量调到 10");
        assertNotNull(c);
        assertEquals("volume.set", c.getCapability());
        assertEquals(10, c.getIntArg("value", 0));
    }

    // ==================== WIFI ====================
    @Test public void wifiStatus() {
        ParsedCommand c = parser.parse("WiFi状态");
        assertNotNull(c);
        assertEquals("wifi.status", c.getCapability());
    }
    @Test public void wifiStatusNetwork() {
        ParsedCommand c = parser.parse("网络状态");
        assertNotNull(c);
        assertEquals("wifi.status", c.getCapability());
    }
    @Test public void wifiToggleOn() {
        ParsedCommand c = parser.parse("打开wifi");
        assertNotNull(c);
        assertEquals("wifi.toggle", c.getCapability());
        assertEquals("enable", c.getStringArg("state", ""));
    }
    @Test public void wifiToggleOff() {
        ParsedCommand c = parser.parse("关闭wifi");
        assertNotNull(c);
        assertEquals("wifi.toggle", c.getCapability());
        assertEquals("disable", c.getStringArg("state", ""));
    }

    // ==================== VIBRATE ====================
    @Test public void vibrate() {
        ParsedCommand c = parser.parse("震动");
        assertNotNull(c);
        assertEquals("vibrate", c.getCapability());
        assertEquals(500, c.getIntArg("duration", 0));
    }
    @Test public void vibrateWithDuration() {
        ParsedCommand c = parser.parse("震动 1000 毫秒");
        assertNotNull(c);
        assertEquals("vibrate", c.getCapability());
        assertEquals(1000, c.getIntArg("duration", 0));
    }
    @Test public void vibrateEn() {
        ParsedCommand c = parser.parse("vibrate 300");
        assertNotNull(c);
        assertEquals("vibrate", c.getCapability());
        assertEquals(300, c.getIntArg("duration", 0));
    }

    // ==================== TTS ====================
    @Test public void ttsSpeak() {
        ParsedCommand c = parser.parse("朗读 你好世界");
        assertNotNull(c);
        assertEquals("tts.speak", c.getCapability());
        assertEquals("你好世界", c.getStringArg("text", ""));
    }
    @Test public void ttsNian() {
        ParsedCommand c = parser.parse("念出来 今天天气不错");
        assertNotNull(c);
        assertEquals("tts.speak", c.getCapability());
        assertTrue(c.getStringArg("text", "").contains("今天天气不错"));
    }
    @Test public void ttsEn() {
        ParsedCommand c = parser.parse("tts hello");
        assertNotNull(c);
        assertEquals("tts.speak", c.getCapability());
    }

    // ==================== CLIPBOARD ====================
    @Test public void clipboardGet() {
        ParsedCommand c = parser.parse("读取剪贴板");
        assertNotNull(c);
        assertEquals("clipboard.get", c.getCapability());
    }
    @Test public void clipboardGetShort() {
        ParsedCommand c = parser.parse("剪贴板");
        assertNotNull(c);
        assertEquals("clipboard.get", c.getCapability());
    }
    @Test public void clipboardSetFixed() {
        // P2: 已修复 — clipboard.set 现在排在 clipboard.get 前面
        ParsedCommand c = parser.parse("复制到剪贴板 hello world");
        assertNotNull(c);
        assertEquals("clipboard.set", c.getCapability());
        assertEquals("hello world", c.getStringArg("text", ""));
    }
    @Test public void clipboardSetViaWrite() {
        ParsedCommand c = parser.parse("写入剪贴板 some text");
        assertNotNull(c);
        assertEquals("clipboard.set", c.getCapability());
        assertEquals("some text", c.getStringArg("text", ""));
    }

    // ==================== TOAST ====================
    @Test public void toast() {
        ParsedCommand c = parser.parse("toast 你好");
        assertNotNull(c);
        assertEquals("toast", c.getCapability());
        assertEquals("你好", c.getStringArg("message", ""));
    }
    @Test public void toastZh() {
        ParsedCommand c = parser.parse("弹窗提示 操作成功");
        assertNotNull(c);
        assertEquals("toast", c.getCapability());
    }

    // ==================== NOTIFICATION ====================
    @Test public void notification() {
        ParsedCommand c = parser.parse("发通知 会议开始");
        assertNotNull(c);
        assertEquals("notification.post", c.getCapability());
        assertTrue(c.getStringArg("content", "").contains("会议开始"));
    }
    @Test public void notificationEn() {
        ParsedCommand c = parser.parse("notification test message");
        assertNotNull(c);
        assertEquals("notification.post", c.getCapability());
    }

    // ==================== LOCATION ====================
    @Test public void locationZh() {
        ParsedCommand c = parser.parse("我在哪");
        assertNotNull(c);
        assertEquals("location.get", c.getCapability());
    }
    @Test public void locationGps() {
        ParsedCommand c = parser.parse("定位");
        assertNotNull(c);
        assertEquals("location.get", c.getCapability());
    }
    @Test public void locationEn() {
        ParsedCommand c = parser.parse("location");
        assertNotNull(c);
        assertEquals("location.get", c.getCapability());
    }

    // ==================== CAMERA ====================
    @Test public void cameraZh() {
        ParsedCommand c = parser.parse("拍照");
        assertNotNull(c);
        assertEquals("camera.photo", c.getCapability());
    }
    @Test public void cameraAlt() {
        ParsedCommand c = parser.parse("拍一张");
        assertNotNull(c);
        assertEquals("camera.photo", c.getCapability());
    }
    @Test public void cameraEn() {
        ParsedCommand c = parser.parse("takephoto");
        assertNotNull(c);
        assertEquals("camera.photo", c.getCapability());
    }

    // ==================== SMS ====================
    @Test public void smsRecent() {
        ParsedCommand c = parser.parse("最近短信");
        assertNotNull(c);
        assertEquals("sms.recent", c.getCapability());
        assertEquals(5, c.getIntArg("limit", 0));
    }
    @Test public void smsRecentWithLimit() {
        ParsedCommand c = parser.parse("列出最近 3 条短信");
        assertNotNull(c);
        assertEquals("sms.recent", c.getCapability());
        assertEquals(3, c.getIntArg("limit", 0));
    }
    @Test public void smsEn() {
        ParsedCommand c = parser.parse("sms");
        assertNotNull(c);
        assertEquals("sms.recent", c.getCapability());
    }

    // ==================== CONTACTS ====================
    @Test public void contactsZh() {
        ParsedCommand c = parser.parse("联系人");
        assertNotNull(c);
        assertEquals("contacts.list", c.getCapability());
    }
    @Test public void contactsAlt() {
        ParsedCommand c = parser.parse("通讯录");
        assertNotNull(c);
        assertEquals("contacts.list", c.getCapability());
    }
    @Test public void contactsEn() {
        ParsedCommand c = parser.parse("contacts");
        assertNotNull(c);
        assertEquals("contacts.list", c.getCapability());
    }

    // ==================== PHONE CALL ====================
    @Test public void callZh() {
        ParsedCommand c = parser.parse("打电话 13800138000");
        assertNotNull(c);
        assertEquals("telephony.call", c.getCapability());
        assertEquals("13800138000", c.getStringArg("number", ""));
    }
    @Test public void callAlt() {
        ParsedCommand c = parser.parse("拨打 13912345678");
        assertNotNull(c);
        assertEquals("telephony.call", c.getCapability());
        assertEquals("13912345678", c.getStringArg("number", ""));
    }
    @Test public void callNoNumber() {
        ParsedCommand c = parser.parse("打电话");
        assertNotNull(c);
        assertTrue(c.isError()); // 需要电话号码
    }

    // ==================== SCREEN CAPTURE ====================
    @Test public void screenshotZh() {
        ParsedCommand c = parser.parse("截屏");
        assertNotNull(c);
        assertEquals("screen.capture", c.getCapability());
    }
    @Test public void screenshotAlt() {
        ParsedCommand c = parser.parse("截图");
        assertNotNull(c);
        assertEquals("screen.capture", c.getCapability());
    }
    @Test public void screenshotEn() {
        ParsedCommand c = parser.parse("screenshot");
        assertNotNull(c);
        assertEquals("screen.capture", c.getCapability());
    }

    // ==================== APP LIST ====================
    @Test public void appListZh() {
        ParsedCommand c = parser.parse("应用列表");
        assertNotNull(c);
        assertEquals("app.list", c.getCapability());
    }
    @Test public void appListAlt() {
        ParsedCommand c = parser.parse("装了哪些app");
        assertNotNull(c);
        assertEquals("app.list", c.getCapability());
    }
    @Test public void appListEn() {
        ParsedCommand c = parser.parse("applist");
        assertNotNull(c);
        assertEquals("app.list", c.getCapability());
    }

    // ==================== APP LAUNCH ====================
    @Test public void appLaunch() {
        ParsedCommand c = parser.parse("打开应用 com.android.settings");
        assertNotNull(c);
        assertEquals("app.launch", c.getCapability());
        assertEquals("com.android.settings", c.getStringArg("package", ""));
    }
    @Test public void appLaunchNoPackage() {
        ParsedCommand c = parser.parse("打开应用");
        assertNotNull(c);
        assertTrue(c.isError()); // 需要包名
    }

    // ==================== INPUT TAP ====================
    @Test public void tapComma() {
        ParsedCommand c = parser.parse("点击 500,800");
        assertNotNull(c);
        assertEquals("input.tap", c.getCapability());
        assertEquals(500, c.getIntArg("x", 0));
        assertEquals(800, c.getIntArg("y", 0));
    }
    @Test public void tapSpace() {
        ParsedCommand c = parser.parse("点击 300 600");
        assertNotNull(c);
        assertEquals("input.tap", c.getCapability());
        assertEquals(300, c.getIntArg("x", 0));
        assertEquals(600, c.getIntArg("y", 0));
    }
    @Test public void tapChinese() {
        ParsedCommand c = parser.parse("触摸 100×200");
        assertNotNull(c);
        assertEquals("input.tap", c.getCapability());
    }
    @Test public void tapNoCoords() {
        ParsedCommand c = parser.parse("点击");
        assertNotNull(c);
        assertTrue(c.isError()); // 需要坐标
    }

    // ==================== INPUT SWIPE ====================
    @Test public void swipeFourCoords() {
        ParsedCommand c = parser.parse("滑动 500 1500 500 500");
        assertNotNull(c);
        assertEquals("input.swipe", c.getCapability());
        assertEquals(500, c.getIntArg("x1", 0));
        assertEquals(1500, c.getIntArg("y1", 0));
        assertEquals(500, c.getIntArg("x2", 0));
        assertEquals(500, c.getIntArg("y2", 0));
    }
    @Test public void swipeEn() {
        ParsedCommand c = parser.parse("swipe 100 200 300 400");
        assertNotNull(c);
        assertEquals("input.swipe", c.getCapability());
    }
    @Test public void swipeNoCoords() {
        ParsedCommand c = parser.parse("滑动");
        assertNotNull(c);
        assertTrue(c.isError()); // 需要 4 个坐标
    }

    // ==================== NETWORK INFO ====================
    @Test public void networkInfoZh() {
        ParsedCommand c = parser.parse("ip地址");
        assertNotNull(c);
        assertEquals("network.info", c.getCapability());
    }
    @Test public void networkInfoAlt() {
        ParsedCommand c = parser.parse("我的ip");
        assertNotNull(c);
        assertEquals("network.info", c.getCapability());
    }

    // ==================== PROCESS LIST ====================
    @Test public void processListZh() {
        ParsedCommand c = parser.parse("进程");
        assertNotNull(c);
        assertEquals("process.list", c.getCapability());
    }
    @Test public void processListAlt() {
        ParsedCommand c = parser.parse("任务列表");
        assertNotNull(c);
        assertEquals("process.list", c.getCapability());
    }
    @Test public void processListEn() {
        ParsedCommand c = parser.parse("process");
        assertNotNull(c);
        assertEquals("process.list", c.getCapability());
    }

    // ==================== FILE LS ====================
    @Test public void fileLsZh() {
        ParsedCommand c = parser.parse("文件列表");
        assertNotNull(c);
        assertEquals("file.ls", c.getCapability());
        assertEquals("/sdcard/", c.getStringArg("path", ""));
    }
    @Test public void fileLsWithPath() {
        ParsedCommand c = parser.parse("看看文件 /sdcard/Download");
        assertNotNull(c);
        assertEquals("file.ls", c.getCapability());
        assertEquals("/sdcard/Download", c.getStringArg("path", ""));
    }
    @Test public void fileLsEn() {
        ParsedCommand c = parser.parse("ls /data/local");
        assertNotNull(c);
        assertEquals("file.ls", c.getCapability());
        assertEquals("/data/local", c.getStringArg("path", ""));
    }

    // ==================== EDGE CASES ====================
    @Test public void nullInput() {
        assertNull(parser.parse(null));
    }
    @Test public void emptyInput() {
        assertNull(parser.parse(""));
    }
    @Test public void blankInput() {
        assertNull(parser.parse("   "));
    }
    @Test public void unknownInput() {
        // 未命中任何规则 → null (交给 AI 处理)
        assertNull(parser.parse("帮我写一首关于春天的诗"));
    }
    @Test public void unknownInputReturnsNull() {
        assertNull(parser.parse("今天天气怎么样"));
    }

    // ==================== PRIORITY REGRESSION ====================
    @Test public void brightnessBeforeVolume() {
        // "亮度" 关键词不应被 "音量" 误匹配
        ParsedCommand c = parser.parse("亮度调到 200");
        assertNotNull(c);
        assertEquals("brightness.set", c.getCapability());
    }
    @Test public void clipboardBeforeSms() {
        // "读取剪贴板" 不应被 "短信" 误匹配
        ParsedCommand c = parser.parse("读取剪贴板");
        assertNotNull(c);
        assertEquals("clipboard.get", c.getCapability());
    }
    @Test public void notificationBeforeContacts() {
        // "通知" 不应被 "通讯录" 误匹配
        ParsedCommand c = parser.parse("发通知 内容");
        assertNotNull(c);
        assertEquals("notification.post", c.getCapability());
    }

    // ==================== NUMBER EXTRACTION ====================
    @Test public void extractFirstNumber() {
        ParsedCommand c = parser.parse("亮度调到 128 和 200");
        assertNotNull(c);
        assertEquals("brightness.set", c.getCapability());
        assertEquals(128, c.getIntArg("value", 0)); // 取第一个数字
    }
    @Test public void negativeBrightness() {
        // \d+ 只匹配正数，"亮度调到 -50" 中匹配到 50, 不会报错
        ParsedCommand c = parser.parse("亮度调到 -50");
        assertNotNull(c);
        assertEquals("brightness.set", c.getCapability());
        assertEquals(50, c.getIntArg("value", 0)); // 正则 \d+ 匹配的是 50
    }
}
