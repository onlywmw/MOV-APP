package com.hermes.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TokenMeter — 全局 token 用量计量 (V5 运行页仪表盘)。
 *
 * AiClient 每次成功调用时 record() 累计:
 * - 按日:   d_<yyyy-MM-dd>_p / _c
 * - 按月:   m_<yyyy-MM>_p / _c
 * - 按模型: x_<modelKey>_p / _c (本月)
 * 纯本地 SharedPreferences, 不上报。
 */
public class TokenMeter {

    private static final String PREFS = "mov_token_meter";
    /** 月配额 (运行页进度条分母; 后续可做成设置项) */
    public static final long MONTH_QUOTA = 5_000_000L;

    private static SharedPreferences prefs;

    public static void init(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 记录一次成功调用; 任意异常静默 (计量永不阻断主链路) */
    public static void record(int promptTokens, int completionTokens, String modelKey) {
        if (prefs == null || (promptTokens <= 0 && completionTokens <= 0)) return;
        try {
            String day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String month = day.substring(0, 7);
            SharedPreferences.Editor e = prefs.edit();
            bump(e, "d_" + day + "_p", promptTokens);
            bump(e, "d_" + day + "_c", completionTokens);
            bump(e, "m_" + month + "_p", promptTokens);
            bump(e, "m_" + month + "_c", completionTokens);
            if (modelKey != null && !modelKey.isEmpty()) {
                bump(e, "x_" + month + "_" + modelKey + "_p", promptTokens);
                bump(e, "x_" + month + "_" + modelKey + "_c", completionTokens);
            }
            e.apply();
        } catch (Exception ignored) {}
    }

    private static void bump(SharedPreferences.Editor e, String key, int delta) {
        if (delta <= 0) return;
        e.putLong(key, prefs.getLong(key, 0) + delta);
    }

    /** 仪表盘数据: {today, month, quota, byModel:[{key,tokens}]} */
    public static String statsJson() {
        try {
            if (prefs == null) return "{\"today\":0,\"month\":0,\"quota\":" + MONTH_QUOTA + "}";
            String day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String month = day.substring(0, 7);
            long today = prefs.getLong("d_" + day + "_p", 0) + prefs.getLong("d_" + day + "_c", 0);
            long monthTotal = prefs.getLong("m_" + month + "_p", 0) + prefs.getLong("m_" + month + "_c", 0);
            JSONObject o = new JSONObject()
                    .put("today", today)
                    .put("month", monthTotal)
                    .put("quota", MONTH_QUOTA);
            return o.toString();
        } catch (Exception e) {
            return "{\"today\":0,\"month\":0,\"quota\":" + MONTH_QUOTA + "}";
        }
    }
}
