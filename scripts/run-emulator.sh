#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_NAME="org.opendeskcalendar.app"
MAIN_ACTIVITY=".MainActivity"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
EMULATOR="${ANDROID_HOME:-$HOME/Library/Android/sdk}/emulator/emulator"

if [[ ! -x "$ADB" ]]; then
  ADB="$(command -v adb)"
fi

first_running_emulator() {
  "$ADB" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1; exit }'
}

wait_for_boot() {
  local serial="$1"
  "$ADB" -s "$serial" wait-for-device
  until [[ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    sleep 2
  done
}

SERIAL="${1:-}"
if [[ -z "$SERIAL" ]]; then
  SERIAL="$(first_running_emulator)"
fi

if [[ -z "$SERIAL" ]]; then
  if [[ ! -x "$EMULATOR" ]]; then
    echo "No running emulator found, and emulator binary is unavailable." >&2
    exit 1
  fi
  AVD="$("$EMULATOR" -list-avds | head -n 1)"
  if [[ -z "$AVD" ]]; then
    echo "No Android Virtual Device found. Create one in Android Studio first." >&2
    exit 1
  fi
  echo "Starting emulator: $AVD"
  nohup "$EMULATOR" -avd "$AVD" -netdelay none -netspeed full >/tmp/opendeskcalendar-emulator.log 2>&1 &
  sleep 5
  SERIAL="$(first_running_emulator)"
fi

if [[ -z "$SERIAL" ]]; then
  "$ADB" wait-for-device
  SERIAL="$(first_running_emulator)"
fi

if [[ -z "$SERIAL" ]]; then
  echo "Unable to find a running emulator." >&2
  exit 1
fi

echo "Using emulator: $SERIAL"
wait_for_boot "$SERIAL"

cd "$ROOT"
./gradlew --no-daemon :app:assembleDebug
"$ADB" -s "$SERIAL" install -r "$APK"
"$ADB" -s "$SERIAL" shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"
