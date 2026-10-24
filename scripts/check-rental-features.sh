#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mode="${1:-rules}"
case "$mode" in
  rules) check_command='npm --prefix tools/firebase test' ;;
  android) check_command='bash gradlew -PuseFirebaseEmulators=true :app:connectedDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --console=plain' ;;
  all) check_command='npm --prefix tools/firebase test && bash gradlew -PuseFirebaseEmulators=true :app:connectedDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --console=plain' ;;
  *) echo 'Usage: bash scripts/check-rental-features.sh [rules|android|all]' >&2; exit 2 ;;
esac
firebase_cli=tools/firebase/node_modules/.bin/firebase
if [[ ! -x "$firebase_cli" ]]; then
  echo 'Run npm ci --prefix tools/firebase first (Node 22+, Java 21 required).' >&2
  exit 1
fi
export NO_PROXY="${NO_PROXY:+$NO_PROXY,}127.0.0.1,localhost,10.0.2.2"
export no_proxy="$NO_PROXY"
export XDG_CONFIG_HOME="${XDG_CONFIG_HOME:-${TMPDIR:-/tmp}/house-firebase-config}"
export FIREBASE_EMULATORS_PATH="${FIREBASE_EMULATORS_PATH:-$PWD/.firebase/emulators}"
# Explicit demo project and no inherited Google service account prevent production writes.
env -u GOOGLE_APPLICATION_CREDENTIALS CI=1 "$firebase_cli" emulators:exec \
  --project demo-house --only auth,firestore,storage "$check_command"
