#!/bin/sh
# SPDX-License-Identifier: GPL-3.0-or-later
set -eu
IME_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 21.0.7)
  export JAVA_HOME
fi
IME_JAVA_CMD=java
if [ -n "${JAVA_HOME:-}" ]; then IME_JAVA_CMD="$JAVA_HOME/bin/java"; fi
IME_JAVA_INFO=$("$IME_JAVA_CMD" -version 2>&1)
case "$IME_JAVA_INFO" in
  *Temurin-21.0.7+6*) ;;
  *) printf '%s\n' 'E_JDK_PIN: this build requires Temurin 21.0.7+6; configure only the local build environment.' >&2; exit 2 ;;
esac
export GRADLE_USER_HOME="$IME_ROOT/.cache/gradle"
export ANDROID_USER_HOME="$IME_ROOT/.cache/android-user"
export ANDROID_EMULATOR_HOME="$IME_ROOT/.cache/android-emulator"
export ANDROID_HOME="$IME_ROOT/.cache/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export BUILD_ABI=arm64-v8a
python3 "$IME_ROOT/scripts/check_toolchain.py"
exec "$IME_ROOT/android/trime/gradlew" -p "$IME_ROOT/android/trime" --no-daemon --max-workers=4 "$@"
