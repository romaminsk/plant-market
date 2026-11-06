#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

pkill -f "next start -p 3000" 2>/dev/null || true
pkill -f "vite preview --port 5173" 2>/dev/null || true

if [[ "${DEMO_DOWN_INFRA:-}" == "1" ]]; then
    ./scripts/dev-down.sh
else
    echo "Frontends stopped. Services are still running; use DEMO_DOWN_INFRA=1 to stop them."
fi
