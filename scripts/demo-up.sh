#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 25)}"
export PNPM_HOME="${PNPM_HOME:-$HOME/Library/pnpm}"
export PATH="$PNPM_HOME:$PNPM_HOME/bin:$PATH"

if [[ ! -f .env ]]; then
    cp .env.example .env
fi

# Demo drives many catalog navigations; keep the public limit above the prod default.
export RATE_LIMIT_PUBLIC_RPM="${RATE_LIMIT_PUBLIC_RPM:-2000}"
export RATE_LIMIT_AUTH_RPM="${RATE_LIMIT_AUTH_RPM:-100}"

./scripts/dev-up.sh
if [[ "${DEMO_SKIP_SEED:-}" != "1" ]]; then
    ./scripts/seed.sh
fi

STORE_PORT="${DEMO_STOREFRONT_PORT:-3100}"

NEXT_PUBLIC_SITE_URL="http://localhost:$STORE_PORT" \
    AUTH_SERVICE_URL=http://localhost:8080 \
    pnpm --filter @plant-market/storefront build
pnpm --filter @plant-market/admin build

pkill -f "next start -p $STORE_PORT" 2>/dev/null || true
pkill -f "vite preview --port 5173" 2>/dev/null || true
sleep 1

API_URL=http://localhost:8080 AUTH_SERVICE_URL=http://localhost:8080 \
    nohup pnpm --filter @plant-market/storefront exec next start -p "$STORE_PORT" \
    > /tmp/plant-market-storefront.log 2>&1 &
nohup pnpm --filter @plant-market/admin exec vite preview --port 5173 --strictPort \
    > /tmp/plant-market-admin.log 2>&1 &

for _ in $(seq 1 30); do
    if curl -sf http://localhost:"$STORE_PORT"/catalog | grep -q "Plant Market" \
        && curl -sf -o /dev/null http://localhost:5173/; then
        break
    fi
    sleep 1
done

if ! curl -sf http://localhost:"$STORE_PORT"/catalog | grep -q "Plant Market"; then
    echo "Storefront did not start on :$STORE_PORT; see /tmp/plant-market-storefront.log" >&2
    exit 1
fi
if ! curl -sf -o /dev/null http://localhost:5173/; then
    echo "Admin did not start; see /tmp/plant-market-admin.log" >&2
    exit 1
fi

cat <<EOF

Plant Market demo is up.

Storefront:  http://localhost:$STORE_PORT
Admin:       http://localhost:5173
MailHog:     http://localhost:8025
WireMock:    http://localhost:8089/__admin
Gateway:     http://localhost:8080

Customer: customer@plantmarket.local / CustomerPassword123!
Admin:    admin@plantmarket.local    / AdminPassword123!

Scenarios: docs/demo.md
Reset data: DEMO_RESET_CONFIRM=yes ./scripts/demo-reset.sh
Stop:       ./scripts/demo-down.sh
EOF
