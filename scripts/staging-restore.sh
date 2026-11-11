#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

if [[ "${RESTORE_CONFIRM:-}" != "yes" ]]; then
    echo "This destroys staging databases and media. Re-run with RESTORE_CONFIRM=yes." >&2
    exit 1
fi

SOURCE="${1:?usage: RESTORE_CONFIRM=yes scripts/staging-restore.sh <backup-dir>}"
if [[ ! -f "$SOURCE/manifest.txt" ]]; then
    echo "Not a staging backup directory: $SOURCE" >&2
    exit 1
fi
ABS_SOURCE="$(cd "$SOURCE" && pwd)"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file .env.staging)
POSTGRES_USER="${POSTGRES_USER:-plantmarket}"

echo "Stopping application services"
"${COMPOSE[@]}" stop auth-service catalog-service order-service payment-service api-gateway storefront admin caddy seaweedfs

for db in pm_auth pm_catalog pm_orders pm_payments; do
    echo "Restoring $db"
    "${COMPOSE[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d postgres \
        -v ON_ERROR_STOP=1 -c "drop database if exists $db with (force)"
    "${COMPOSE[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d postgres \
        -v ON_ERROR_STOP=1 -c "create database $db"
    gunzip -c "$SOURCE/$db.sql.gz" \
        | "${COMPOSE[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d "$db" -v ON_ERROR_STOP=1 >/dev/null
done

SEAWEED_CONTAINER="$("${COMPOSE[@]}" ps -aq seaweedfs | head -1)"
CADDY_CONTAINER="$("${COMPOSE[@]}" ps -aq caddy | head -1)"
if [[ -z "$SEAWEED_CONTAINER" ]]; then
    echo "seaweedfs container not found" >&2
    exit 1
fi

echo "Restoring SeaweedFS data"
docker run --rm --volumes-from "$SEAWEED_CONTAINER" -v "$ABS_SOURCE":/backup alpine:3.22 \
    sh -c 'rm -rf /data/* && tar xzf /backup/seaweedfs-data.tar.gz -C /data'

if [[ -n "$CADDY_CONTAINER" && -f "$SOURCE/caddy-data.tar.gz" ]]; then
    echo "Restoring Caddy data"
    docker run --rm --volumes-from "$CADDY_CONTAINER" -v "$ABS_SOURCE":/backup alpine:3.22 \
        sh -c 'rm -rf /data/* && tar xzf /backup/caddy-data.tar.gz -C /data'
fi

echo "Starting staging"
"${COMPOSE[@]}" up -d --wait
echo "Restore complete from $ABS_SOURCE"
