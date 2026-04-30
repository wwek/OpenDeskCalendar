#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_PROPERTIES="$ROOT/local.properties"
REPO="${1:-}"

if [[ -z "$REPO" ]]; then
  if command -v git >/dev/null 2>&1; then
    origin="$(git -C "$ROOT" remote get-url origin 2>/dev/null || true)"
    if [[ "$origin" =~ github.com[:/]([^/]+/[^/.]+)(\.git)?$ ]]; then
      REPO="${BASH_REMATCH[1]}"
    fi
  fi
fi

if [[ -z "$REPO" ]]; then
  echo "Usage: $0 owner/repo" >&2
  exit 2
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI is required. Install gh and run: gh auth login" >&2
  exit 1
fi

if [[ ! -f "$LOCAL_PROPERTIES" ]]; then
  echo "Missing local.properties. Run ./scripts/create-release-keystore.sh first." >&2
  exit 1
fi

property() {
  awk -F= -v key="$1" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$LOCAL_PROPERTIES"
}

store_file="$(property opendeskcalendar.storeFile)"
store_password="$(property opendeskcalendar.storePassword)"
key_alias="$(property opendeskcalendar.keyAlias)"
key_password="$(property opendeskcalendar.keyPassword)"

if [[ -z "$store_file" || -z "$store_password" || -z "$key_alias" || -z "$key_password" ]]; then
  echo "local.properties does not contain complete release signing config." >&2
  echo "Run ./scripts/create-release-keystore.sh first." >&2
  exit 1
fi

absolute_store_file="$store_file"
if [[ "$absolute_store_file" != /* ]]; then
  absolute_store_file="$ROOT/$absolute_store_file"
fi

if [[ ! -f "$absolute_store_file" ]]; then
  echo "Keystore not found: $absolute_store_file" >&2
  exit 1
fi

base64_one_line() {
  if base64 --help 2>&1 | grep -q -- '-w'; then
    base64 -w 0 "$1"
  else
    base64 < "$1" | tr -d '\n'
  fi
}

echo "Writing GitHub Actions release signing secrets to $REPO"
base64_one_line "$absolute_store_file" | gh secret set OPEN_DESK_CALENDAR_RELEASE_KEYSTORE_BASE64 --repo "$REPO"
printf '%s' "$store_password" | gh secret set OPEN_DESK_CALENDAR_STORE_PASSWORD --repo "$REPO"
printf '%s' "$key_alias" | gh secret set OPEN_DESK_CALENDAR_KEY_ALIAS --repo "$REPO"
printf '%s' "$key_password" | gh secret set OPEN_DESK_CALENDAR_KEY_PASSWORD --repo "$REPO"
echo "GitHub release signing secrets are configured."
