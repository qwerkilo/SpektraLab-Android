#!/usr/bin/env python3
"""Static permission and export policy for the shipped Android manifest.

The app uses the system picker, so it has no runtime permission request to
exercise. This check replaces that missing layer: it pins the permissions,
the single exported entry point, cleartext policy, and backup policy. Run it
before packaging; a policy regression fails the build, not the device.
"""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
NETWORK = ROOT / "app" / "src" / "main" / "res" / "xml" / "network_security_config.xml"
ANDROID = "{http://schemas.android.com/apk/res/android}"

checks = 0
errors = []


def check(name, condition, detail=""):
    global checks
    checks += 1
    if not condition:
        errors.append(name + (": " + detail if detail else ""))


def attr(node, name, default=None):
    return node.get(ANDROID + name, default)


def main():
    if not MANIFEST.is_file():
        print("ManifestPolicy: missing " + str(MANIFEST), file=sys.stderr)
        return 1
    if not NETWORK.is_file():
        print("ManifestPolicy: missing " + str(NETWORK), file=sys.stderr)
        return 1

    root = ET.parse(MANIFEST).getroot()
    check("package", root.get("package") == "app.spektralab", root.get("package", "?"))

    sdk = root.find("uses-sdk")
    check("uses-sdk present", sdk is not None)
    if sdk is not None:
        check("minSdk 24", attr(sdk, "minSdkVersion") == "24", attr(sdk, "minSdkVersion", "?"))
        check("targetSdk 36", attr(sdk, "targetSdkVersion") == "36", attr(sdk, "targetSdkVersion", "?"))

    permissions = {}
    for node in root.findall("uses-permission"):
        permissions[node.get(ANDROID + "name")] = node
    check("exactly two permissions", len(permissions) == 2, str(sorted(permissions)))
    read = permissions.get("android.permission.READ_EXTERNAL_STORAGE")
    check("READ_EXTERNAL_STORAGE capped at API 32", read is not None
          and attr(read, "maxSdkVersion") == "32",
          attr(read, "maxSdkVersion", "missing") if read is not None else "missing")
    check("INTERNET present", "android.permission.INTERNET" in permissions)
    check("no write/storage-wide permission",
          not any("WRITE_EXTERNAL_STORAGE" in p or "MANAGE_EXTERNAL_STORAGE" in p
                  for p in permissions))

    application = root.find("application")
    check("application present", application is not None)
    if application is not None:
        check("allowBackup=false", attr(application, "allowBackup") == "false",
              str(attr(application, "allowBackup")))
        check("usesCleartextTraffic=false",
              attr(application, "usesCleartextTraffic") == "false",
              str(attr(application, "usesCleartextTraffic")))
        check("networkSecurityConfig referenced",
              attr(application, "networkSecurityConfig") == "@xml/network_security_config",
              str(attr(application, "networkSecurityConfig")))
        check("not debuggable", attr(application, "debuggable") in (None, "false"),
              str(attr(application, "debuggable")))

    exported = []
    for tag in ("activity", "activity-alias", "service", "receiver", "provider"):
        for node in application.findall(tag) if application is not None else []:
            if attr(node, "exported") == "true":
                exported.append((tag, node.get(ANDROID + "name")))
    check("only the launcher activity is exported",
          exported == [("activity", ".MainActivity")], str(exported))

    activities = application.findall("activity") if application is not None else []
    check("one activity", len(activities) == 1, str(len(activities)))
    if activities:
        activity = activities[0]
        check("launcher activity exported", attr(activity, "exported") == "true")
        filters = activity.findall("intent-filter")
        check("one intent filter", len(filters) == 1, str(len(filters)))
        actions = [a.get(ANDROID + "name") for f in filters for a in f.findall("action")]
        categories = [c.get(ANDROID + "name") for f in filters for c in f.findall("category")]
        check("launcher action only", actions == ["android.intent.action.MAIN"], str(actions))
        check("launcher category only",
              categories == ["android.intent.category.LAUNCHER"], str(categories))
        check("no VIEW filter (dead external entry)",
              "android.intent.action.VIEW" not in actions)

    if application is not None:
        providers = application.findall("provider")
        check("no content provider", not providers, str(len(providers)))
        services = application.findall("service")
        check("no service", not services, str(len(services)))
        receivers = application.findall("receiver")
        check("no receiver", not receivers, str(len(receivers)))

    network = ET.parse(NETWORK).getroot()
    base = network.find("base-config")
    check("no global cleartext base-config",
          base is None or base.get("cleartextTrafficPermitted") != "true")
    domain_configs = network.findall("domain-config")
    cleartext = [d for d in domain_configs if d.get("cleartextTrafficPermitted") == "true"]
    check("one cleartext domain-config", len(cleartext) == 1, str(len(cleartext)))
    if cleartext:
        domains = sorted(d.text for d in cleartext[0].findall("domain") if d.text)
        check("cleartext is loopback only",
              domains == ["127.0.0.1", "localhost"], str(domains))
    else:
        check("cleartext is loopback only", False, "no cleartext domain")

    if errors:
        for error in errors:
            print("ManifestPolicy: " + error, file=sys.stderr)
        print("ManifestPolicy: " + str(len(errors)) + " failure(s)", file=sys.stderr)
        return 1
    print("ManifestPolicy: " + str(checks) + " checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
