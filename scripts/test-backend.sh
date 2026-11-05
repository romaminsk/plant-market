#!/usr/bin/env bash
# Runs backend Gradle tasks with the Colima Docker socket wired for Testcontainers.
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 25)}"

if [[ -z "${DOCKER_HOST:-}" ]]; then
    DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"
    export DOCKER_HOST
fi
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-/var/run/docker.sock}"

exec ./gradlew "$@"
