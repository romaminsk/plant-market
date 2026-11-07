#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 25)}"
./gradlew mutationTest "$@"
echo "PIT report: platform-common/build/reports/pitest/index.html"
