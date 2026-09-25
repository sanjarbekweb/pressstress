"""Check that the user-facing start button creates a real system overlay."""

from __future__ import annotations

import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


PACKAGE = "com.pressstress.app"
APK = "app/build/outputs/apk/debug/app-debug.apk"
PREFS = "shared_prefs/pressstress_preferences.xml"


def adb(*args: str, timeout: int = 45) -> str:
    result = subprocess.run(
        ["adb", *args], capture_output=True, text=True, timeout=timeout, check=True
    )
    return result.stdout


def find_start_button() -> tuple[int, int] | None:
    adb("shell", "uiautomator", "dump", "/sdcard/pressstress-ui.xml")
    screen = adb("exec-out", "cat", "/sdcard/pressstress-ui.xml")
    root = ET.fromstring(screen)
    for node in root.iter("node"):
        if "Enable floating reset" in node.attrib.get("text", ""):
            points = [int(value) for value in re.findall(r"\d+", node.attrib["bounds"])]
            return ((points[0] + points[2]) // 2, (points[1] + points[3]) // 2)
    return None


def main() -> None:
    adb("install", "-r", APK, timeout=120)
    adb("shell", "appops", "set", PACKAGE, "SYSTEM_ALERT_WINDOW", "allow")
    adb("shell", "pm", "grant", PACKAGE, "android.permission.POST_NOTIFICATIONS")
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(2)

    size = adb("shell", "wm", "size")
    match = re.search(r"(\d+)x(\d+)", size)
    if match is None:
        raise AssertionError(f"Could not read emulator screen size: {size}")
    width, height = map(int, match.groups())

    button = None
    for _ in range(6):
        button = find_start_button()
        if button:
            break
        adb(
            "shell", "input", "swipe",
            str(width // 2), str(height * 4 // 5),
            str(width // 2), str(height // 5), "350",
        )
        time.sleep(1)
    if button is None:
        print(adb("shell", "am", "get-current-user"))
        print(adb("shell", "dumpsys", "activity", "activities")[-6000:])
        print(adb("exec-out", "cat", "/sdcard/pressstress-ui.xml")[-6000:])
        save_screenshot()
        raise AssertionError("Enable floating reset button was not found on the main screen")

    adb("shell", "input", "tap", str(button[0]), str(button[1]))
    for _ in range(12):
        time.sleep(1)
        preferences = adb("shell", "run-as", PACKAGE, "cat", PREFS)
        if 'name="overlay_enabled" value="true"' in preferences:
            break
        if 'name="overlay_error"' in preferences:
            raise AssertionError(f"Overlay start error: {preferences}")
    else:
        raise AssertionError(f"Overlay did not start: {preferences}")

    adb(
        "shell", "am", "start",
        "-a", "android.intent.action.MAIN",
        "-c", "android.intent.category.HOME",
    )
    time.sleep(2)
    output = save_screenshot()
    windows = adb("shell", "dumpsys", "window", "windows", timeout=60)
    if PACKAGE not in windows:
        raise AssertionError("No PressStress overlay window remains over the home screen")
    adb("shell", "uiautomator", "dump", "/sdcard/pressstress-home.xml")
    home_ui = adb("exec-out", "cat", "/sdcard/pressstress-home.xml")
    if "A pause between" in home_ui:
        raise AssertionError("PressStress main screen is still visible after opening home")
    # UI Automator can omit application-overlay windows from the accessibility
    # hierarchy, so the window dump and saved screenshot verify its presence.

    print(f"Overlay started and remained on home screen: {output}")


def save_screenshot() -> Path:
    screenshot = subprocess.run(
        ["adb", "exec-out", "screencap", "-p"],
        capture_output=True,
        timeout=30,
        check=True,
    ).stdout
    output = Path("build/overlay-home.png")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(screenshot)
    return output


if __name__ == "__main__":
    main()
