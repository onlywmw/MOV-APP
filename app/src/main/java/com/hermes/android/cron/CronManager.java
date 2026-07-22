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
            // P2: 创建时校验指令白名单, 不支持直接拒绝
            com.hermes.android.ParsedCommand cmd = new com.hermes.android.IntentParser().parse(command);
            if (cmd == null || cmd.isError() || !CronPolicy.isAllowed(cmd.getCapability())) {
                return "{\"ok\":false,\"error\":\"该指令不支持定时执行\"}";
            }

            // P2: 解析 cron, 计算下次触发时间 (非法表达式直接报错, 不再静默回落 1440)
            CronScheduler.Plan plan;
            try {
                plan = CronScheduler.plan(cronExpr, System.currentTimeMillis());
            } catch (IllegalArgumentException e) {
                return new JSONObject().put("ok", false).put("error", e.getMessage()).toString();
            }

            JSONArray jobs = loadJobs();

            String jobId = UUID.randomUUID().toString().substring(0, 8);

            JSONObject job = new JSONObject();
            job.put("id", jobId);
            job.put("name", name);
            job.put("cron", cronExpr);
            job.put("command", command);
            job.put("enabled", true);
            job.put("intervalMin", plan.intervalMinutes);
            job.put("lastRun", "");
            job.put("lastStatus", "");
            job.put("createdAt", System.currentTimeMillis());
            jobs.put(job);

            saveJobs(jobs);

            // 注册 WorkManager
            scheduleWork(jobId, plan.intervalMinutes, plan.initialDelayMinutes);

            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("id", jobId);
            result.put("intervalMin", plan.intervalMinutes);
            if (plan.notice != null) {
                result.put("notice", plan.notice);
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
                        // P2: 重新启用时按 cron 重算下次触发时间
                        long delay = 0;
                        try {
                            delay = CronScheduler.plan(
                                    job.getString("cron"), System.currentTimeMillis())
                                    .initialDelayMinutes;
                        } catch (IllegalArgumentException ignored) {}
                        scheduleWork(jobId, job.getLong("intervalMin"), delay);
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
    private void scheduleWork(String jobId, long intervalMinutes, long initialDelayMinutes) {
        // WorkManager 最小周期 15 分钟
        long interval = Math.max(15, intervalMinutes);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                HermesCronWorker.class, interval, TimeUnit.MINUTES)
                // P2: 按 cron 表达式计算下次触发时间作为 initialDelay
                .setInitialDelay(Math.max(0, initialDelayMinutes), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("hermes_cron")
                .addTag("job_" + jobId)
                .build();

        String uniqueName = "hermes_cron_" + jobId;
        // P2: UPDATE 策略会保留旧调度, 先取消再入队, 保证新的 initialDelay 生效
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName);
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE,
                work);

        Log.i(TAG, "scheduled cron job " + jobId + " every " + interval
                + "min, first in " + Math.max(0, initialDelayMinutes) + "min");
    }
}
