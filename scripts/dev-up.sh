#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 25)}"

if [[ ! -f .env ]]; then
    echo "No .env found; copying .env.example to .env" >&2
    cp .env.example .env
fi

./gradlew \
    :services:auth-service:bootJar \
    :services:catalog-service:bootJar \
    :services:api-gateway:bootJar \
    --console=plain

docker compose up -d --build --wait
docker compose ps
