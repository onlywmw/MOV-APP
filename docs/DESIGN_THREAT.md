# DESIGN: 威胁模型

版本: v1.0
日期: 2026-07-22

---

## 五个攻击面

### 1. API Key 泄露 — 严重度: 高

**攻击路径**: 设备 root → 读 `/data/data/com.hermes.android/shared_prefs/mov_models.xml` → 明文 Key。

**当前**: EncryptedSharedPreferences 加密存储（ModelRegistry 已做）。降级明文只在加密不可用时触发。

**缺口**: 降级明文无警告。用户不知道 Key 在以明文存储。

**缓解**: 加密不可用时不降级——直接弹 toast 告知用户"加密存储不可用，无法保存 API Key"。

---

### 2. JS 桥暴露面 — 严重度: 高

**攻击路径**: WebView 加载恶意内容 → 调用 `window.HermesBridge.writeFile("../../etc/hosts", "127.0.0.1 google.com")` → 文件写入系统路径。

**当前**: `StorageManager.isSafe()` 做了路径逃逸检查。`CapabilityExecutor.doFileWrite()` 也检查了。

**缺口**: 
- 30+ 桥方法，没有统一的参数校验层
- `writeFile` 的 content 参数无大小限制——1GB 内容可以写爆磁盘
- `execCommand` 接收任意字符串 → `IntentParser.parse()` → 执行任意设备指令

**缓解**:
1. 所有桥方法加参数校验：path 不能含 `..`、content 限制 10MB、roomId 必须匹配 `[a-zA-Z0-9_-]+`
2. `execCommand` 加危险指令黑名单：`打电话`、`发短信` 需要用户二次确认
3. iframe 只加载 `assets/board-apps/` 下的本地文件，不加载远程 URL（看板应用限制）

---

### 3. Cron 执行 AI 生成的指令 — 严重度: 高

**攻击路径**: Council 讨论中 AI 被 prompt injection → 输出恶意步骤 → Cron 自动执行。

例如：
```
用户在聊天里贴了一段网页内容（含隐藏的 prompt injection）
→ AI 汇总时被注入 → 输出 {"action":"file.write","target":"/sdcard/.evil.sh","detail":"rm -rf /"}
→ Cron 每天执行 → 持续损害
```

**当前**: 没有防护。Cron 执行 `IntentParser.parse()` → `CapabilityExecutor.execute()` 的完整链路，没有任何 AI 输出校验。

**缓解**:
1. Cron 执行前，步骤必须经过用户审批（当前"手动审批"模式是默认）
2. AI 输出的 `action` 字段加白名单——只允许 `file.write` / `notification.post` / `tts.speak`，不允许 `input.tap` / `input.swipe` / 任何 shell 命令
3. `file.write` 的 `target` 强制限制在 `/sdcard/mov/rooms/<roomId>/files/` 下，且只能写 `.md` `.txt` `.json` `.html` `.tsx` `.ts` `.js` `.css` 白名单后缀

---

### 4. 文件路径遍历 — 严重度: 中

**攻击路径**: 用户添加的看板应用中 iframe 通过 JS 桥写入恶意路径。

**当前**: `isSafe()` 检查。`listRoomFiles` 过滤 `.mov` 隐藏目录。

**缺口**: 新加的 SQLite `dbExec` 桥方法如果拼接字符串可能 SQL 注入。

**缓解**: `dbExec` 用参数化查询（`?` 占位符），Java 层 `SQLiteDatabase.rawQuery` 不接受拼接。

---

### 5. 数据泄漏 — 严重度: 中

**攻击路径**:
- APK 反编译 → 拿到 Supabase anon key → 灌假数据
- ADB 备份 → 拿到整个应用数据
- 旧设备出售 → 数据未清除

**当前**: 遥测系统设计了但端点未配置。Supabase key 未写入。

**缓解**:
1. 砍掉遥测（用户量没到需要的时候）
2. AndroidManifest 加 `android:allowBackup="false"`
3. 设备出售/重置：`/sdcard/mov/` 物理删除（手动操作即可）

---

## 缓解优先级

| # | 措施 | 工作量 | 立即做？ |
|---|------|--------|---------|
| 1 | API Key 加密不降级 | 3行 | ✅ |
| 2 | 桥方法统一参数校验 | 30行 | ✅ |
| 3 | iframe 只加载本地文件 | 5行 | ✅ |
| 4 | Cron 步骤白名单 + 审批 | 20行 | ✅ |
| 5 | `dbExec` 参数化查询 | 设计已包含 | ✅ |
| 6 | 砍掉遥测 | 删文件 | ⚠ 改天 |
| 7 | `allowBackup="false"` | 1行 | ✅ |
