#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file .env.staging)
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
ROOT="${BACKUP_DIR:-backups/staging}"
DEST="$ROOT/$STAMP"
POSTGRES_USER="${POSTGRES_USER:-plantmarket}"

mkdir -p "$DEST"
ABS_DEST="$(cd "$DEST" && pwd)"

echo "Backing up to $ABS_DEST"
for db in pm_auth pm_catalog pm_orders pm_payments; do
    "${COMPOSE[@]}" exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$db" \
        | gzip > "$DEST/$db.sql.gz"
    echo "  $db.sql.gz"
done

SEAWEED_CONTAINER="$("${COMPOSE[@]}" ps -q seaweedfs)"
if [[ -z "$SEAWEED_CONTAINER" ]]; then
    echo "seaweedfs container is not running" >&2
    exit 1
fi
docker run --rm --volumes-from "$SEAWEED_CONTAINER" -v "$ABS_DEST":/backup alpine:3.22 \
    tar czf /backup/seaweedfs-data.tar.gz -C /data .
echo "  seaweedfs-data.tar.gz"

CADDY_CONTAINER="$("${COMPOSE[@]}" ps -q caddy)"
if [[ -n "$CADDY_CONTAINER" ]]; then
    docker run --rm --volumes-from "$CADDY_CONTAINER" -v "$ABS_DEST":/backup alpine:3.22 \
        tar czf /backup/caddy-data.tar.gz -C /data .
    echo "  caddy-data.tar.gz"
fi

cat > "$DEST/manifest.txt" <<EOF
created_at=$STAMP
postgres_user=$POSTGRES_USER
databases=pm_auth,pm_catalog,pm_orders,pm_payments
EOF

# Keep two weeks of local staging backups.
find "$ROOT" -type f -mtime +14 -delete
echo "Backup complete: $ABS_DEST"
