#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 25)}"
export E2E_BASE_URL="${E2E_BASE_URL:-http://localhost:3000}"
export E2E_ADMIN_URL="${E2E_ADMIN_URL:-http://localhost:5173}"

./gradlew :tests:e2e:test "$@"
