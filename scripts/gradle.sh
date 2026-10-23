#!/bin/sh
# SPDX-License-Identifier: GPL-3.0-or-later
set -eu
IME_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
export GRADLE_USER_HOME="$IME_ROOT/.cache/gradle"
export ANDROID_USER_HOME="$IME_ROOT/.cache/android-user"
export ANDROID_EMULATOR_HOME="$IME_ROOT/.cache/android-emulator"
export ANDROID_HOME="$IME_ROOT/.cache/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export BUILD_ABI=arm64-v8a
exec "$IME_ROOT/android/trime/gradlew" -p "$IME_ROOT/android/trime" --no-daemon --max-workers=4 "$@"
