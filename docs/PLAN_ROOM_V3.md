# IMPLEMENTATION: 房间 v3.0 — 项目仓库模型

版本: v1.0
日期: 2026-07-21
状态: ✅ 待实施
前置设计: [DESIGN_ROOM_V3.md](DESIGN_ROOM_V3.md)

---

## 0. 实施顺序与依赖

```
S1: Java 文件能力  (底层, 无依赖)
  └─ S2: Java 桥方法 (依赖 S1)
       └─ S3: JS 桥封装 (依赖 S2)
            ├─ S4: JS 文件树 + 卡片组件 (依赖 S3)
            ├─ S5: HTML 房间子 tab + 文件视图 (依赖 S4)
            └─ S6: 新建房间流程更新 (依赖 S3+S5)
```

**规则**: 先 Java 后前端。每步完成即可独立测试，不依赖后续步骤。

---

## S1: Java — 4 个文件能力

### 文件: `CapabilityExecutor.java`

在 `doExecute()` 的 switch 中新增 4 个 case:

```java
case "file.write":  return doFileWrite(ctx, cmd);
case "file.read":   return doFileRead(cmd);
case "file.delete": return doFileDelete(cmd);
case "file.mkdir":  return doFileMkdir(cmd);
```

### S1.1 `file.write`

```java
/**
 * file.write — 写内容到房间文件。
 * args: roomId (String), path (String), content (String)
 * 磁盘路径: /sdcard/mov/rooms/<roomId>/<path>
 * 自动创建父目录。
 * 返回: 文件路径 + 大小
 */
private CommandResult doFileWrite(Context ctx, ParsedCommand cmd) {
    String roomId = cmd.getStringArg("roomId", "");
    String path = cmd.getStringArg("path", "");
    String content = cmd.getStringArg("content", "");
    if (roomId.isEmpty() || path.isEmpty()) {
        return CommandResult.fail("需要 roomId 和 path");
    }
    try {
        File base = new File("/sdcard/mov/rooms/" + roomId);
        File target = new File(base, path);
        // 安全检查: 路径不能逃逸出房间目录
        if (!target.getCanonicalPath().startsWith(base.getCanonicalPath())) {
            return CommandResult.fail("路径越界");
        }
        target.getParentFile().mkdirs();
        try (java.io.FileWriter fw = new java.io.FileWriter(target)) {
            fw.write(content);
        }
        return CommandResult.ok("✅ 已写入: " + path + " (" + target.length() + " bytes)");
    } catch (Exception e) {
        return CommandResult.fail("写入失败: " + e.getMessage());
    }
}
```

**验收**: 执行 `file.write roomId=r12345 path=test.md content=hello` → `/sdcard/mov/rooms/r12345/test.md` 存在且内容为 "hello"。

### S1.2 `file.read`

```java
/**
 * file.read — 读房间文件内容。
 * args: roomId (String), path (String)
 * 返回: 文件内容 (纯文本, 最大 100KB)
 */
private CommandResult doFileRead(ParsedCommand cmd) {
    String roomId = cmd.getStringArg("roomId", "");
    String path = cmd.getStringArg("path", "");
    if (roomId.isEmpty() || path.isEmpty()) {
        return CommandResult.fail("需要 roomId 和 path");
    }
    try {
        File base = new File("/sdcard/mov/rooms/" + roomId);
        File target = new File(base, path);
        if (!target.getCanonicalPath().startsWith(base.getCanonicalPath())) {
            return CommandResult.fail("路径越界");
        }
        if (!target.exists()) return CommandResult.fail("文件不存在: " + path);
        if (target.length() > 100 * 1024) return CommandResult.fail("文件过大 (>100KB)");
        String content = new String(java.nio.file.Files.readAllBytes(target.toPath()));
        return CommandResult.ok(content);
    } catch (Exception e) {
        return CommandResult.fail("读取失败: " + e.getMessage());
    }
}
```

### S1.3 `file.delete`

```java
/**
 * file.delete — 删除房间文件。
 * args: roomId (String), path (String)
 * 只能删文件, 不能删目录 (安全)。
 */
private CommandResult doFileDelete(ParsedCommand cmd) {
    String roomId = cmd.getStringArg("roomId", "");
    String path = cmd.getStringArg("path", "");
    if (roomId.isEmpty() || path.isEmpty()) {
        return CommandResult.fail("需要 roomId 和 path");
    }
    try {
        File base = new File("/sdcard/mov/rooms/" + roomId);
        File target = new File(base, path);
        if (!target.getCanonicalPath().startsWith(base.getCanonicalPath())) {
            return CommandResult.fail("路径越界");
        }
        if (!target.exists()) return CommandResult.fail("文件不存在");
        if (target.isDirectory()) return CommandResult.fail("不可删除目录");
        target.delete();
        return CommandResult.ok("✅ 已删除: " + path);
    } catch (Exception e) {
        return CommandResult.fail("删除失败: " + e.getMessage());
    }
}
```

### S1.4 `file.mkdir`

```java
/**
 * file.mkdir — 在房间内创建目录。
 * args: roomId (String), path (String)
 */
private CommandResult doFileMkdir(ParsedCommand cmd) {
    String roomId = cmd.getStringArg("roomId", "");
    String path = cmd.getStringArg("path", "");
    if (roomId.isEmpty() || path.isEmpty()) {
        return CommandResult.fail("需要 roomId 和 path");
    }
    try {
        File base = new File("/sdcard/mov/rooms/" + roomId);
        File target = new File(base, path);
        if (!target.getCanonicalPath().startsWith(base.getCanonicalPath())) {
            return CommandResult.fail("路径越界");
        }
        target.mkdirs();
        return CommandResult.ok("✅ 已创建目录: " + path);
    } catch (Exception e) {
        return CommandResult.fail("创建目录失败: " + e.getMessage());
    }
}
```

### S1.5 更新 IntentParser — 文件指令入口

在 `IntentParser.parse()` 中追加 (放到现有的 `file.ls` 附近):

```java
// File write — "写文件 index.html 内容 <html>..."
if (containsAny(compact, "写文件", "创建文件", "写入文件")) {
    String path = extractAfter(text, "写文件", "创建文件", "写入文件");
    String content = extractAfter(path, "内容", "正文");
    // ... 根据实际匹配提取 roomId, path, content
    // 注意: roomId 需从上下文获取, 指令入口主要给 debug 用
    return new ParsedCommand("file.write")
        .arg("roomId", "")
        .arg("path", path.trim())
        .arg("content", content);
}
```

**简化处理**: 指令入口的 `roomId` 可留空, 实际调用时由 JS 侧填充当前房间 ID。此能力主要供 `B.writeFile()` JS 桥使用，不要求用户手动输入 roomId。

---

## S2: Java — 桥方法 + 房间初始化

### 文件: `MOVActivity.java` → `MOVBridge` 内部类

新增 4 个 `@JavascriptInterface`:

```java
@JavascriptInterface
public String writeFile(String roomId, String path, String content) {
    ParsedCommand cmd = new ParsedCommand("file.write")
        .arg("roomId", roomId)
        .arg("path", path)
        .arg("content", content);
    CommandResult r = capabilityExecutor.execute(MOVActivity.this, cmd);
    try {
        return new JSONObject()
            .put("ok", r.isSuccess())
            .put("message", r.getMessage()).toString();
    } catch (Exception e) { return "{\"ok\":false}"; }
}

@JavascriptInterface
public String readFile(String roomId, String path) {
    ParsedCommand cmd = new ParsedCommand("file.read")
        .arg("roomId", roomId).arg("path", path);
    CommandResult r = capabilityExecutor.execute(MOVActivity.this, cmd);
    try {
        JSONObject o = new JSONObject();
        o.put("ok", r.isSuccess());
        if (r.isSuccess()) {
            o.put("content", r.getMessage());
        } else {
            o.put("error", r.getMessage());
        }
        return o.toString();
    } catch (Exception e) { return "{\"ok\":false}"; }
}

@JavascriptInterface
public String deleteFile(String roomId, String path) {
    ParsedCommand cmd = new ParsedCommand("file.delete")
        .arg("roomId", roomId).arg("path", path);
    CommandResult r = capabilityExecutor.execute(MOVActivity.this, cmd);
    try {
        return new JSONObject()
            .put("ok", r.isSuccess())
            .put("message", r.getMessage()).toString();
    } catch (Exception e) { return "{\"ok\":false}"; }
}

@JavascriptInterface
public String listRoomFiles(String roomId, String subPath) {
    // 复用 file.ls 能力
    String fullPath = "/sdcard/mov/rooms/" + roomId +
        (subPath != null && !subPath.isEmpty() ? "/" + subPath : "");
    ParsedCommand cmd = new ParsedCommand("file.ls").arg("path", fullPath);
    CommandResult r = capabilityExecutor.execute(MOVActivity.this, cmd);
    try {
        JSONObject o = new JSONObject();
        o.put("ok", r.isSuccess());
        if (r.isSuccess()) {
            o.put("files", parseFileList(r.getMessage())); // 解析 ls 输出
        }
        return o.toString();
    } catch (Exception e) { return "{\"ok\":false}"; }
}
```

### 房间初始化

在 `MOVActivity.java` 或新建 `RoomManager.java`:

```java
/**
 * 创建房间文件目录 + README
 * 由 JS 侧新建房间时通过桥调用
 */
@JavascriptInterface
public String initRoom(String roomId, String name, String description, String membersJson) {
    try {
        File base = new File("/sdcard/mov/rooms/" + roomId);
        base.mkdirs();
        new File(base, ".hermes").mkdir();

        // README.md
        String readme = "# " + name + "\n\n" + description + "\n\n"
            + "## 成员\n\n" + membersJson + "\n\n"
            + "## 文件\n\n项目文件存储在此目录下。\n";
        try (java.io.FileWriter fw = new java.io.FileWriter(new File(base, "README.md"))) {
            fw.write(readme);
        }

        // .hermes/config.json
        JSONObject config = new JSONObject();
        config.put("name", name);
        config.put("description", description);
        config.put("members", new JSONArray(membersJson));
        config.put("created", System.currentTimeMillis());
        try (java.io.FileWriter fw = new java.io.FileWriter(new File(base, ".hermes/config.json"))) {
            fw.write(config.toString(2));
        }

        return "{\"ok\":true}";
    } catch (Exception e) {
        return "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}";
    }
}
```

**验收**: 从 JS 调用 `B.initRoom("r123", "测试", "描述", '[{"who":"you","role":"owner"}]')` → `/sdcard/mov/rooms/r123/README.md` 和 `.hermes/config.json` 存在。

---

## S3: JS — 桥封装

### 文件: `js/bridge.js`

在 B 对象中新增:

```javascript
/* 文件操作 */
writeFile: function(roomId, path, content) {
    try { return b ? JSON.parse(b.writeFile(roomId, path, content)) : {ok:false}; }
    catch(e) { return {ok:false}; }
},
readFile: function(roomId, path) {
    try { return b ? JSON.parse(b.readFile(roomId, path)) : {ok:false}; }
    catch(e) { return {ok:false}; }
},
deleteFile: function(roomId, path) {
    try { return b ? JSON.parse(b.deleteFile(roomId, path)) : {ok:false}; }
    catch(e) { return {ok:false}; }
},
listRoomFiles: function(roomId, subPath) {
    try { return b ? JSON.parse(b.listRoomFiles(roomId, subPath||'')) : {ok:false,files:[]}; }
    catch(e) { return {ok:false,files:[]}; }
},
initRoom: function(roomId, name, desc, members) {
    try { return b ? JSON.parse(b.initRoom(roomId, name, desc, JSON.stringify(members))) : {ok:false}; }
    catch(e) { return {ok:false}; }
},
```

---

## S4: JS — 文件树组件

### 新建文件: `js/files.js`

```javascript
/* ============================================================
   files.js — 房间文件 tab: 文件树 + 操作
   ============================================================ */

var _filesPath = '';  // 当前浏览的子目录

function renderFileTree(roomId) {
    var res = B.listRoomFiles(roomId, _filesPath);
    if (!res.ok) { $('fileList').innerHTML = '<div class="sysline">无法读取文件列表</div>'; return; }
    var files = res.files || [];
    var h = '';
    // 如果不是根目录, 加 ".." 返回上级
    if (_filesPath) {
        h += '<div class="file-row" data-up="1">📁 ..</div>';
    }
    files.forEach(function(f) {
        var icon = f.isDir ? '📁' : '📄';
        var size = f.isDir ? '' : ' <span style="color:var(--ink-4);font-size:10px">'+formatFileSize(f.size)+'</span>';
        var author = f.author || '';
        h += '<div class="file-row" data-file="'+esc(f.name)+'" data-dir="'+(f.isDir?'1':'0')+'">'
            + icon + ' ' + esc(f.name) + size
            + '<span style="margin-left:auto;font-size:9px;color:var(--ink-4)">'+esc(author)+'</span>'
            + '</div>';
    });
    if (files.length === 0) h = '<div class="sysline">空目录</div>';
    $('fileList').innerHTML = h;

    // 绑定点击
    document.querySelectorAll('#fileList .file-row').forEach(function(el) {
        bindLongPress(el, {
            text: el.getAttribute('data-dir') === '1' ? '打开目录' : '操作文件',
            exec: function() {
                var name = el.getAttribute('data-file');
                var isDir = el.getAttribute('data-dir') === '1';
                if (isDir) {
                    _filesPath = _filesPath ? _filesPath + '/' + name : name;
                    renderFileTree(roomId);
                } else {
                    showFileActions(roomId, name);
                }
            }
        });
        // 单击 = 进入目录 或 预览文件
        el.addEventListener('click', function() {
            var name = el.getAttribute('data-file');
            var isDir = el.getAttribute('data-dir') === '1';
            if (isDir || el.getAttribute('data-up')) {
                if (el.getAttribute('data-up')) {
                    _filesPath = _filesPath.substring(0, _filesPath.lastIndexOf('/'));
                    if (_filesPath === '') _filesPath = '';
                } else {
                    _filesPath = _filesPath ? _filesPath + '/' + name : name;
                }
                renderFileTree(roomId);
            } else {
                // 预览文件
                var res = B.readFile(roomId, (_filesPath ? _filesPath + '/' : '') + name);
                if (res.ok) {
                    showFilePreview(name, res.content);
                } else {
                    B.toast('无法读取: ' + (res.error || ''));
                }
            }
        });
    });
}

function showFileActions(roomId, fileName) {
    // 底部操作 sheet: 重命名 / 导出 / 删除
    // 复用已有的房间操作 sheet 风格
    // TODO: 实现 fileOps sheet
}

function showFilePreview(name, content) {
    // 在聊天区展示文件内容 (代码高亮暂不做, 纯文本)
    var h = '<div class="sysline">📄 ' + esc(name) + '</div>'
        + '<div class="msg wide"><div class="bubble" style="font-family:var(--font-mono);font-size:11px;white-space:pre-wrap;max-height:300px;overflow:auto">'
        + esc(content)
        + '</div></div>';
    var b = $('chatBody');
    b.innerHTML += h;
    b.scrollTop = b.scrollHeight;
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + 'B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB';
    return (bytes / 1048576).toFixed(1) + 'MB';
}

// 路径导航栏
function renderFilePathBar(roomId) {
    var parts = _filesPath ? _filesPath.split('/') : [];
    var h = '<span class="file-crumb" data-path="">' + esc(roomId) + '</span>';
    var accumulated = '';
    parts.forEach(function(p) {
        accumulated = accumulated ? accumulated + '/' + p : p;
        h += ' / <span class="file-crumb" data-path="'+esc(accumulated)+'">'+esc(p)+'</span>';
    });
    $('filePathBar').innerHTML = h;
    document.querySelectorAll('#filePathBar .file-crumb').forEach(function(el) {
        el.addEventListener('click', function() {
            _filesPath = el.getAttribute('data-path') || '';
            renderFileTree(roomId);
        });
    });
}
```

---

## S5: HTML — 房间内子 tab + 文件视图

### 文件: `hermes-shell.html`

在 `view-room` 的顶栏下面, 聊天区上面, 插入子 tab 切换:

```html
<!-- 房间内子 tab -->
<div class="room-tabs" id="roomTabs">
  <span class="room-tab on" data-subtab="chat">讨论</span>
  <span class="room-tab" data-subtab="files">文件</span>
</div>
```

在 `view-room` 内, 聊天区 (`#chatBody`) 旁边, 新增文件视图:

```html
<!-- 文件视图 -->
<div class="file-view" id="fileView" style="display:none">
  <div class="file-pathbar" id="filePathBar"></div>
  <div class="file-list" id="fileList"></div>
  <div class="file-fab" id="fileFabAdd">＋</div>
</div>
```

### CSS 新增 (加到 `css/shell.css`)

```css
/* 房间内子 tab */
.room-tabs{display:flex;padding:8px 12px;gap:4px;border-bottom:1px solid var(--line-soft);background:var(--panel);}
.room-tab{font-family:var(--font-mono);font-size:11px;font-weight:600;padding:6px 14px;border-radius:var(--r-sm);cursor:pointer;color:var(--ink-3);transition:all var(--t-fast);}
.room-tab:hover{color:var(--ink-1);background:var(--panel-2);}
.room-tab.on{color:var(--ink-1);background:var(--acc-tint-2);border:1px solid var(--acc-live);}

/* 文件视图 */
.file-view{flex:1;display:flex;flex-direction:column;background:var(--bg);}
.file-pathbar{padding:8px 12px;font-family:var(--font-mono);font-size:10px;color:var(--ink-3);border-bottom:1px solid var(--line-soft);background:var(--panel);}
.file-crumb{color:var(--acc);cursor:pointer;transition:color var(--t-fast);}
.file-crumb:hover{color:var(--acc-strong);text-decoration:underline;}
.file-list{flex:1;overflow-y:auto;padding:8px 0;}
.file-row{display:flex;align-items:center;gap:6px;padding:9px 12px;font-family:var(--font-mono);font-size:11px;cursor:pointer;transition:background var(--t-fast);border-bottom:1px solid var(--line-soft);}
.file-row:hover{background:var(--panel);}
.file-fab{position:absolute;right:14px;bottom:14px;width:40px;height:40px;border-radius:12px;background:var(--ink-1);color:#fff;border:none;font-family:var(--font-mono);font-size:18px;cursor:pointer;box-shadow:var(--sh-3);transition:all var(--t-fast);z-index:5;}
```

### JS 子 tab 切换逻辑 (加在 `js/render.js` 或 `js/app.js`)

```javascript
var curSubtab = 'chat';

function setSubtab(t) {
    curSubtab = t;
    document.querySelectorAll('.room-tab').forEach(function(el) {
        el.classList.toggle('on', el.getAttribute('data-subtab') === t);
    });
    $('chatBody').parentElement.style.display = (t === 'chat') ? '' : 'none';
    $('chat-foot').style.display = (t === 'chat') ? '' : 'none';
    $('fileView').style.display = (t === 'files') ? '' : 'none';
    if (t === 'files' && curRoomId) {
        _filesPath = '';
        renderFilePathBar(curRoomId);
        renderFileTree(curRoomId);
    }
}

document.querySelectorAll('.room-tab').forEach(function(el) {
    el.addEventListener('click', function() {
        setSubtab(el.getAttribute('data-subtab'));
    });
});
```

---

## S6: 新建房间流程更新

### 文件: `js/app.js` — `btnCreate` 处理

在现有创建逻辑后, 追加文件初始化:

```javascript
$('btnCreate').addEventListener('click', function() {
    // ... 现有逻辑: 生成 room 对象, ROOMS.splice, renderRooms, enterRoom ...

    // 新增: 初始化房间文件目录
    var members = [
        {who: 'you', role: 'owner'}  // 你
    ];
    // 添加 AI 成员
    if (newMode === 'council') {
        selectedAi.forEach(function(ai) {  // selectedAi 从勾选列表获取
            members.push({who: ai, role: ai});
        });
    }
    // 添加人类成员
    selectedPeople.forEach(function(p) {
        members.push({who: p.who, role: p.role});
    });

    B.initRoom(id, name, '', members);
    ev('初始化房间文件: ' + id);
});
```

---

## S7: 文件卡片 — AI 产出在讨论区的展示

### 文件: `js/render.js`

新增 `mkFileCard()`:

```javascript
function mkFileCard(fileName, filePath, size, author, roomId) {
    var d = document.createElement('div');
    d.className = 'msg wide';
    d.innerHTML = '<div class="file-card">'
        + '<div class="fc-icon">📄</div>'
        + '<div class="fc-info">'
        + '<div class="fc-name">' + esc(fileName) + '</div>'
        + '<div class="fc-meta">' + esc(size) + ' · ' + esc(author) + '</div>'
        + '</div>'
        + '<div class="fc-actions">'
        + '<span class="fc-btn" data-action="view">查看</span>'
        + '<span class="fc-btn" data-action="download">下载</span>'
        + '</div>'
        + '</div>';
    // 绑定查看: 读取文件并在聊天区预览
    d.querySelector('[data-action="view"]').addEventListener('click', function() {
        var res = B.readFile(roomId, filePath);
        if (res.ok) {
            var preview = document.createElement('div');
            preview.className = 'msg wide';
            preview.innerHTML = '<div class="bubble" style="font-family:var(--font-mono);font-size:11px;white-space:pre-wrap;max-height:300px;overflow:auto">'
                + esc(res.content) + '</div>';
            var b = $('chatBody');
            b.appendChild(preview);
            b.scrollTop = b.scrollHeight;
        }
    });
    return d;
}
```

CSS:

```css
.file-card{display:flex;align-items:center;gap:10px;padding:10px 12px;background:var(--panel);border:1px solid var(--line);border-left:3px solid var(--acc-live);border-radius:var(--r-md);width:100%;}
.file-card .fc-icon{font-size:20px;flex:none;}
.file-card .fc-info{flex:1;}
.file-card .fc-name{font-family:var(--font-mono);font-size:11px;font-weight:600;}
.file-card .fc-meta{font-family:var(--font-mono);font-size:9px;color:var(--ink-4);margin-top:2px;}
.file-card .fc-actions{display:flex;gap:6px;flex:none;}
.file-card .fc-btn{font-family:var(--font-mono);font-size:10px;color:var(--acc);cursor:pointer;padding:4px 8px;border:1px solid var(--acc-live);border-radius:var(--r-sm);transition:all var(--t-fast);}
.file-card .fc-btn:hover{background:var(--acc-tint-2);}
```

---

## 8. 完整任务清单 (供另一位程序员使用)

| # | 步骤 | 文件 | 新建/修改 | 验收方式 |
|---|------|------|----------|---------|
| S1.1 | `doFileWrite` | `CapabilityExecutor.java` | 修改 | ADB: `file.write roomId=test path=a.txt content=hi` → `/sdcard/mov/rooms/test/a.txt` 存在 |
| S1.2 | `doFileRead` | `CapabilityExecutor.java` | 修改 | ADB: 读回 a.txt 内容 |
| S1.3 | `doFileDelete` | `CapabilityExecutor.java` | 修改 | ADB: 删掉 a.txt |
| S1.4 | `doFileMkdir` | `CapabilityExecutor.java` | 修改 | ADB: 创建目录 |
| S1.5 | 更新 `doExecute` switch + IntentParser | `CapabilityExecutor.java`, `IntentParser.java` | 修改 | 指令解析正确 |
| S2.1 | 4 个文件桥方法 + initRoom | `MOVActivity.java` MOVBridge | 修改 | JS 可调用 |
| S2.2 | `listRoomFiles` 返回 JSON | `MOVActivity.java` MOVBridge | 修改 | JS 调 B.listRoomFiles → 返回文件数组 |
| S3 | B 对象封装 | `js/bridge.js` | 修改 | console 测试 |
| S4 | 文件树 + 预览组件 | `js/files.js` | **新建** | 浏览器开发者工具测试 |
| S5.1 | HTML 子 tab + 文件视图 | `hermes-shell.html` | 修改 | 视觉检查 |
| S5.2 | CSS 子 tab + 文件视图 + 文件卡片 | `css/shell.css` | 修改 | 视觉检查 |
| S5.3 | JS 子 tab 切换 + 文件视图驱动 | `js/render.js` 或 `js/app.js` | 修改 | 点击"文件" tab → 文件树出现 |
| S6 | 新建房间初始化文件目录 | `js/app.js` | 修改 | 新建房间 → `/sdcard/mov/rooms/<id>/` 存在 |
| S7 | 文件卡片 + CSS | `js/render.js`, `css/shell.css` | 修改 | AI 产出文件时讨论区出现卡片 |
| — | 加 `files.js` 到 HTML script 加载 | `hermes-shell.html` | 修改 | `<script src="js/files.js">` 放在 runtime.js 之后 |
| — | i18n 新增文件相关 key | `js/i18n.js` | 修改 | 约 10 条 |
