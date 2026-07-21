# DESIGN: 房间文件系统修复 v1.0

版本: v1.0
日期: 2026-07-21
状态: 📐 design-ready
前置: [PLAN_ROOM_V3.md](PLAN_ROOM_V3.md)

---

## 问题清单

| # | 严重度 | 问题 | 用户感知 |
|---|--------|------|---------|
| 1 | 🔴 | 文件上传是空操作 | 选文件 → toast "已上传" → 文件没出现 |
| 2 | 🔴 | 文件预览在隐藏 DOM 里 | 点"查看" → 内容写到了看不见的聊天区 |
| 3 | 🔴 | `_filesPath` 房间切换不重置 | 从房间A切到房间B → 文件路径停留在A的子目录 |
| 4 | ⚠️ | doHelp 未更新 | 帮助列表少了文件能力 |
| 5 | ⚠️ | 缺新建文件入口 | 用户无法创建空白文件 |

---

## Fix 1: 文件上传

### 问题

`fileFabAction()` 调 `B.pickFile()` 只获取文件元信息（uri, name, size），未读取文件内容写入房间目录。

### 方案

新增 Java 方法 `copyFileToRoom(roomId, uri)` —— 从 ContentResolver 读取 URI 内容流，写入房间目录。

#### Java: CapabilityExecutor.java

```java
/**
 * 从设备文件选择器的 URI 复制文件到房间目录。
 * 由 pickFile 回调触发，不在指令解析中暴露。
 */
private void copyFileToRoom(String roomId, Uri uri) {
    try {
        String name = "unknown";
        // 读文件名
        try (android.database.Cursor c = getContentResolver().query(
                uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        }
        java.io.File base = new java.io.File(ROOMS_BASE + roomId);
        java.io.File target = new java.io.File(base, name);
        try (java.io.InputStream is = getContentResolver().openInputStream(uri);
             java.io.FileOutputStream os = new java.io.FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
        }
    } catch (Exception e) {
        Log.w(TAG, "copyFileToRoom: " + e.getMessage());
    }
}
```

#### Java: MOVActivity.MOVBridge

修改 `pickFile` 的回调处理。当前是:

```java
public void pickFile(String callbackId) {
    pendingFileCallbackId = callbackId;
    // launches file picker...
}
```

文件选择器回调在 `onCreate` 中的 `filePickerLauncher`。修改回调:

```java
filePickerLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(), result -> {
        if (pendingFileCallbackId == null) return;
        String cbId = pendingFileCallbackId;
        pendingFileCallbackId = null;
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                // 如果当前在房间内，复制文件到房间目录
                // 通过 callback 通知 JS 已完成
                String roomId = getCurrentRoomIdJs(); // 需要新增获取
                if (roomId != null && !roomId.isEmpty()) {
                    capabilityExecutor.copyFileToRoom(roomId, uri);
                }
                String info = getFileInfoJson(uri);
                evalJs("window._hermesCb('" + cbId + "'," + info + ")");
                return;
            }
        }
        evalJs("window._hermesCb('" + cbId + "',null)");
    });
```

**简化方案**: 不修改 Java——在 JS 侧 `fileFabAction` 的回调里，拿到 `info.uri` 后，目前 Java 没有暴露 `readUriContent` 桥方法。更简单的做法是让 JS 侧通知 Java 侧 "当前房间 ID"，让 pickFile 回调自动复制。

**最简方案**: 给 `pickFile` 传 roomId:

```java
// 桥方法签名改为: pickFile(String callbackId, String roomId)
// 回调时自动复制到房间目录
@JavascriptInterface
public void pickFile(String callbackId, String roomId) {
    pendingFileCallbackId = callbackId;
    pendingFileRoomId = roomId;  // 新增字段
    uiHandler.post(() -> { ... });
}
```

在 `filePickerLauncher` 回调中:

```java
// 复制文件到房间
if (pendingFileRoomId != null && !pendingFileRoomId.isEmpty()) {
    capabilityExecutor.copyFileToRoom(pendingFileRoomId, uri);
}
pendingFileRoomId = null;
```

#### JS: bridge.js

```javascript
// 签名为 pickFile(callback, roomId) 可选第二个参数
pickFile: function(cb, roomId) {
    if (!b) { cb(null); return; }
    var id = nextCbId(); _cbMap[id] = cb;
    b.pickFile(id, roomId || '');
},
```

#### JS: files.js

```javascript
function fileFabAction(roomId) {
    B.pickFile(function(info) {
        if (!info) return;
        B.toast(t('files.uploaded') + ' ' + info.name);
        renderFileTree(roomId);
    }, roomId);  // ← 传入 roomId
}
```

**验收**: 在文件 tab 点 ＋ → 选一个文件 → toast "已上传" → 文件出现在树中 → 点击可查看内容。

---

## Fix 2: 文件预览可见

### 问题

`showFilePreview()` 把内容写进 `#chatBody`，但在文件 tab 下 `#chatPane` 是隐藏的。

### 方案

文件预览改为 **overlay 弹窗**——浮在文件视图上方，点击外部或 ✕ 关闭。

#### HTML: hermes-shell.html

在 `view-room` 内，`fileView` 之后，新增:

```html
<!-- 文件预览 overlay -->
<div class="preview-mask" id="previewMask" style="display:none"></div>
<div class="preview-overlay" id="previewOverlay" style="display:none">
  <div class="preview-bar">
    <span class="preview-name" id="previewName"></span>
    <span class="sheet-close" id="previewClose">✕</span>
  </div>
  <div class="preview-body" id="previewBody"></div>
</div>
```

#### CSS: shell.css

```css
.preview-mask{position:absolute;inset:0;background:rgba(9,9,11,0.5);z-index:29;}
.preview-overlay{position:absolute;inset:20px;background:var(--panel);border-radius:var(--r-lg);z-index:30;display:flex;flex-direction:column;overflow:hidden;box-shadow:var(--sh-3);}
.preview-bar{display:flex;align-items:center;justify-content:space-between;padding:10px 14px;border-bottom:1px solid var(--line-soft);flex:none;}
.preview-name{font-family:var(--font-mono);font-size:11px;font-weight:600;}
.preview-body{flex:1;overflow:auto;padding:14px;font-family:var(--font-mono);font-size:11px;line-height:1.7;white-space:pre-wrap;word-break:break-word;}
```

#### JS: files.js

```javascript
function showFilePreview(name, content) {
    $('previewName').textContent = name;
    $('previewBody').textContent = content;
    $('previewMask').style.display = '';
    $('previewOverlay').style.display = '';
}

function closeFilePreview() {
    $('previewMask').style.display = 'none';
    $('previewOverlay').style.display = 'none';
}
```

#### JS: app.js (事件绑定)

```javascript
$('previewClose').addEventListener('click', closeFilePreview);
$('previewMask').addEventListener('click', closeFilePreview);
```

**验收**: 在文件 tab 点文件 → overlay 弹出显示文件内容 → 点 ✕ 或遮罩关闭。

---

## Fix 3: `_filesPath` 房间切换重置

### 问题

全局 `_filesPath` 不随房间切换清零。

### 方案

在 `enterRoom()` 中重置。

#### JS: chat.js enterRoom 函数末尾

```javascript
// 重置文件浏览路径
_filesPath = '';
```

**验收**: 进入房间A → 切到文件tab → 进入子目录 → 返回列表 → 进入房间B → 切文件tab → 路径显示 `~`（根目录）。

---

## Fix 4: doHelp 更新

### Java: CapabilityExecutor.java

在 `doHelp()` 返回的帮助文本末尾追加:

```java
"📁 文件：写文件 xxx 内容 yyy / 读文件 xxx / 删文件 xxx\n" +
"📂 目录：创建目录 xxx"
```

**验收**: 在 desk 房间输入"帮助" → 看到文件操作说明。

---

## Fix 5: 新建文件入口

### 方案

文件 tab 的 ＋ 按钮点击 → sheet 弹出，输入文件名和内容 → 确认 → `B.writeFile()`。

#### HTML: hermes-shell.html

在 `view-room` 内，`fileView` 之后，复用新建房间 sheet 的风格:

```html
<!-- 新建文件 sheet -->
<div id="fileNewMask" class="sheet-mask" style="z-index:11"></div>
<div class="sheet" id="fileNewSheet" style="z-index:12">
  <div style="display:flex;align-items:center;justify-content:space-between">
    <h5 data-i18n="files.new">新建文件</h5>
    <span class="sheet-close" id="btnFileNewClose">✕</span>
  </div>
  <input id="fileNewName" data-i18n-ph="files.newName" placeholder="文件名 · 如: notes.md" style="...">
  <textarea id="fileNewContent" data-i18n-ph="files.newContent" placeholder="文件内容 (选填)" 
    style="width:100%;height:120px;margin:8px 0;border:1px solid var(--line);border-radius:var(--r-md);
    padding:10px;font-family:var(--font-mono);font-size:12px;resize:vertical;outline:none;
    background:var(--panel-2);"></textarea>
  <button class="btn btn-acc" id="btnFileNewCreate" style="width:100%;justify-content:center" data-i18n="files.create">创建文件</button>
</div>
```

#### JS: app.js

```javascript
/* 新建文件 sheet */
function openFileNewSheet() {
    $('fileNewMask').classList.add('open');
    $('fileNewSheet').classList.add('open');
    $('fileNewName').value = '';
    $('fileNewContent').value = '';
    $('fileNewName').focus();
}
function closeFileNewSheet() {
    $('fileNewMask').classList.remove('open');
    $('fileNewSheet').classList.remove('open');
}
$('btnFileNewClose').addEventListener('click', closeFileNewSheet);
$('fileNewMask').addEventListener('click', closeFileNewSheet);

$('btnFileNewCreate').addEventListener('click', function() {
    var name = $('fileNewName').value.trim();
    if (!name) { B.toast('请输入文件名'); return; }
    var content = $('fileNewContent').value;
    var fp = (_filesPath ? _filesPath + '/' : '') + name;
    var res = B.writeFile(curRoomId, fp, content);
    if (res.ok) {
        closeFileNewSheet();
        B.toast(name + ' 已创建');
        renderFileTree(curRoomId);
    } else {
        B.toast(res.message || '创建失败');
    }
});

/* 修改 fileFabAdd: 弹出选择: 上传 或 新建 */
$('fileFabAdd').addEventListener('click', function() {
    if (!curRoomId) return;
    // 简单方案: 直接弹出选择 (用 confirm 或后续改为 sheet)
    // 当前: 点击 = 上传, 长按 = 新建
});
```

#### 简化: fileFabAdd 点击行为

```javascript
$('fileFabAdd').addEventListener('click', function() {
    if (!curRoomId) return;
    var action = confirm('＋ 添加文件\n确定 = 从设备上传\n取消 = 新建空白文件');
    if (action) {
        fileFabAction(curRoomId);      // 上传
    } else {
        openFileNewSheet();             // 新建
    }
});
```

**验收**: 点 ＋ → 选择"新建" → 填文件名和内容 → 确认 → 文件出现在树中。

---

## 涉及文件汇总

| 文件 | 改动 | 对应 Fix |
|------|------|---------|
| `CapabilityExecutor.java` | +`copyFileToRoom` | Fix 1 |
| `CapabilityExecutor.java` | doHelp 追加文件能力 | Fix 4 |
| `MOVActivity.java` | pickFile 加 roomId 参数, 回调里调 copyFileToRoom | Fix 1 |
| `bridge.js` | pickFile 加 roomId 参数 | Fix 1 |
| `files.js` | fileFabAction 传 roomId, showFilePreview 改用 overlay | Fix 1+2 |
| `chat.js` | enterRoom 末尾 `_filesPath=''` | Fix 3 |
| `hermes-shell.html` | +preview overlay, +new file sheet | Fix 2+5 |
| `shell.css` | +preview 样式, +new file sheet 样式 (复用已有) | Fix 2+5 |
| `app.js` | preview + newFile sheet 事件绑定 | Fix 2+5 |
| `i18n.js` | +`files.new` / `files.newName` / `files.newContent` / `files.create` | Fix 5 |

## 实施顺序

```
Fix 3 (1行, 无依赖) → Fix 4 (3行, 无依赖)
  → Fix 1 (Java+JS, 有依赖关系: Java桥 → JS调用)
  → Fix 2 (HTML+CSS+JS)
  → Fix 5 (HTML+JS+i18n)
```
