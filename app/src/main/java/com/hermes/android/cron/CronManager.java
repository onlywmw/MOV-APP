package com.hermes.android.cron;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * P1-6: 真实 Cron 调度引擎。
 * 使用 WorkManager 实现周期性任务调度。
 * 任务元数据存储在 SharedPreferences, 实际执行由 HermesCronWorker 完成。
 */
public class CronManager {

    private static final String TAG = "CronManager";
    private static final String PREFS = "hermes_cron_jobs";
    private static final String KEY_JOBS = "jobs_json";

    private final Context context;
    private final SharedPreferences prefs;

    public CronManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ==================== JSON 持久化 helpers ====================

    private JSONArray loadJobs() {
        try {
            return new JSONArray(prefs.getString(KEY_JOBS, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void saveJobs(JSONArray jobs) {
        prefs.edit().putString(KEY_JOBS, jobs.toString()).apply();
    }

    // ==================== 公共 API ====================

    /** 列出所有任务 JSON */
    public String listJobsJson() {
        return loadJobs().toString();
    }

    /** 创建任务 */
    public String createJob(String name, String cronExpr, String command) {
        try {
            JSONArray jobs = loadJobs();

            String jobId = UUID.randomUUID().toString().substring(0, 8);
            long intervalMinutes = parseCronToMinutes(cronExpr);
            long actualInterval = Math.max(15, intervalMinutes); // WorkManager 最小周期 15 分钟

            JSONObject job = new JSONObject();
            job.put("id", jobId);
            job.put("name", name);
            job.put("cron", cronExpr);
            job.put("command", command);
            job.put("enabled", true);
            job.put("intervalMin", actualInterval);
            job.put("lastRun", "");
            job.put("lastStatus", "");
            job.put("createdAt", System.currentTimeMillis());
            jobs.put(job);

            saveJobs(jobs);

            // 注册 WorkManager
            scheduleWork(jobId, actualInterval);

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("id", jobId);
            result.put("intervalMin", actualInterval);
            if (actualInterval != intervalMinutes) {
                result.put("notice", "WorkManager 最小周期为 15 分钟, 已自动调整为 " + actualInterval + " 分钟");
            }
            return result.toString();
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /** 启用/禁用任务 */
    public String toggleJob(String jobId, boolean enabled) {
        try {
            JSONArray jobs = loadJobs();
            for (int i = 0; i < jobs.length(); i++) {
                JSONObject job = jobs.getJSONObject(i);
                if (jobId.equals(job.getString("id"))) {
                    job.put("enabled", enabled);
                    saveJobs(jobs);
                    if (enabled) {
                        scheduleWork(jobId, job.getLong("intervalMin"));
                    } else {
                        WorkManager.getInstance(context).cancelUniqueWork("hermes_cron_" + jobId);
                    }
                    return new JSONObject().put("ok", true).toString();
                }
            }
            return "{\"ok\":false,\"error\":\"任务不存在\"}";
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /** 删除任务 */
    public String deleteJob(String jobId) {
        try {
            JSONArray jobs = loadJobs();
            JSONArray filtered = new JSONArray();
            for (int i = 0; i < jobs.length(); i++) {
                JSONObject job = jobs.getJSONObject(i);
                if (!jobId.equals(job.getString("id"))) {
                    filtered.put(job);
                }
            }
            saveJobs(filtered);
            WorkManager.getInstance(context).cancelUniqueWork("hermes_cron_" + jobId);
            return new JSONObject().put("ok", true).toString();
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /** 注册 WorkManager 周期任务 */
    private void scheduleWork(String jobId, long intervalMinutes) {
        // WorkManager 最小周期 15 分钟
        long interval = Math.max(15, intervalMinutes);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                HermesCronWorker.class, interval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("hermes_cron")
                .addTag("job_" + jobId)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "hermes_cron_" + jobId,
                ExistingPeriodicWorkPolicy.UPDATE,
                work);

        Log.i(TAG, "scheduled cron job " + jobId + " every " + interval + "min");
    }

    /**
     * 简易 cron 表达式转分钟间隔。
     * 支持: "30 8 * * *" (每天), "0 星号/6 * * *" (每6小时),
     *       "星号/30 * * * *" (每30分钟)
     */
    private long parseCronToMinutes(String cron) {
        try {
            String[] parts = cron.trim().split("\\s+");
            if (parts.length < 5) return 1440; // 默认每天

            String minute = parts[0];
            String hour = parts[1];

            // */N 分钟
            if (minute.startsWith("*/")) {
                return Long.parseLong(minute.substring(2));
            }
            // */N 小时
            if (hour.startsWith("*/")) {
                return Long.parseLong(hour.substring(2)) * 60;
            }
            // 固定时间 → 每天
            return 1440;
        } catch (Exception e) {
            return 1440;
        }
    }
}
