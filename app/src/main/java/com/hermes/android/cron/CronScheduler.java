package com.hermes.android.cron;

import java.util.Calendar;
import java.util.TreeSet;

/**
 * P2: 5 段 cron 表达式 (分 时 日 月 周) 解析 + 下次触发时间计算。纯 Java, 可单测。
 * 支持: "*" 、"* /N" (无空格)、数字、逗号列表、"a-b" 区间。
 * 日/周同时受限时按 cron 惯例取并集 (OR 语义); 周字段 0/7 均为周日。
 */
public final class CronScheduler {

    /** WorkManager 最小周期 15 分钟 */
    public static final long MIN_INTERVAL_MIN = 15;

    /** 调度计划: 周期 + 首次触发延迟 + 提示 */
    public static class Plan {
        public final long intervalMinutes;
        public final long initialDelayMinutes;
        public final String notice; // 周期被 clamp 时非 null

        Plan(long intervalMinutes, long initialDelayMinutes, String notice) {
            this.intervalMinutes = intervalMinutes;
            this.initialDelayMinutes = initialDelayMinutes;
            this.notice = notice;
        }
    }

    /** 解析 cron 并计算调度计划; 非法表达式抛 IllegalArgumentException */
    public static Plan plan(String cron, long nowMillis) {
        String[] parts = cron == null ? new String[0] : cron.trim().split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException("无法解析的时间表达式: " + cron);
        }
        Field minute, hour, dom, month, dow;
        try {
            minute = parseField(parts[0], 0, 59, false);
            hour = parseField(parts[1], 0, 23, false);
            dom = parseField(parts[2], 1, 31, false);
            month = parseField(parts[3], 1, 12, false);
            dow = parseField(parts[4], 0, 7, true);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无法解析的时间表达式: " + cron);
        }

        long t1 = nextTrigger(minute, hour, dom, month, dow, nowMillis);
        long t2 = nextTrigger(minute, hour, dom, month, dow, t1);
        long interval = (t2 - t1) / 60000L;
        String notice = null;
        if (interval < MIN_INTERVAL_MIN) {
            interval = MIN_INTERVAL_MIN;
            notice = "WorkManager 最小周期为 15 分钟, 已自动调整为 " + MIN_INTERVAL_MIN + " 分钟";
        }
        long delay = (t1 - nowMillis + 59999L) / 60000L;
        if (delay < 1) delay = 1;
        return new Plan(interval, delay, notice);
    }

    // ==================== 内部实现 ====================

    private static class Field {
        final TreeSet<Integer> values;
        final boolean star;
        Field(TreeSet<Integer> values, boolean star) {
            this.values = values;
            this.star = star;
        }
    }

    private static Field parseField(String s, int min, int max, boolean isDow) {
        TreeSet<Integer> values = new TreeSet<>();
        boolean star = "*".equals(s);
        for (String tok : s.split(",")) {
            tok = tok.trim();
            if (tok.isEmpty()) throw new IllegalArgumentException("empty token");
            if (tok.equals("*")) {
                for (int i = min; i <= max; i++) values.add(i);
            } else if (tok.startsWith("*/")) {
                int step = parseInt(tok.substring(2));
                if (step < 1) throw new IllegalArgumentException("bad step");
                for (int i = min; i <= max; i += step) values.add(i);
            } else if (tok.contains("-")) {
                String[] ab = tok.split("-");
                if (ab.length != 2) throw new IllegalArgumentException("bad range");
                int a = parseInt(ab[0]);
                int b = parseInt(ab[1]);
                if (a > b || a < min || b > max) throw new IllegalArgumentException("bad range");
                for (int i = a; i <= b; i++) values.add(i);
            } else {
                int v = parseInt(tok);
                if (v < min || v > max) throw new IllegalArgumentException("out of range");
                values.add(v);
            }
        }
        if (values.isEmpty()) throw new IllegalArgumentException("empty field");
        if (isDow) {
            // 0/7 均为周日, 归一化为 0
            TreeSet<Integer> norm = new TreeSet<>();
            for (int v : values) norm.add(v == 7 ? 0 : v);
            values = norm;
        }
        return new Field(values, star);
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + s);
        }
    }

    /** afterMillis 之后 (不含) 的下一个触发时刻 */
    private static long nextTrigger(Field minute, Field hour, Field dom,
                                    Field month, Field dow, long afterMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(afterMillis);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MINUTE, 1);
        for (int d = 0; d < 400; d++) {
            if (month.values.contains(cal.get(Calendar.MONTH) + 1)
                    && dayMatch(dom, dow, cal)) {
                int nowH = cal.get(Calendar.HOUR_OF_DAY);
                int nowM = cal.get(Calendar.MINUTE);
                for (int h : hour.values) {
                    for (int mi : minute.values) {
                        if (h > nowH || (h == nowH && mi >= nowM)) {
                            cal.set(Calendar.HOUR_OF_DAY, h);
                            cal.set(Calendar.MINUTE, mi);
                            return cal.getTimeInMillis();
                        }
                    }
                }
            }
            // 推进到次日 00:00
            cal.add(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
        }
        throw new IllegalArgumentException("无法解析的时间表达式");
    }

    /** 日/周匹配: 两者都受限时取并集 (Vixie cron OR 语义) */
    private static boolean dayMatch(Field dom, Field dow, Calendar cal) {
        if (dom.star && dow.star) return true;
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=周日
        if (dom.star) return dow.values.contains(dayOfWeek);
        if (dow.star) return dom.values.contains(dayOfMonth);
        return dom.values.contains(dayOfMonth) || dow.values.contains(dayOfWeek);
    }
}
