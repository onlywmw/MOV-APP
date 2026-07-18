"""Hermes plugin that exposes Android capabilities via the Android bridge socket."""

import json
import logging
from typing import Any, Dict

from hermes_cli.plugins import PluginContext
from tools.registry import registry
from toolsets import TOOLSETS

from .client import get_bridge_client

logger = logging.getLogger(__name__)

TOOLSET_NAME = "android_control"


def _register_toolset() -> None:
    """Add the android_control toolset to Hermes if not already present."""
    if TOOLSET_NAME not in TOOLSETS:
        TOOLSETS[TOOLSET_NAME] = {
            "description": "Android system control tools: clipboard, notifications, sensors, calls, root, etc.",
            "tools": [],
            "includes": [],
        }


def _check_bridge_available() -> bool:
    try:
        client = get_bridge_client()
        client.call("ping", timeout=2.0)
        return True
    except Exception as e:
        logger.debug("Android bridge not available: %s", e)
        return False


def _bridge_call(method: str, params: Dict[str, Any] = None) -> str:
    """调用桥并把结果序列化为 JSON 字符串（hermes 工具契约要求 str 返回）。"""
    result = get_bridge_client().call(method, params)
    return json.dumps(result, ensure_ascii=False)


def _clipboard_read(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("clipboard_read")


def _clipboard_write(text: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("clipboard_write", {"text": text})


def _notification_show(title: str, message: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("notification", {"title": title, "message": message})


def _device_info(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("device_info")


def _vibrate(duration: int = 200, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("vibrate", {"duration": duration})


def _torch(on: bool = True, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("torch", {"on": on})


def _battery_status(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("battery")


def _shell(command: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("shell", {"command": command})


def _root_shell(command: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("root_shell", {"command": command})


def _accessibility_dump(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("accessibility_dump")


def _accessibility_click(text: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("accessibility_click", {"text": text})


def _accessibility_input(text: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("accessibility_input", {"text": text})


def _device_admin_lock(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("device_admin_lock")


def _location_get(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("location_get")


def _sms_list(limit: int = 20, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("sms_list", {"limit": limit})


def _sms_send(to: str, body: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("sms_send", {"to": to, "body": body})


def _contacts_list(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("contacts_list")


def _app_list(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("app_list")


def _app_open(package: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("app_open", {"package": package})


def _volume_get(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("volume_get")


def _volume_set(volume: int, stream: int = 3, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("volume_set", {"stream": stream, "volume": volume})


def _brightness_get(**_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("brightness_get")


def _brightness_set(brightness: int, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("brightness_set", {"brightness": brightness})


def _open_url(url: str, **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("open_url", {"url": url})


def _alarm_set(hour: int, minutes: int = 0, message: str = "Hermes 提醒", **_kwargs: Any) -> Dict[str, Any]:
    return _bridge_call("alarm_set", {"hour": hour, "minutes": minutes, "message": message})


def register(ctx: PluginContext) -> None:
    """Plugin entry point called by Hermes during plugin discovery."""
    _register_toolset()

    registry.register(
        name="android_clipboard_read",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _clipboard_read(**args),
        check_fn=_check_bridge_available,
        description="Read the current text from the Android clipboard.",
        emoji="📋",
    )

    registry.register(
        name="android_clipboard_write",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "text": {"type": "string", "description": "Text to write to the Android clipboard."},
            },
            "required": ["text"],
        },
        handler=lambda args, **_kw: _clipboard_write(**args),
        check_fn=_check_bridge_available,
        description="Write text to the Android clipboard.",
        emoji="📋",
    )

    registry.register(
        name="android_notification_show",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "title": {"type": "string", "description": "Notification title."},
                "message": {"type": "string", "description": "Notification body."},
            },
            "required": ["title", "message"],
        },
        handler=lambda args, **_kw: _notification_show(**args),
        check_fn=_check_bridge_available,
        description="Show an Android notification.",
        emoji="🔔",
    )

    registry.register(
        name="android_device_info",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _device_info(**args),
        check_fn=_check_bridge_available,
        description="Get basic Android device info from the bridge.",
        emoji="📱",
    )

    registry.register(
        name="android_vibrate",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "duration": {"type": "integer", "description": "Vibration duration in milliseconds.", "default": 200},
            },
        },
        handler=lambda args, **_kw: _vibrate(**args),
        check_fn=_check_bridge_available,
        description="Vibrate the Android device.",
        emoji="📳",
    )

    registry.register(
        name="android_torch",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "on": {"type": "boolean", "description": "Turn torch on or off.", "default": True},
            },
        },
        handler=lambda args, **_kw: _torch(**args),
        check_fn=_check_bridge_available,
        description="Toggle the Android camera flashlight.",
        emoji="🔦",
    )

    registry.register(
        name="android_battery_status",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _battery_status(**args),
        check_fn=_check_bridge_available,
        description="Get Android battery level and charging status.",
        emoji="🔋",
    )

    registry.register(
        name="android_shell",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "command": {"type": "string", "description": "Shell command to execute in the app context."},
            },
            "required": ["command"],
        },
        handler=lambda args, **_kw: _shell(**args),
        check_fn=_check_bridge_available,
        description="Run a shell command in the Android app context (not Termux).",
        emoji="🐚",
    )

    registry.register(
        name="android_root_shell",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "command": {"type": "string", "description": "Shell command to execute as root via su."},
            },
            "required": ["command"],
        },
        handler=lambda args, **_kw: _root_shell(**args),
        check_fn=_check_bridge_available,
        description="Run a shell command as root via su (requires rooted device).",
        emoji="🔓",
    )

    registry.register(
        name="android_accessibility_dump",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _accessibility_dump(**args),
        check_fn=_check_bridge_available,
        description="Dump the current Android window UI hierarchy (requires accessibility service enabled).",
        emoji="🖱️",
    )

    registry.register(
        name="android_accessibility_click",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "text": {"type": "string", "description": "Text/content-desc of the element to click."},
            },
            "required": ["text"],
        },
        handler=lambda args, **_kw: _accessibility_click(**args),
        check_fn=_check_bridge_available,
        description="Click a UI element by its visible text (requires accessibility service enabled).",
        emoji="👆",
    )

    registry.register(
        name="android_accessibility_input",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "text": {"type": "string", "description": "Text to input into the focused editable field."},
            },
            "required": ["text"],
        },
        handler=lambda args, **_kw: _accessibility_input(**args),
        check_fn=_check_bridge_available,
        description="Type text into the focused input field (requires accessibility service enabled).",
        emoji="⌨️",
    )

    registry.register(
        name="android_device_admin_lock",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _device_admin_lock(**args),
        check_fn=_check_bridge_available,
        description="Lock the device screen (requires device admin enabled).",
        emoji="🔒",
    )

    registry.register(
        name="android_location_get",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _location_get(**args),
        check_fn=_check_bridge_available,
        description="Get last known device location (requires location permission).",
        emoji="📍",
    )

    registry.register(
        name="android_sms_list",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "limit": {"type": "integer", "description": "Max number of recent SMS messages to return.", "default": 20},
            },
        },
        handler=lambda args, **_kw: _sms_list(**args),
        check_fn=_check_bridge_available,
        description="List recent SMS messages (requires SMS permission).",
        emoji="✉️",
    )

    registry.register(
        name="android_sms_send",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "to": {"type": "string", "description": "Recipient phone number."},
                "body": {"type": "string", "description": "SMS text body."},
            },
            "required": ["to", "body"],
        },
        handler=lambda args, **_kw: _sms_send(**args),
        check_fn=_check_bridge_available,
        description="Send an SMS message (requires SMS permission). Confirm with the user before sending.",
        emoji="📤",
    )

    registry.register(
        name="android_contacts_list",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _contacts_list(**args),
        check_fn=_check_bridge_available,
        description="List device contacts (requires contacts permission).",
        emoji="👥",
    )

    registry.register(
        name="android_app_list",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _app_list(**args),
        check_fn=_check_bridge_available,
        description="List installed Android apps with package names.",
        emoji="📦",
    )

    registry.register(
        name="android_app_open",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "package": {"type": "string", "description": "Package name of the app to open, e.g. com.android.settings."},
            },
            "required": ["package"],
        },
        handler=lambda args, **_kw: _app_open(**args),
        check_fn=_check_bridge_available,
        description="Open (launch) an Android app by package name.",
        emoji="🚀",
    )

    registry.register(
        name="android_volume_get",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _volume_get(**args),
        check_fn=_check_bridge_available,
        description="Get current Android volume levels.",
        emoji="🔊",
    )

    registry.register(
        name="android_volume_set",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "volume": {"type": "integer", "description": "Target volume index."},
                "stream": {"type": "integer", "description": "Audio stream: 3=music, 2=ring, 4=alarm, 1=system, 0=voice call.", "default": 3},
            },
            "required": ["volume"],
        },
        handler=lambda args, **_kw: _volume_set(**args),
        check_fn=_check_bridge_available,
        description="Set Android volume for an audio stream.",
        emoji="🔉",
    )

    registry.register(
        name="android_brightness_get",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {},
        },
        handler=lambda args, **_kw: _brightness_get(**args),
        check_fn=_check_bridge_available,
        description="Get current screen brightness (0-255) and mode.",
        emoji="☀️",
    )

    registry.register(
        name="android_brightness_set",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "brightness": {"type": "integer", "description": "Brightness value 0-255."},
            },
            "required": ["brightness"],
        },
        handler=lambda args, **_kw: _brightness_set(**args),
        check_fn=_check_bridge_available,
        description="Set screen brightness (0-255, requires write-settings permission).",
        emoji="🔆",
    )

    registry.register(
        name="android_open_url",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "url": {"type": "string", "description": "URL to open in the browser."},
            },
            "required": ["url"],
        },
        handler=lambda args, **_kw: _open_url(**args),
        check_fn=_check_bridge_available,
        description="Open a URL in the device browser.",
        emoji="🌐",
    )

    registry.register(
        name="android_alarm_set",
        toolset=TOOLSET_NAME,
        schema={
            "type": "object",
            "properties": {
                "hour": {"type": "integer", "description": "Hour of day, 0-23."},
                "minutes": {"type": "integer", "description": "Minutes, 0-59.", "default": 0},
                "message": {"type": "string", "description": "Alarm label text."},
            },
            "required": ["hour"],
        },
        handler=lambda args, **_kw: _alarm_set(**args),
        check_fn=_check_bridge_available,
        description="Set an alarm in the system Clock app (opens prefilled editor for one-tap user confirm). Prefer this over cronjob when the user says 定闹钟/闹钟.",
        emoji="⏰",
    )

    logger.info("Android bridge plugin registered")
