#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_FILE="${OPEN_DESK_CALENDAR_STORE_FILE:-.local/signing/opendeskcalendar-release.jks}"
KEY_ALIAS="${OPEN_DESK_CALENDAR_KEY_ALIAS:-opendeskcalendar}"
STORE_PASSWORD="${OPEN_DESK_CALENDAR_STORE_PASSWORD:-}"
KEY_PASSWORD="${OPEN_DESK_CALENDAR_KEY_PASSWORD:-}"
FORCE="${1:-}"

absolute_keystore="$KEYSTORE_FILE"
if [[ "$absolute_keystore" != /* ]]; then
  absolute_keystore="$ROOT/$absolute_keystore"
fi

if [[ -e "$absolute_keystore" && "$FORCE" != "--force" ]]; then
  echo "Keystore already exists: $absolute_keystore"
  echo "Use --force to replace it."
else
  mkdir -p "$(dirname "$absolute_keystore")"
  if [[ -z "$STORE_PASSWORD" ]]; then
    if command -v openssl >/dev/null 2>&1; then
      STORE_PASSWORD="$(openssl rand -hex 24)"
    else
      STORE_PASSWORD="$(python3 - <<'PY'
import secrets
print(secrets.token_hex(24))
PY
)"
    fi
  fi
  if [[ -z "$KEY_PASSWORD" ]]; then
    KEY_PASSWORD="$STORE_PASSWORD"
  fi
  rm -f "$absolute_keystore"
  keytool -genkeypair \
    -v \
    -storetype JKS \
    -keystore "$absolute_keystore" \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=OpenDeskCalendar, OU=Open Source, O=OpenDeskCalendar, L=Unknown, ST=Unknown, C=CN"
fi

export ODC_KEYSTORE_FILE="$KEYSTORE_FILE"
export ODC_STORE_PASSWORD="$STORE_PASSWORD"
export ODC_KEY_ALIAS="$KEY_ALIAS"
export ODC_KEY_PASSWORD="$KEY_PASSWORD"
export ODC_ROOT="$ROOT"

python3 - <<'PY'
from pathlib import Path
import os

root = Path(os.environ["ODC_ROOT"])
local_properties = root / "local.properties"
values = {}
if local_properties.exists():
    for line in local_properties.read_text().splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()

values["opendeskcalendar.storeFile"] = os.environ["ODC_KEYSTORE_FILE"]
values["opendeskcalendar.storePassword"] = os.environ["ODC_STORE_PASSWORD"]
values["opendeskcalendar.keyAlias"] = os.environ["ODC_KEY_ALIAS"]
values["opendeskcalendar.keyPassword"] = os.environ["ODC_KEY_PASSWORD"]

ordered = []
if local_properties.exists():
    seen = set()
    for line in local_properties.read_text().splitlines():
        if "=" not in line or line.strip().startswith("#"):
            ordered.append(line)
            continue
        key = line.split("=", 1)[0].strip()
        if key in values:
            ordered.append(f"{key}={values[key]}")
            seen.add(key)
        else:
            ordered.append(line)
    for key in values:
        if key not in seen:
            ordered.append(f"{key}={values[key]}")
else:
    ordered = [f"{key}={value}" for key, value in values.items()]

local_properties.write_text("\n".join(ordered).rstrip() + "\n")
PY

echo "Release keystore configured in local.properties."
echo "Keystore: $absolute_keystore"
echo "Back up this keystore and local.properties secrets. Losing them means future APK updates cannot use the same signing identity."
