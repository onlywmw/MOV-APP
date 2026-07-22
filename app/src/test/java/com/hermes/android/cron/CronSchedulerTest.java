package com.hermes.android.cron;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Calendar;

/**
 * CronScheduler / CronPolicy 单元测试 — 纯 Java, 不依赖 Android 运行时。
 * 覆盖 TC-RT04 (定点 cron 下次触发) / TC-RT05 (每 N 小时 + 15 分钟 clamp notice)
 * 及白名单创建校验 (CONTRACT_SECURITY 约束3)。
 */
public class CronSchedulerTest {

    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, hour, minute, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // ==================== 定点 cron: "30 8 * * *" ====================

    @Test public void fixedTimeLaterToday() {
        // 今天 6:00 创建 → 今天 8:30 触发
        CronScheduler.Plan p = CronScheduler.plan("30 8 * * *", at(2026, 7, 22, 6, 0));
        assertEquals(1440, p.intervalMinutes);
        assertEquals(150, p.initialDelayMinutes);
        assertNull(p.notice);
    }

    @Test public void fixedTimeAlreadyPassed() {
        // 今天 9:00 创建 → 明天 8:30 触发
        CronScheduler.Plan p = CronScheduler.plan("30 8 * * *", at(2026, 7, 22, 9, 0));
        assertEquals(1440, p.intervalMinutes);
        assertEquals(23 * 60 + 30, p.initialDelayMinutes);
    }

    @Test public void fixedTimeExactMinuteFiresNextDay() {
        // 正好 8:30:00 创建 → 下一次是明天 8:30
        CronScheduler.Plan p = CronScheduler.plan("30 8 * * *", at(2026, 7, 22, 8, 30));
        assertEquals(1440, p.initialDelayMinutes);
    }

    // ==================== 间隔 cron: "0 */3 * * *" ====================

    @Test public void every3Hours() {
        // 6:10 创建 → 9:00 触发 (170 分钟)
        CronScheduler.Plan p = CronScheduler.plan("0 */3 * * *", at(2026, 7, 22, 6, 10));
        assertEquals(180, p.intervalMinutes);
        assertEquals(170, p.initialDelayMinutes);
        assertNull(p.notice);
    }

    @Test public void every3HoursCrossesMidnight() {
        // 23:30 创建 → 次日 0:00 触发 (30 分钟)
        CronScheduler.Plan p = CronScheduler.plan("0 */3 * * *", at(2026, 7, 22, 23, 30));
        assertEquals(180, p.intervalMinutes);
        assertEquals(30, p.initialDelayMinutes);
    }

    // ==================== 高频 cron: clamp 15 分钟带 notice ====================

    @Test public void every10MinClampedWithNotice() {
        CronScheduler.Plan p = CronScheduler.plan("*/10 * * * *", at(2026, 7, 22, 6, 10));
        assertEquals(15, p.intervalMinutes);
        assertNotNull(p.notice);
        assertTrue(p.notice.contains("15"));
        // 下次触发仍是 6:20 (10 分钟后)
        assertEquals(10, p.initialDelayMinutes);
    }

    @Test public void every15MinNotClamped() {
        CronScheduler.Plan p = CronScheduler.plan("*/15 * * * *", at(2026, 7, 22, 6, 10));
        assertEquals(15, p.intervalMinutes);
        assertNull(p.notice); // 恰好 15 分钟, 无需调整
    }

    // ==================== 列表 / 区间 ====================

    @Test public void commaListHours() {
        // "0 8,20 * * *" 从 6:00 → 8:00, 间隔 12h
        CronScheduler.Plan p = CronScheduler.plan("0 8,20 * * *", at(2026, 7, 22, 6, 0));
        assertEquals(120, p.initialDelayMinutes);
        assertEquals(720, p.intervalMinutes);
    }

    @Test public void rangeWeekday() {
        // 2026-07-22 是周三, "0 9 * * 1-5" 当天 9 点可触发
        CronScheduler.Plan p = CronScheduler.plan("0 9 * * 1-5", at(2026, 7, 22, 6, 0));
        assertEquals(180, p.initialDelayMinutes);
    }

    @Test public void dowSkipsWeekend() {
        // 2026-07-25 是周六, "0 9 * * 1-5" → 下周一 9:00 (2 天 + 3 小时)
        CronScheduler.Plan p = CronScheduler.plan("0 9 * * 1-5", at(2026, 7, 25, 6, 0));
        assertEquals((2 * 24 + 3) * 60, p.initialDelayMinutes);
    }

    // ==================== 非法表达式 ====================

    @Test public void invalidWordThrows() {
        try {
            CronScheduler.plan("abc", 0);
            fail("应当抛 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("无法解析的时间表达式"));
        }
    }

    @Test public void outOfRangeThrows() {
        try {
            CronScheduler.plan("61 * * * *", 0);
            fail("应当抛 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("无法解析的时间表达式"));
        }
    }

    @Test public void wrongFieldCountThrows() {
        try {
            CronScheduler.plan("* * *", 0);
            fail("应当抛 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("无法解析的时间表达式"));
        }
    }

    @Test public void nullExprThrows() {
        try {
            CronScheduler.plan(null, 0);
            fail("应当抛 IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("无法解析的时间表达式"));
        }
    }

    // ==================== 白名单 (CONTRACT_SECURITY 约束3) ====================

    @Test public void whitelistRejectsUnreachableActions() {
        assertFalse(CronPolicy.isAllowed("file.write"));
        assertFalse(CronPolicy.isAllowed("file.read"));
        assertFalse(CronPolicy.isAllowed("file.delete"));
        assertFalse(CronPolicy.isAllowed("file.mkdir"));
        assertFalse(CronPolicy.isAllowed("http.get"));
    }

    @Test public void whitelistRejectsDangerousActions() {
        assertFalse(CronPolicy.isAllowed("input.tap"));
        assertFalse(CronPolicy.isAllowed("telephony.call"));
        assertFalse(CronPolicy.isAllowed("sms.recent"));
        assertFalse(CronPolicy.isAllowed("location.get"));
        assertFalse(CronPolicy.isAllowed(null));
    }

    @Test public void whitelistAllowsQueryActions() {
        assertTrue(CronPolicy.isAllowed("battery.status"));
        assertTrue(CronPolicy.isAllowed("system.info"));
        assertTrue(CronPolicy.isAllowed("wifi.status"));
        assertTrue(CronPolicy.isAllowed("network.info"));
        assertTrue(CronPolicy.isAllowed("process.list"));
        assertTrue(CronPolicy.isAllowed("file.ls"));
        assertTrue(CronPolicy.isAllowed("brightness.get"));
        assertTrue(CronPolicy.isAllowed("volume.get"));
    }
}
