#!/usr/bin/env bash
# Publish a release: announcement + APK, in one command.
#
#   CHANNEL=preview|stable CF_TOKEN=xxxx ./scripts/announce.sh <version_code> <version_name> <apk_path|-> "note" ...
#
# The channel is derived from the version name when CHANNEL is unset: a semver
# pre-release (1.4.0-m3) is `preview`, everything else `stable`.
#
# What it does, in order:
#   1. refuses a `stable` announcement while docs/known-issues.md has an open
#      release gate, then writes preview.json (or update.json) -- the file every
#      client can read through jsDelivr / raw
#   2. commits and pushes it, and purges the CDN
#   3. uploads the APK to the worker's KV, so https://spektralab.workshop.skin/download
#      serves it from Cloudflare instead of GitHub (which is slow or blocked in CN)
#   4. writes a *stable* announcement to the worker's KV. A preview announcement
#      is never written there: `/admin/latest` is what 1.3.4 clients read, and
#      they do not know about channels.
#   5. publishes a GitHub Release with the APK attached -- the Releases page is
#      where most people land, and it drifted twice (1.2.5 and 1.2.6 both went
#      out through the channel with no release behind them)
#
# The token is never stored here: this repository is public.
set -euo pipefail

if [ $# -lt 3 ]; then
  echo "usage: [CHANNEL=preview|stable] [CF_TOKEN=...] $0 <version_code> <version_name> <apk_path|-> [note...]" >&2
  exit 2
fi

CODE=$1
NAME=$2
APK=$3
shift 3

SITE="https://spektralab.workshop.skin"
UPD="https://update.workshop.skin"
REPO="SAINTTaiYi/SpektraLab-Android"
GATE="docs/known-issues.md"

# The gate is read, not trusted to memory: a build with KI-1 open may only be
# announced as `preview`, and the stable clients' file must not carry it.
CHANNEL=${CHANNEL:-}
if [ -z "$CHANNEL" ]; then
  case "$NAME" in
    *-*) CHANNEL=preview ;;
    *)   CHANNEL=stable ;;
  esac
fi
case "$CHANNEL" in
  stable|preview) ;;
  *) echo "invalid CHANNEL: $CHANNEL (use stable or preview)" >&2; exit 2 ;;
esac
if [ "$CHANNEL" = "stable" ] && ! grep -q '\*\*Release gate: accepted' "$GATE"; then
  echo "refusing to write/announce stable: $GATE has no 'Release gate: accepted' line." >&2
  echo "Announce this build as CHANNEL=preview, or accept/fix KI-1 first." >&2
  exit 1
fi

ANNOUNCE=update.json
if [ "$CHANNEL" = "preview" ]; then ANNOUNCE=preview.json; fi

NOTES=$(printf '%s\n' "$@" | python3 -c 'import json,sys
print(json.dumps([l.rstrip("\n") for l in sys.stdin if l.strip()], ensure_ascii=False))')
TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)

cat > "$ANNOUNCE" <<JSON
{
  "latest_version_code": $CODE,
  "latest_version_name": "$NAME",
  "url": "$SITE/",
  "notes": $NOTES,
  "updated_at": "$TS",
  "channel": "$CHANNEL"
}
JSON
echo "--- $ANNOUNCE"; cat "$ANNOUNCE"

git add "$ANNOUNCE"
git commit -m "chore(update): announce $NAME ($CODE) on $CHANNEL"
git push origin HEAD

# jsDelivr caches a moving ref; purge so the new announcement is served now.
for repo in qwerkilo SAINTTaiYi; do
  curl -s "https://purge.jsdelivr.net/gh/$repo/SpektraLab-Android@master/$ANNOUNCE" >/dev/null \
    && echo "--- jsDelivr purged ($repo)" || echo "--- jsDelivr purge failed for $repo (harmless)"
done

if [ -z "${CF_TOKEN:-}" ]; then
  echo "--- CF_TOKEN unset: KV not updated (clients fall back to the CDN copy)."
  exit 0
fi

if [ "$CHANNEL" = "stable" ] && [ "$APK" != "-" ]; then
  [ -f "$APK" ] || { echo "APK not found: $APK" >&2; exit 1; }
  SIZE=$(stat -c %s "$APK")
  if [ "$SIZE" -gt $((24 * 1024 * 1024)) ]; then
    echo "APK is $SIZE bytes; the KV value limit is 24 MiB. Use R2 or the GitHub asset." >&2
  else
    SHA=$(sha256sum "$APK" | cut -d' ' -f1)
    FILE="SpektraLab-$NAME-arm64-v8a.apk"
    echo "--- uploading $FILE ($SIZE bytes)"
    curl -s -X PUT "$UPD/admin/apk?name=$FILE&sha256=$SHA" \
      -H "authorization: Bearer $CF_TOKEN" \
      -H "content-type: application/vnd.android.package-archive" \
      --data-binary "@$APK"
    echo
  fi
elif [ "$APK" != "-" ]; then
  # The KV download URL is not channel-aware; a preview APK goes out through
  # the GitHub Release asset instead.
  echo "--- preview channel: APK not uploaded to the stable download URL"
fi

if [ "$CHANNEL" = "preview" ]; then
  # `/admin/latest` is read by every 1.3.4 client, which has no channel logic.
  # Leaving it on the stable announcement is the release gate made real.
  echo "--- preview channel: /admin/latest left at the stable announcement"
else
  echo "--- announcement"
  curl -s -X PUT "$UPD/admin/latest" \
    -H "authorization: Bearer $CF_TOKEN" \
    -H "content-type: application/json" \
    --data-binary @update.json
  echo
fi
# --- GitHub Release -------------------------------------------------------
# A failure here is a warning, not an abort: the channel is already updated.
if command -v gh >/dev/null 2>&1; then
  echo "--- github release v$NAME"
  GH_NOTES=$(printf -- '- %s\n' "$@")
  ASSET="$APK"
  if [ "$APK" != "-" ]; then
    ASSET="$HOME/spk-debug/SpektraLab-$NAME-arm64-v8a.apk"
    cp "$APK" "$ASSET"
  fi
  gh release create "v$NAME" -R "$REPO" \
    --title "SpektraLab Android $NAME" --notes "$GH_NOTES" "$ASSET" || \
    echo "(release creation failed; the update channel is already live)" >&2
fi

echo "--- live: $SITE/ ($CHANNEL)"
