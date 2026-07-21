package com.hermes.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 版本迁移机制 (DESIGN_POLISH #1)。
 * 启动时检查 data_version → 按需执行迁移 → 写完成标记。
 * 迁移失败不阻塞启动，记录日志。
 */
public class MigrationManager {

    private static final String TAG = "MigrationManager";
    private static final String PREFS = "mov_meta";
    private static final String KEY_VERSION = "data_version";

    private static final Map<Integer, Runnable> MIGRATIONS = new LinkedHashMap<>();

    static {
        MIGRATIONS.put(1, MigrationManager::migrate_v1_storagePaths);
        MIGRATIONS.put(2, MigrationManager::migrate_v2_roomMembers);
        MIGRATIONS.put(3, MigrationManager::migrate_v3_msgDataToFiles);
    }

    public static void run(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int current = prefs.getInt(KEY_VERSION, 0);

        for (Map.Entry<Integer, Runnable> e : MIGRATIONS.entrySet()) {
            if (e.getKey() > current) {
                try {
                    e.getValue().run();
                    prefs.edit().putInt(KEY_VERSION, e.getKey()).apply();
                    Log.i(TAG, "Migration v" + e.getKey() + " completed");
                } catch (Exception ex) {
                    Log.w(TAG, "Migration v" + e.getKey() + " failed: " + ex.getMessage());
                    // 不阻塞启动，下次重试
                }
            }
        }
    }

    /** v1: /sdcard/mov → getExternalFilesDir (StorageManager 已有迁移逻辑，这里只做标记) */
    private static void migrate_v1_storagePaths() {
        // StorageManager.init() 内部已有 migrateIfNeeded()
        // 此处仅标记版本，避免重复检查
        Log.i(TAG, "v1: storage path migration handled by StorageManager");
    }

    /** v2: 旧房间 members 数组 → 新 {human, ai} 格式 (JS 层 roomAiMembers 已兼容，此处仅标记) */
    private static void migrate_v2_roomMembers() {
        // JS 层 roomAiMembers() 已兼容旧数组格式
        // 此处仅标记版本
        Log.i(TAG, "v2: room members format migration handled by JS compat layer");
    }

    /** v3: localStorage msgData → 文件按天存储 (JS 层已实现 appendChatMessage，此处仅标记) */
    private static void migrate_v3_msgDataToFiles() {
        // JS 层 B.appendChat / B.loadChat 已实现文件存储
        // 此处仅标记版本
        Log.i(TAG, "v3: msgData to files migration handled by JS storage layer");
    }
}
