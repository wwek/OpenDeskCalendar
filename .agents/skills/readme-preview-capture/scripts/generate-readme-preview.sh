#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PACKAGE="org.opendeskcalendar.app"
ACTIVITY="$PACKAGE/.MainActivity"
SCREENSHOT_DIR="$ROOT/docs/assets/screenshots"
SHOWCASE="$ROOT/docs/assets/readme-showcase.png"
MIN_READY_BYTES="${ODC_PREVIEW_MIN_READY_BYTES:-150000}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

device_count() {
  adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }'
}

write_theme_preferences() {
  local theme="$1"
  local tmp
  tmp="$(mktemp)"
  cat > "$tmp" <<EOF
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="theme">$theme</string>
</map>
EOF
  adb push "$tmp" /data/local/tmp/opendeskcalendar-preview.xml >/dev/null
  adb shell chmod 644 /data/local/tmp/opendeskcalendar-preview.xml >/dev/null
  adb shell run-as "$PACKAGE" mkdir -p shared_prefs >/dev/null
  adb shell run-as "$PACKAGE" cp /data/local/tmp/opendeskcalendar-preview.xml shared_prefs/open_desk_calendar.xml
  rm -f "$tmp"
}

capture_when_ready() {
  local target="$1"
  local tmp="$target.tmp"
  local attempt=1
  while [ "$attempt" -le 12 ]; do
    adb exec-out screencap -p > "$tmp"
    local size
    size="$(stat -f%z "$tmp" 2>/dev/null || stat -c%s "$tmp")"
    if [ "$size" -gt "$MIN_READY_BYTES" ]; then
      mv "$tmp" "$target"
      return 0
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
  mv "$tmp" "$target"
  echo "Captured $target but it did not pass ready threshold $MIN_READY_BYTES bytes." >&2
  return 1
}

capture_theme_orientation() {
  local theme="$1"
  local orientation="$2"
  local rotation=0
  if [ "$orientation" = "landscape" ]; then
    rotation=1
  fi

  adb shell am force-stop "$PACKAGE" >/dev/null || true
  write_theme_preferences "$theme"
  adb shell cmd window fixed-to-user-rotation enabled >/dev/null || true
  adb shell cmd window user-rotation lock "$rotation" >/dev/null
  adb shell am start -n "$ACTIVITY" >/dev/null
  sleep 4
  capture_when_ready "$SCREENSHOT_DIR/$theme-$orientation.png"
}

make_label() {
  local theme="$1"
  case "$theme" in
    light) printf 'Light' ;;
    dark) printf 'Dark' ;;
    mono) printf 'Mono' ;;
    eink) printf 'E-ink' ;;
  esac
}

compose_showcase() {
  local font_args=()
  if [ -f /System/Library/Fonts/Helvetica.ttc ]; then
    font_args=(-font /System/Library/Fonts/Helvetica.ttc)
  fi

  find /tmp -maxdepth 1 \( \
    -name 'opendeskcalendar-row-*.png' \
    -o -name 'opendeskcalendar-label-*.png' \
    -o -name 'opendeskcalendar-*-portrait.png' \
    -o -name 'opendeskcalendar-*-landscape.png' \
  \) -delete

  for theme in light dark mono eink; do
    local label
    label="$(make_label "$theme")"
    magick -background '#f8fafc' -fill '#111827' "${font_args[@]}" \
      -gravity center -size 160x522 -pointsize 32 label:"$label" \
      "/tmp/opendeskcalendar-label-$theme.png"
    magick "$SCREENSHOT_DIR/$theme-portrait.png" -resize x520 \
      -bordercolor '#d1d5db' -border 1 "/tmp/opendeskcalendar-$theme-portrait.png"
    magick "$SCREENSHOT_DIR/$theme-landscape.png" -resize x520 \
      -bordercolor '#d1d5db' -border 1 "/tmp/opendeskcalendar-$theme-landscape.png"
    magick -background '#f8fafc' \
      "/tmp/opendeskcalendar-label-$theme.png" \
      "/tmp/opendeskcalendar-$theme-portrait.png" \
      "/tmp/opendeskcalendar-$theme-landscape.png" \
      +append "/tmp/opendeskcalendar-row-$theme.png"
  done

  magick -background '#f8fafc' -gravity center \
    /tmp/opendeskcalendar-row-light.png \
    /tmp/opendeskcalendar-row-dark.png \
    /tmp/opendeskcalendar-row-mono.png \
    /tmp/opendeskcalendar-row-eink.png \
    -append -bordercolor '#f8fafc' -border 32 "$SHOWCASE"
  magick "$SHOWCASE" -strip -depth 8 "$SHOWCASE"
}

restore_default_preview_state() {
  adb shell am force-stop "$PACKAGE" >/dev/null || true
  write_theme_preferences dark
  adb shell cmd window user-rotation lock 1 >/dev/null || true
  adb shell am start -n "$ACTIVITY" >/dev/null || true
}

main() {
  require_command adb
  require_command magick

  if [ "$(device_count)" -ne 1 ]; then
    echo "Expected exactly one connected adb device." >&2
    adb devices >&2
    exit 1
  fi

  if ! adb shell run-as "$PACKAGE" true >/dev/null 2>&1; then
    echo "Package $PACKAGE is not debuggable or not installed. Run ./gradlew installDebug first." >&2
    exit 1
  fi

  mkdir -p "$SCREENSHOT_DIR"
  for theme in light dark mono eink; do
    capture_theme_orientation "$theme" portrait
    capture_theme_orientation "$theme" landscape
  done
  compose_showcase
  restore_default_preview_state

  magick identify "$SHOWCASE" "$SCREENSHOT_DIR"/*.png
}

main "$@"
