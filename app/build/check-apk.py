#!/usr/bin/env python3
"""Check the packaged APK, not just the source manifest.

The source Manifest and `app/build.gradle` are checked by
`check-manifest.py`. This script closes the remaining gap: Gradle merges
library manifests, rewrites the target SDK, and adds AndroidX permissions and
components, so the artifact users install can differ from the source.

Usage:
    python app/build/check-apk.py [path-to.apk]

`aapt` is resolved from AAPT, ANDROID_HOME/ANDROID_SDK_ROOT, local.properties,
or the standard build-tools 36.0.0 location.
"""

from pathlib import Path
import os
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug-16k-signed.apk"
GRADLE = ROOT / "app" / "build.gradle"
LOCAL = ROOT / "local.properties"

checks = 0
errors = []


def check(name, condition, detail=""):
    global checks
    checks += 1
    if not condition:
        errors.append(name + (": " + detail if detail else ""))


def find_aapt():
    explicit = os.environ.get("AAPT")
    if explicit and Path(explicit).is_file():
        return explicit
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk and LOCAL.is_file():
        for line in LOCAL.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                sdk = line.split("=", 1)[1].strip().replace("\\\\", "\\")
                break
    if sdk:
        for version in ("36.0.0", "35.0.0", "34.0.0"):
            candidate = Path(sdk) / "build-tools" / version / "aapt.exe"
            if candidate.is_file():
                return str(candidate)
    found = shutil.which("aapt")
    return found


def run(aapt, *args):
    result = subprocess.run([aapt, *args], capture_output=True, text=True,
                            encoding="utf-8", errors="replace")
    if result.returncode != 0:
        raise RuntimeError("aapt failed: " + result.stderr.strip())
    return result.stdout


def gradle_value(name):
    text = GRADLE.read_text(encoding="utf-8")
    match = re.search(r"\b" + re.escape(name) + r"\s+['\"]?([^'\"\s]+)", text)
    return match.group(1) if match else None


def main():
    apk = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_APK
    if not apk.is_file():
        print("ApkPolicy: missing " + str(apk), file=sys.stderr)
        return 1
    aapt = find_aapt()
    if not aapt:
        print("ApkPolicy: aapt not found; set AAPT or ANDROID_HOME", file=sys.stderr)
        return 1

    badging = run(aapt, "dump", "badging", str(apk))
    xmltree = run(aapt, "dump", "xmltree", str(apk), "AndroidManifest.xml")

    package = re.search(r"package: name='([^']+)'", badging)
    check("package id", package is not None and package.group(1) == "app.spektralab",
          package.group(1) if package else "missing")
    version_code = re.search(r"versionCode='([^']+)'", badging)
    check("versionCode matches Gradle",
          version_code is not None and version_code.group(1) == gradle_value("versionCode"),
          version_code.group(1) if version_code else "missing")
    version_name = re.search(r"versionName='([^']+)'", badging)
    check("versionName matches Gradle",
          version_name is not None and version_name.group(1) == gradle_value("versionName"),
          version_name.group(1) if version_name else "missing")
    min_sdk = re.search(r"^sdkVersion:'(\d+)'", badging, re.M)
    target_sdk = re.search(r"^targetSdkVersion:'(\d+)'", badging, re.M)
    compile_sdk = re.search(r"compileSdkVersion='(\d+)'", badging)
    check("minSdk 24", min_sdk is not None and min_sdk.group(1) == "24",
          min_sdk.group(1) if min_sdk else "missing")
    check("targetSdk 36", target_sdk is not None and target_sdk.group(1) == "36",
          target_sdk.group(1) if target_sdk else "missing")
    check("compileSdk 36", compile_sdk is not None and compile_sdk.group(1) == "36",
          compile_sdk.group(1) if compile_sdk else "missing")

    perms = {}
    for match in re.finditer(r"uses-permission: name='([^']+)'(?: maxSdkVersion='(\d+)')?", badging):
        perms[match.group(1)] = match.group(2)
    check("READ_EXTERNAL_STORAGE capped at 32",
          perms.get("android.permission.READ_EXTERNAL_STORAGE") == "32",
          perms.get("android.permission.READ_EXTERNAL_STORAGE", "missing"))
    check("INTERNET present", "android.permission.INTERNET" in perms)
    forbidden = [p for p in perms if p in {
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
    }]
    check("no broad storage/media permission", not forbidden, str(sorted(forbidden)))

    check("allowBackup=false", "android:allowBackup" in xmltree and "0x0" in
          xmltree.split("android:allowBackup", 1)[1].splitlines()[0])
    check("usesCleartextTraffic=false", "android:usesCleartextTraffic" in xmltree and "0x0" in
          xmltree.split("android:usesCleartextTraffic", 1)[1].splitlines()[0])
    check("networkSecurityConfig referenced", "android:networkSecurityConfig" in xmltree)
    check("launcher action present", "android.intent.action.MAIN" in xmltree)
    check("launcher category present", "android.intent.category.LAUNCHER" in xmltree)
    check("no external VIEW action", "android.intent.action.VIEW" not in xmltree)

    if errors:
        for error in errors:
            print("ApkPolicy: " + error, file=sys.stderr)
        print("ApkPolicy: " + str(len(errors)) + " failure(s)", file=sys.stderr)
        return 1
    print("ApkPolicy: " + str(checks) + " checks passed (" + str(apk) + ")")
    return 0


if __name__ == "__main__":
    sys.exit(main())
