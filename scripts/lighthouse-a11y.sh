#!/usr/bin/env bash
# Runs Lighthouse accessibility audit and fails below the Phase 1 gate (0.95).
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

URL="${1:-http://localhost:3000/catalog}"
OUT="${2:-build/lighthouse-a11y.json}"
mkdir -p "$(dirname "$OUT")"

pnpm dlx lighthouse@13.6.0 "$URL" \
    --only-categories=accessibility \
    --output=json \
    --output-path="$OUT" \
    --chrome-flags='--headless=new --no-sandbox' \
    --quiet

python3 - "$OUT" <<'PY'
import json
import sys

score = json.load(open(sys.argv[1], encoding="utf-8"))["categories"]["accessibility"]["score"]
print(f"accessibility score: {score}")
raise SystemExit(0 if score >= 0.95 else 1)
PY
