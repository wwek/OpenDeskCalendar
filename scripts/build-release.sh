#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_PROPERTIES="$ROOT/local.properties"
APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
APKSIGNER="${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools/30.0.3/apksigner"

has_local_signing=false
if [[ -f "$LOCAL_PROPERTIES" ]] \
  && grep -q '^opendeskcalendar.storeFile=' "$LOCAL_PROPERTIES" \
  && grep -q '^opendeskcalendar.storePassword=' "$LOCAL_PROPERTIES" \
  && grep -q '^opendeskcalendar.keyAlias=' "$LOCAL_PROPERTIES" \
  && grep -q '^opendeskcalendar.keyPassword=' "$LOCAL_PROPERTIES"; then
  has_local_signing=true
fi

has_env_signing=false
if [[ -n "${OPEN_DESK_CALENDAR_STORE_FILE:-}" \
  && -n "${OPEN_DESK_CALENDAR_STORE_PASSWORD:-}" \
  && -n "${OPEN_DESK_CALENDAR_KEY_ALIAS:-}" \
  && -n "${OPEN_DESK_CALENDAR_KEY_PASSWORD:-}" ]]; then
  has_env_signing=true
fi

if [[ "$has_local_signing" == false && "$has_env_signing" == false ]]; then
  echo "No release signing config found. Creating a local release keystore."
  "$ROOT/scripts/create-release-keystore.sh"
fi

cd "$ROOT"
./gradlew --no-daemon :app:assembleRelease

if [[ ! -f "$APK" ]]; then
  echo "Release APK was not produced: $APK" >&2
  exit 1
fi

if [[ -x "$APKSIGNER" ]]; then
  "$APKSIGNER" verify --print-certs "$APK"
else
  echo "apksigner not found at $APKSIGNER; skipped signature verification." >&2
fi

echo "Signed release APK: $APK"
