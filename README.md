# MOV — 多模型协作工作台

> Android 手机上的 AI 协作工具。配置多个大模型，在项目房间里让 AI 团队一起工作——讨论、写代码、产出文件。

---

## 是什么

```
会话  │  看板  │  运行
──────┼────────┼──────
AI 协作│轻应用  │系统 & 模型
项目房间│面板   │设备 & Cron
```

- **会话**：项目房间。讨论区 + 文件仓库。多个 AI 模型参与讨论，产出代码和文档。
- **看板**：轻应用桌面。音乐、阅读、健身、笔记，可自行添加。
- **运行**：设备状态、AI 模型监控、Cron 定时任务、通道状态。

---

## 快速开始

```bash
# 构建
./gradlew assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动
adb shell am start -n com.hermes.android/.HermesActivity

# 日志
adb logcat -s MOV:D

# 测试
./gradlew test
```

---

## 架构

```
HermesActivity (WebView 壳, ~900行)
  └─ hermes-shell.html (单 HTML 入口, ~260行)
       ├─ 11 个 JS 模块 (~2400行): chat / render / files / board / runtime / skills / council / store / bridge / i18n / app
       │
       ├─ HermesBridge (30+ @JavascriptInterface)
       │   ├─ IntentParser → CapabilityExecutor (34 个设备 & 文件能力)
       │   ├─ AiClient (OpenAI 兼容: DeepSeek / Qwen / OpenAI / Ollama)
       │   ├─ ModelRegistry (多模型注册 & 加密存储)
       │   ├─ CouncilClient (多模型并行讨论 → 汇总 → 结构化输出)
       │   ├─ StorageManager (五种存储: 产出/资料/归档/模板/个人)
       │   ├─ CronManager (WorkManager 定时调度)
       │   ├─ SkillStore (技能 CRUD)
       │   └─ StatsCollector (匿名使用统计)
       │
       └─ B (JS 桥封装, 50+ 方法)
```

---

## 技术栈

- Java 11 · Android API 26+ · targetSdk 36
- WebView 壳 + 纯 HTML/CSS/JS（无前端框架）
- Gradle 8.13 · appcompat 1.6.1
- JUnit 4 · 87 测试用例

---

## 文档

| 文档 | 内容 |
|------|------|
| [MOV_MASTER.md](docs/MOV_MASTER.md) | 项目全貌 |
| [DESIGN_MULTI_MODEL.md](docs/DESIGN_MULTI_MODEL.md) | 多模型协作引擎 |
| [DESIGN_STORAGE.md](docs/DESIGN_STORAGE.md) | 存储系统 |
| [DESIGN_INTERACTION.md](docs/DESIGN_INTERACTION.md) | 交互系统 |
| [DESIGN_BOARD_V2.md](docs/DESIGN_BOARD_V2.md) | 看板设计 |
| [DESIGN_RUNTIME.md](docs/DESIGN_RUNTIME.md) | 运行页设计 |
| [DESIGN_RUNTIME_LAYOUT.md](docs/DESIGN_RUNTIME_LAYOUT.md) | 运行页布局 |
| [DESIGN_NEW_ROOM.md](docs/DESIGN_NEW_ROOM.md) | 新建房间流程 |
| [DESIGN_SECURITY.md](docs/DESIGN_SECURITY.md) | 安全修复 |
| [DESIGN_REFACTOR.md](docs/DESIGN_REFACTOR.md) | 架构重构 |
| [DESIGN_GAP.md](docs/DESIGN_GAP.md) | 验收缺口 & 分工 |

---

## 许可

MIT
