#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# A15 smoke: p95 latency for the public read paths through api-gateway.
# Usage: BASE_URL=http://localhost:8080 REQUESTS=15 ./scripts/perf-smoke.sh
BASE_URL="${BASE_URL:-http://localhost:8080}"
REQUESTS="${REQUESTS:-15}"
THRESHOLD_MS="${THRESHOLD_MS:-500}"
PATHS=(
    "/api/v1/products?size=20"
    "/api/v1/individual-plants?size=20"
    "/api/v1/shipping/zones"
)

failures=0
for path in "${PATHS[@]}"; do
    if ! for _ in $(seq 1 "$REQUESTS"); do
        curl -sS -o /dev/null -w '%{time_total}\n' "$BASE_URL$path"
    done | sort -n | awk -v path="$path" -v limit="$THRESHOLD_MS" '
        { samples[NR] = $1 }
        END {
            p50 = samples[int(NR * 0.5) + 1]
            p95 = samples[int(NR * 0.95) + 1]
            printf "%-42s p50=%4.0fms p95=%4.0fms (budget %dms)\n", path, p50 * 1000, p95 * 1000, limit
            exit (p95 * 1000 > limit)
        }'; then
        failures=1
    fi
done

if (( failures )); then
    echo "p95 budget exceeded" >&2
    exit 1
fi
echo "p95 within budget for ${#PATHS[@]} paths"
