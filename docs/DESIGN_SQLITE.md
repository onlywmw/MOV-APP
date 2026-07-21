# DESIGN: 存储升级 — JSON → SQLite

版本: v1.0
日期: 2026-07-22

---

## 问题

`index.json` 单文件管所有元数据。200 个文件时每次写都要读全量 → 改一行 → 写回。O(n) 写。搜索暴力 grep。并发无锁。消息删除和 jsonl 追加冲突。

## 方案

Android 自带 SQLite，零额外依赖。WebView 通过 JS 桥调用。

---

## 表结构

```sql
-- 房间文件
CREATE TABLE room_files (
  id INTEGER PRIMARY KEY,
  room_id TEXT NOT NULL,
  path TEXT NOT NULL,           -- "src/Login.tsx"
  type TEXT NOT NULL,           -- "work" | "inbox" | "archive" | "template"
  size INTEGER DEFAULT 0,
  author TEXT,
  created_at INTEGER,
  modified_at INTEGER,
  UNIQUE(room_id, path)
);

-- 文件版本
CREATE TABLE file_versions (
  id INTEGER PRIMARY KEY,
  file_id INTEGER REFERENCES room_files(id),
  version INTEGER,
  author TEXT,
  message TEXT,                -- "review 修改"
  chat_link TEXT,              -- 跳回消息
  parent_version INTEGER,      -- 版本树
  is_branch INTEGER DEFAULT 0, -- 并行分支标记
  snapshot_path TEXT,          -- 快照文件路径
  created_at INTEGER
);

-- 文件锁
CREATE TABLE file_locks (
  id INTEGER PRIMARY KEY,
  file_id INTEGER REFERENCES room_files(id) UNIQUE,
  locked_by TEXT,              -- "you" | "DeepSeek"
  locked_at INTEGER,
  expires_at INTEGER           -- 超时自动释放
);

-- 聊天消息
CREATE TABLE chat_messages (
  id INTEGER PRIMARY KEY,
  room_id TEXT NOT NULL,
  timestamp INTEGER NOT NULL,
  who TEXT,
  role TEXT,
  content TEXT,
  action TEXT,                 -- "file.write" 等
  file_ref TEXT,               -- 关联文件路径
  deleted INTEGER DEFAULT 0   -- tombstone
);
CREATE INDEX idx_chat_room_date ON chat_messages(room_id, timestamp);

-- 全文搜索
CREATE VIRTUAL TABLE chat_fts USING fts5(
  room_id, content, action, file_ref,
  content=chat_messages, content_rowid=id
);
```

---

## 关键路径

### 进房间

```sql
-- 文件列表
SELECT * FROM room_files WHERE room_id = ?;

-- 今日消息
SELECT * FROM chat_messages 
WHERE room_id = ? AND timestamp > ? AND deleted = 0
ORDER BY timestamp;
```

一次查询，不需要读 JSON 文件。

### 写文件

```sql
BEGIN TRANSACTION;
-- 查锁
SELECT * FROM file_locks WHERE file_id = ? AND expires_at > ?;
-- 有锁 → 拒绝或分支
-- 无锁 → UPDATE room_files SET size=?, modified_at=? WHERE id=?;
-- 插入版本快照
INSERT INTO file_versions (...) VALUES (...);
COMMIT;
```

加锁是原子操作，解决了 JSON 方案做不到的并发控制。

### 消息删除

```sql
UPDATE chat_messages SET deleted = 1 WHERE id = ?;
```

Tombstone。不物理删除。查询永远加 `WHERE deleted = 0`。

### 搜索

```sql
SELECT * FROM chat_fts WHERE chat_fts MATCH ?;
```

FTS5 全文索引。不比 grep，不需要遍历文件。

---

## 桥方法（新增）

| Java | JS 封装 |
|------|---------|
| `dbQuery(sql, args)` | `B.db(sql, args)` |
| `dbExec(sql, args)` | `B.dbExec(sql, args)` |

泛化接口——不每个操作一个桥方法。SQL 在 JS 层构建，Java 层只做参数化执行。防止 SQL 注入：Java 层用 `SQLiteDatabase.rawQuery` + `?` 占位符。

---

## 迁移

`MigrationManager` 新增第 4 步：

```
v3: msgData 从 localStorage → chat 文件 (已完成)
v4: chat 文件 + index.json → SQLite
  1. 遍历 rooms/*/chat/*.jsonl → INSERT INTO chat_messages
  2. 遍历 rooms/*/files/.meta/index.json → INSERT INTO room_files
  3. 建 FTS5 索引
  4. 删除旧文件
```

---

## 性能

| 操作 | 之前 | 之后 |
|------|------|------|
| 进房间（200 文件） | 读 3KB JSON + 解析 | `SELECT * FROM room_files` ~5ms |
| 写文件 | 读全量 JSON → 改 → 写回 | `UPDATE` ~2ms |
| 搜"登录"（100 房间 × 1000 条） | 暴力 grep >2s | `MATCH` ~50ms |
| 消息删除 | jsonl 无法改 | `UPDATE SET deleted=1` ~1ms |

---

## 需要改的文件

| 文件 | 内容 |
|------|------|
| `StorageManager.java` | 加 DB 初始化 + `dbQuery`/`dbExec` |
| `HermesActivity.java` | 桥方法：`dbQuery` / `dbExec` |
| `js/bridge.js` | `B.db` / `B.dbExec` 封装 |
| `js/chat.js` | push → INSERT；enterRoom → SELECT |
| `js/files.js` | 文件列表 → SELECT room_files |
| `MigrationManager.java` | 加 v4 迁移步骤 |
