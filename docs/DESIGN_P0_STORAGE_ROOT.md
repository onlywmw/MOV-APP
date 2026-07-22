# DESIGN: P0 — 存储根目录修正

版本: v1.0
日期: 2026-07-22
status: design-ready

---

## 问题

整个存储系统（`StorageManager.java`、`CapabilityExecutor.java`）的根路径硬编码为 `/sdcard/mov/`。

Android 11+ Scoped Storage：应用不能直接写 `/sdcard/` 下的自定义目录。当前能跑是因为旧设备从 v3.1 升级安装保留了旧权限。全新安装或系统更新后，所有文件功能（创建房间、写文件、聊天按天存储、上传资料、Cron 归档）全部不可用。

更严重的是：`/sdcard/mov/` 不受系统保护。用户用文件管理器删掉 `rooms/` 下的文件，SQLite 里的记录变成幽灵。用户卸载 APP，`/sdcard/mov/` 残留不删。用户清缓存，数据全丢。

**SQLite 方案设计得再漂亮也没用——地基是沙地。**

---

## 方案

所有 MOV 数据从 `/sdcard/mov/` 迁移到 `context.getExternalFilesDir(null)/mov/`。

`getExternalFilesDir(null)` 是 Android 官方推荐的应用专属外部存储目录：
- 不需要任何存储权限
- 卸载 APP 时自动删除
- 系统保护，用户文件管理器不可直接访问
- 路径示例：`/sdcard/Android/data/com.hermes.android/files/mov/`

### 改动

`StorageManager.java`：

```java
// 旧
private static final String BASE = "/sdcard/mov/";

// 新
private static String BASE; // 启动时由 HermesApplication 初始化

public static void init(Context context) {
    BASE = context.getExternalFilesDir(null).getAbsolutePath() + "/mov/";
    new File(BASE).mkdirs();
}
```

`CapabilityExecutor.java` 中 `ROOMS_BASE` 同理。

`HermesApplication.onCreate()` 加一行 `StorageManager.init(this)`。

### 旧数据迁移

首次启动 v4.0 时：

1. 检查新路径 `/sdcard/Android/data/com.hermes.android/files/mov/` 是否为空
2. 如果为空且旧路径 `/sdcard/mov/` 存在 → 递归复制到新路径
3. 迁移完成 → 写标记 `migrated_storage_v4: true`
4. 旧路径不删（用户可手动删）

迁移在 `MigrationManager` 中实现。

### 影响

| 文件 | 改动 |
|------|------|
| `StorageManager.java` | `BASE` 常量 → `init(Context)` 初始化 |
| `CapabilityExecutor.java` | `ROOMS_BASE` 同理 |
| `HermesApplication.java` | onCreate 调 `StorageManager.init(this)` |
| `MigrationManager.java` | 新增 v4 迁移步骤：`/sdcard/mov/` → `getExternalFilesDir` |

改动量：~20 行 Java。

---

## 验收

- [ ] Android 11+ 全新安装 → 创建房间 → 文件写入成功
- [ ] 旧用户升级 → 旧数据自动迁移到新路径
- [ ] 卸载 APP → `/sdcard/Android/data/com.hermes.android/files/mov/` 被系统自动删除
- [ ] 文件管理器看不到 MOV 的内部文件
