#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
    echo "No .env found; copying .env.example to .env" >&2
    cp .env.example .env
fi

docker compose up -d --wait
docker compose ps
