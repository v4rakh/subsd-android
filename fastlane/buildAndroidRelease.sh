#!/usr/bin/env sh
# Build a signed release AAB suitable for upload to the Google Play Store.
# Signing env vars are loaded from fastlane/secrets/signing.env if it exists.
# Required env vars:
#   SUBSD_KEYSTORE_FILE      - path to the .jks keystore (absolute or relative to android/)
#   SUBSD_KEYSTORE_PASSWORD  - keystore password
#   SUBSD_KEY_ALIAS          - key alias inside the keystore
#   SUBSD_KEY_PASSWORD       - key password
set -e

cd "$(dirname "$0")/.."

SIGNING_ENV="fastlane/secrets/signing.env"
if [ -f "$SIGNING_ENV" ]; then
    # shellcheck disable=SC1090
    . "$SIGNING_ENV"
fi

./gradlew clean bundleRelease $AAPT2_OVERRIDE
