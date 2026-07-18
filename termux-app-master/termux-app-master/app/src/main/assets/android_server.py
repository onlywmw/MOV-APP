#!/usr/bin/env python3
"""Hermes Android HTTP 适配器（完整版 agent 后端）

在 127.0.0.1:18080 提供与轻量 agent 完全兼容的接口：
  POST /chat   {"message": "..."} -> {"reply": "..."}
  GET  /health -> {"status": "ok", "model": "...", "agent": "full"}

与轻量 agent.py 的差异：后端是完整 hermes-agent（多轮会话 + SQLite 持久化 +
技能 + 记忆 + cron + 完整工具循环），UI 侧零改动。
"""
import json
import os
import sys
import threading
import time
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HERMES_HOME = os.path.expanduser("~/.hermes")
AGENT_SRC = os.path.join(HERMES_HOME, "hermes-agent")
ENV_PATH = os.path.join(HERMES_HOME, ".env")

# 必须在 import hermes 模块之前设置
os.environ.setdefault("HERMES_HOME", HERMES_HOME)
os.environ.setdefault("HERMES_YOLO_MODE", "1")      # 无人值守：自动批准工具
os.environ.setdefault("HERMES_ACCEPT_HOOKS", "1")
os.environ.setdefault("HERMES_GATEWAY_SESSION", "1")  # 网关语义：解锁 cronjob 等工具
os.environ.setdefault("HERMES_PLUGINS_DEBUG", "1")
os.environ.setdefault("TERM", "dumb")
if AGENT_SRC not in sys.path:
    sys.path.insert(0, AGENT_SRC)

HOST, PORT = "127.0.0.1", 18080
SESSION_ID = "android-main"

# 与轻量 agent 一致的厂商映射（base_url 直连，不依赖 hermes provider 注册表）
PROVIDERS = {
    "deepseek": {"base_url": "https://api.deepseek.com/v1",
                 "default_model": "deepseek-v4-flash", "hermes_provider": "deepseek"},
    "kimi": {"base_url": "https://api.moonshot.cn/v1",
             "default_model": "moonshot-v1-8k", "hermes_provider": None},
    "qwen": {"base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
             "default_model": "qwen-plus", "hermes_provider": None},
    "zhipu": {"base_url": "https://open.bigmodel.cn/api/paas/v4",
              "default_model": "glm-4-flash", "hermes_provider": None},
    "doubao": {"base_url": "https://ark.cn-beijing.volces.com/api/v3",
               "default_model": "doubao-pro-32k", "hermes_provider": None},
    "yi": {"base_url": "https://api.lingyiwanwu.com/v1",
           "default_model": "yi-lightning", "hermes_provider": None},
    "openrouter": {"base_url": "https://openrouter.ai/api/v1",
                   "default_model": "openai/gpt-4o-mini", "hermes_provider": "openrouter"},
}

# 启用的工具集：hermes-cli = 核心本地工具全集；android_control 由桥插件注册
ENABLED_TOOLSETS = ["hermes-cli", "skills", "memory", "cronjob",
                    "session_search", "android_control"]

ANDROID_PROMPT = (
    "你是运行在用户 Android 设备上的 Hermes 智能体。"
    "可以使用 android_* 系列工具直接操作手机/平板（剪贴板、通知、短信、通讯录、"
    "应用、音量、亮度、无障碍点击、位置、电量、系统闹钟等），也可以使用本地终端和文件工具。"
    "用户说“定闹钟/闹钟”时优先用 android_alarm_set（系统时钟闹钟，最可靠）；"
    "用户说“提醒我”时用 cronjob 创建定时任务（结果以系统通知送达）。"
    "所有回复请使用中文。敏感操作（root_shell、锁屏、发短信等）执行前简要说明。"
)


def log(msg):
    print(f"[HermesFullAgent] {msg}", flush=True)


def load_env(path):
    env = {}
    try:
        with open(path, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip()
    except FileNotFoundError:
        pass
    return env


def resolve_llm_config(env):
    """从 .env 解析 provider/api_key/base_url/model，兼容旧版 OPENROUTER_*。"""
    provider = env.get("PROVIDER", "").strip().lower()
    api_key = env.get("API_KEY", "").strip()
    model = env.get("MODEL", "").strip()

    if not provider:
        provider = "openrouter"
    if not api_key:
        api_key = env.get("OPENROUTER_API_KEY", "").strip()
    if not model:
        model = env.get("OPENROUTER_MODEL", "").strip()

    cfg = PROVIDERS.get(provider, {})
    base_url = env.get("BASE_URL", "").strip() or cfg.get("base_url", "")
    if not model:
        model = cfg.get("default_model", "openai/gpt-4o-mini")
    # 厂商模型名大小写敏感，统一转小写防止手滑
    model = model.strip().lower()

    if not api_key:
        raise RuntimeError("未配置 API Key，请在设置中填写")

    # 让辅助/后台 LLM 调用（上下文压缩、cron 任务执行、标题生成等）
    # 也走同一厂商，而不是 config.yaml 里残留的 openrouter 默认值。
    os.environ.setdefault("HERMES_INFERENCE_PROVIDER",
                          cfg.get("hermes_provider") or "custom")
    os.environ.setdefault("HERMES_INFERENCE_MODEL", model)
    provider_key_env = {
        "deepseek": "DEEPSEEK_API_KEY",
        "openrouter": "OPENROUTER_API_KEY",
        "kimi": "KIMI_API_KEY",
        "zhipu": "GLM_API_KEY",
        "qwen": "DASHSCOPE_API_KEY",
    }.get(provider)
    if provider_key_env:
        os.environ.setdefault(provider_key_env, api_key)

    return {
        "provider": provider,
        "hermes_provider": cfg.get("hermes_provider"),
        "api_key": api_key,
        "base_url": base_url,
        "model": model,
    }


class AgentRunner:
    """单例 AIAgent + 会话历史，线程安全。"""

    def __init__(self, llm_cfg):
        from run_agent import AIAgent  # noqa:  import 时触发插件发现
        from hermes_state import SessionDB

        # 显式注册 android_bridge 插件：用户目录插件默认 opt-in
        # （不在 plugins.enabled 里会被跳过），而我们的插件 register() 不依赖 ctx，
        # 直接调即可，免去改 config.yaml。
        try:
            plugins_dir = os.path.join(HERMES_HOME, "plugins")
            if plugins_dir not in sys.path:
                sys.path.insert(0, plugins_dir)
            import android_bridge
            android_bridge.register(None)
            log("android_bridge plugin registered")
        except Exception as e:
            log(f"android_bridge plugin load failed: {e}")

        kwargs = dict(
            api_key=llm_cfg["api_key"],
            model=llm_cfg["model"],
            enabled_toolsets=ENABLED_TOOLSETS,
            quiet_mode=True,
            platform="cli",
            session_id=SESSION_ID,
            session_db=SessionDB(),
            skip_context_files=True,
            ephemeral_system_prompt=ANDROID_PROMPT,
            max_iterations=30,
        )
        if llm_cfg["base_url"]:
            kwargs["base_url"] = llm_cfg["base_url"]
        if llm_cfg["hermes_provider"]:
            kwargs["provider"] = llm_cfg["hermes_provider"]

        self.agent = AIAgent(**kwargs)
        self.agent.suppress_status_output = True
        self.history = None
        self.lock = threading.Lock()
        self.model = llm_cfg["model"]

    def chat(self, message):
        with self.lock:
            result = self.agent.run_conversation(
                user_message=message,
                conversation_history=self.history,
                task_id=SESSION_ID,
            )
            self.history = result.get("messages", self.history)
            if result.get("failed"):
                raise RuntimeError(result.get("final_response") or "agent 执行失败")
            return result.get("final_response") or ""


RUNNER = None
RUNNER_ERROR = None


def get_runner():
    global RUNNER, RUNNER_ERROR
    if RUNNER is None and RUNNER_ERROR is None:
        try:
            env = load_env(ENV_PATH)
            llm_cfg = resolve_llm_config(env)
            log(f"init agent: provider={llm_cfg['provider']} model={llm_cfg['model']}")
            RUNNER = AgentRunner(llm_cfg)
            log("agent ready")
        except Exception as e:
            RUNNER_ERROR = f"{e}"
            log(f"agent init failed: {e}\n{traceback.format_exc()}")
    return RUNNER


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def _json(self, code, obj):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/health":
            env = load_env(ENV_PATH)
            model = env.get("MODEL", "unknown")
            self._json(200, {"status": "ok", "model": model, "agent": "full"})
        else:
            self._json(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/chat":
            self._json(404, {"error": "not found"})
            return
        try:
            length = int(self.headers.get("Content-Length", 0))
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            if isinstance(payload, str):  # 兼容双重编码
                payload = json.loads(payload)
            message = (payload.get("message") or "").strip()
        except Exception:
            self._json(400, {"error": "invalid json"})
            return

        if not message:
            self._json(200, {"reply": "请输入消息"})
            return

        runner = get_runner()
        if runner is None:
            self._json(200, {"reply": f"智能体初始化失败：{RUNNER_ERROR}"})
            return
        try:
            reply = runner.chat(message)
            self._json(200, {"reply": reply})
        except Exception as e:
            log(f"chat error: {e}\n{traceback.format_exc()}")
            self._json(200, {"reply": f"调用模型失败：{e}"})


def _notify_cron_output(path):
    """把 cron 任务的新输出以 Android 通知投递。"""
    try:
        plugins_dir = os.path.join(HERMES_HOME, "plugins")
        if plugins_dir not in sys.path:
            sys.path.insert(0, plugins_dir)
        from android_bridge.client import get_bridge_client
        with open(path, encoding="utf-8", errors="ignore") as f:
            content = f.read().strip()
        job_id = os.path.basename(os.path.dirname(path))
        body = content[:400] if content else "(任务已完成，无输出)"
        get_bridge_client().call("notification", {
            "title": f"⏰ 定时任务 {job_id}",
            "message": body,
        })
        log(f"cron output notified: {path}")
    except Exception as e:
        log(f"cron notify failed: {e}")


def _cron_loop():
    """每 60 秒驱动 cron 调度；deliver=local 的任务输出走 Android 通知。"""
    try:
        from cron.scheduler import tick
    except Exception as e:
        log(f"cron scheduler unavailable: {e}")
        return
    out_dir = os.path.join(HERMES_HOME, "cron", "output")
    # 启动前的历史输出不补发
    seen = set()
    if os.path.isdir(out_dir):
        for root, _, files in os.walk(out_dir):
            for f in files:
                seen.add(os.path.join(root, f))
    log("cron ticker started")
    while True:
        try:
            tick(verbose=False)
            if os.path.isdir(out_dir):
                for root, _, files in os.walk(out_dir):
                    for f in files:
                        p = os.path.join(root, f)
                        if p not in seen:
                            seen.add(p)
                            _notify_cron_output(p)
        except Exception as e:
            log(f"cron tick error: {e}")
        time.sleep(60)


def main():
    # 启动时预初始化，尽早暴露配置错误
    get_runner()
    threading.Thread(target=_cron_loop, daemon=True).start()
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    log(f"listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
