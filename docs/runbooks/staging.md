# Staging runbook

## Prerequisites

- Docker/Compose and `openssl`, `jq`.
- Ports 80/443 free (dev stack stopped).
- DNS for a real domain, or `*.localhost` locally.

## First start

```bash
./scripts/staging-init.sh
JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew \
  :services:auth-service:bootJar :services:catalog-service:bootJar \
  :services:order-service:bootJar :services:payment-service:bootJar \
  :services:api-gateway:bootJar
docker compose -f docker-compose.yml -f docker-compose.staging.yml \
  --env-file .env.staging build
docker compose -f docker-compose.yml -f docker-compose.staging.yml \
  --env-file .env.staging up -d --wait
```

Seed once (the seed script uses the gateway directly, not TLS):

```bash
COMPOSE_PROJECT_NAME=plant-market-staging API_URL=http://localhost:8080 ./scripts/seed.sh
```

URLs: `https://staging.localhost/`, `https://admin.staging.localhost/`.
`/api/v1/**`, `/oauth2/**`, `/login`, `/media/**` are routed by Caddy.

## TLS

Local mode uses Caddy's internal CA. Trust it for browser testing:

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml \
  --env-file .env.staging exec caddy \
  cat /data/caddy/pki/authorities/local/root.crt > /tmp/plant-market-ca.crt
curl --cacert /tmp/plant-market-ca.crt https://staging.localhost/
```

For a public staging domain, point DNS at the host, then:

```bash
STAGING_TLS=acme ACME_EMAIL=ops@example.com \
  ./scripts/staging-init.sh --force
docker compose ... up -d --wait caddy
```

ACME was not verified in this environment; ports 80/443 must be reachable.

## Secrets

- App/DB secrets live in `infra/staging/secrets/*.txt` (mode 600) and are mounted as
  `/run/secrets/*`; `infra/docker/secret-entrypoint.sh` exports them before Java starts.
- `.env.staging` contains non-secret config plus MinIO/SeaweedFS root/S3 keys (the SeaweedFS
  image does not read Docker secret files); it is mode 600 and git-ignored.
- Rotate with `./scripts/staging-init.sh --force` and `up -d --force-recreate`.
- Check that app secrets are not in the container env: `docker inspect <container>` shows only the
  compose placeholder, the real value is exported by the entrypoint at runtime.

## Media (SeaweedFS S3)

- `seaweedfs` exposes S3 on `:8333` inside the compose network; `s3-init` creates the
  `plant-market-media` bucket.
- The bucket is private. `catalog-service` streams objects through `/media/**`; Caddy routes that
  path to the gateway.
- Smoke test: log in, `POST /api/v1/media` with a PNG, fetch the returned URL, compare bytes, then
  `aws --endpoint-url http://seaweedfs:8333 s3 ls s3://plant-market-media`.
- SeaweedFS runs single-node with a named volume; it has no replication or backup. Before prod use
  a managed S3 or back up the volume.

## Backup and restore

Create a timestamped backup (Postgres dumps, SeaweedFS volume, Caddy data, manifest):

```bash
./scripts/staging-backup.sh
```

Backups go to `backups/staging/<UTC timestamp>/`, are git-ignored, and files older than 14 days are
removed. Verify one archive before relying on it:

```bash
tar tzf backups/staging/<timestamp>/seaweedfs-data.tar.gz | head
gzip -t backups/staging/<timestamp>/pm_orders.sql.gz
```

Restore is destructive and requires an explicit confirmation:

```bash
RESTORE_CONFIRM=yes ./scripts/staging-restore.sh backups/staging/<timestamp>
```

The restore stops the app services, recreates the four databases, restores SeaweedFS/Caddy volumes,
then starts the stack. Rehearse this on a disposable staging host before prod.

## Stop / reset

```bash
docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file .env.staging down
# wipe staging data and object storage:
docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file .env.staging down -v
```

## Known limits

- The base compose still publishes Postgres/gateway/WireMock/MailHog on `127.0.0.1`; a real staging
  host should firewall them and expose only Caddy.
- Docker secrets are file-based, not Vault/SOPS. Rotation requires recreating containers.
- No backups, HA, WAF, image resizing or malware scanning yet.
- MinIO official images are unavailable (ADR-0003); SeaweedFS is the supported S3-compatible
  staging backend (ADR-0017).
