#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."

./gradlew clean assembleDebug $AAPT2_OVERRIDE
