#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${1:-}"

if [[ -z "$VERSION" || "$VERSION" == "-h" || "$VERSION" == "--help" ]]; then
  cat >&2 <<'EOF'
Usage: ./scripts/release-tag.sh 0.1.2-alpha [--no-push]

Updates versionCode/versionName, commits the version bump, creates annotated tag
v<version>, builds the signed release APK locally, then pushes main and the tag.
EOF
  exit 2
fi

PUSH=true
if [[ "${2:-}" == "--no-push" ]]; then
  PUSH=false
fi

if [[ "$VERSION" == v* ]]; then
  VERSION="${VERSION#v}"
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$ ]]; then
  echo "Invalid version: $VERSION" >&2
  echo "Expected format like 0.1.2-alpha or 1.0.0." >&2
  exit 2
fi

TAG="v$VERSION"

cd "$ROOT"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is not clean. Commit or stash current changes before releasing." >&2
  git status --short
  exit 1
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Tag already exists: $TAG" >&2
  exit 1
fi

current_code="$(awk '/versionCode/ { print $2; exit }' app/build.gradle)"
if [[ -z "$current_code" || ! "$current_code" =~ ^[0-9]+$ ]]; then
  echo "Could not read versionCode from app/build.gradle" >&2
  exit 1
fi
next_code=$((current_code + 1))

badge_version="${VERSION//-/--}"

export ODC_RELEASE_VERSION="$VERSION"
export ODC_RELEASE_TAG="$TAG"
export ODC_RELEASE_CODE="$next_code"
export ODC_RELEASE_BADGE_VERSION="$badge_version"

perl -0pi -e 's/versionCode\s+\d+/"versionCode $ENV{ODC_RELEASE_CODE}"/e; s/versionName\s+"[^"]+"/"versionName \"$ENV{ODC_RELEASE_VERSION}\""/e' app/build.gradle
perl -0pi -e 's#version-[0-9]+\.[0-9]+\.[0-9]+(?:--[A-Za-z0-9.]+)?-0f766e#"version-$ENV{ODC_RELEASE_BADGE_VERSION}-0f766e"#ge; s#version [0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.]+)?#"version $ENV{ODC_RELEASE_VERSION}"#ge; s#`v[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.]+)?`#"`$ENV{ODC_RELEASE_TAG}`"#ge' README.md README.zh-Hant.md

git add app/build.gradle README.md README.zh-Hant.md
git commit -m "Release $TAG"

./gradlew --no-daemon :app:assembleDebug :app:lintDebug
./scripts/build-release.sh

git tag -a "$TAG" -m "Release $TAG"

if [[ "$PUSH" == true ]]; then
  branch="$(git branch --show-current)"
  git push origin "$branch"
  git push origin "$TAG"
  echo "Pushed $branch and $TAG. GitHub Actions will build and publish the signed release when secrets are configured."
else
  echo "Created local tag $TAG. Push with: git push origin $(git branch --show-current) && git push origin $TAG"
fi
