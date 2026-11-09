#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

if [[ "${DEMO_RESET_CONFIRM:-}" != "yes" ]]; then
    echo "Refusing to wipe demo data. Re-run with DEMO_RESET_CONFIRM=yes." >&2
    exit 1
fi

POSTGRES_USER="${POSTGRES_USER:-plantmarket}"
psql_cmd=(docker compose exec -T postgres psql -U "$POSTGRES_USER" -v ON_ERROR_STOP=1 -q)

"${psql_cmd[@]}" -d pm_catalog -c \
    "TRUNCATE reservations, restock_subscriptions, categories CASCADE;"
"${psql_cmd[@]}" -d pm_orders -c \
    "TRUNCATE orders CASCADE; TRUNCATE care_reminders CASCADE; TRUNCATE outbox_events; TRUNCATE shipping_zones CASCADE;"
"${psql_cmd[@]}" -d pm_payments -c \
    "TRUNCATE payments CASCADE; TRUNCATE payment_webhook_events;"
"${psql_cmd[@]}" -d pm_auth -c "TRUNCATE users CASCADE;"

./scripts/seed.sh
