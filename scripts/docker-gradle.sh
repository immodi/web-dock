#!/usr/bin/env bash
set -euo pipefail

# Runs Gradle inside the Android SDK Docker image used for local builds.
# Usage: scripts/docker-gradle.sh [gradle arguments]
# With no arguments it runs scripts/check.sh.

cd "$(dirname "$0")/.."

IMAGE="${WEBDOCK_ANDROID_IMAGE:-ghcr.io/cirruslabs/android-sdk:35}"
GRADLE_CACHE_VOLUME="${WEBDOCK_GRADLE_CACHE_VOLUME:-webdock-gradle}"

if [ "$#" -eq 0 ]; then
    set -- ./scripts/check.sh
fi

exec docker run --rm \
    -v "$PWD":/work \
    -v "$GRADLE_CACHE_VOLUME":/root/.gradle \
    -w /work \
    -e GRADLE_USER_HOME=/root/.gradle \
    "$IMAGE" "$@"
