# Staging infrastructure — design

- **Date:** 2026-11-10
- **Status:** Accepted (autonomous owner mode)
- **Scope:** local/staging compose stack with TLS, Docker secrets, MinIO/S3 media

## Intent

The demo stack runs plain HTTP with `.env` secrets and filesystem media. Staging needs HTTPS,
secrets that are not baked into environment files, and object storage that survives a container
restart and can be replaced by a cloud bucket.

## Goals

- Caddy terminates TLS for `app.<domain>`, `admin.<domain>` and `api.<domain>`; local mode uses
  Caddy's internal CA, public staging can switch to ACME.
- App secrets (JWT, DB password, webhook secret) are Docker secrets mounted at `/run/secrets` and
  exported by a shared entrypoint; `.env.staging` contains only non-secret config.
- Media uploads go to MinIO through an `S3MediaStorage` implementation; `/media/**` is served from
  the bucket through Caddy.
- Storefront and admin run as containers behind Caddy.
- The existing dev/demo stack stays unchanged and E2E remains green.

## Non-goals

- Kubernetes, cloud load balancers, HA Postgres/Redis, backups and disaster recovery.
- Vault/SOPS integration; Docker secrets are the staging mechanism, with a documented prod path.
- Real payment provider, CDN, image resizing or antivirus scanning.
- Public DNS/ACME verification in this environment; the ACME Caddyfile is provided but local
  verification uses the internal CA.

## Approaches considered

1. **Caddy + Docker secrets + MinIO** (chosen): smallest moving parts, automatic TLS, file-based
   secrets, S3-compatible storage.
2. nginx/Traefik + SOPS + cloud S3: closer to some prod setups, but adds a secret tool and external
   bucket credentials that cannot be verified locally.
3. Keep host-run frontends and only add MinIO/TLS to the gateway: less work, but not a staging stack
   and secure cookies/subdomain routing would be untested.

## Decisions

- Caddy is the only public entry point; app ports stay bound to localhost or internal networks.
- `infra/docker/secret-entrypoint.sh` exports every `/run/secrets/*` basename into the environment
  before exec; dev images work unchanged when the directory is absent.
- Postgres uses `POSTGRES_PASSWORD_FILE`; MinIO root/S3 keys come from `.env.staging` because the
  MinIO image does not read secret files, and the file is git-ignored with mode 600.
- `MEDIA_STORAGE=filesystem` remains the default; staging sets `s3`.
- S3 keys are `media/<uuid>.<ext>` and the bucket allows anonymous download.
- Storefront uses Next standalone output; admin is static files served by nginx.

## Consequences

- Staging media is public by URL, like the current filesystem; private documents would need
  presigned URLs.
- MinIO root credentials are env-based (file protected) rather than Docker secrets; app/DB secrets
  are not visible in `docker inspect`.
- A real public staging domain needs `STAGING_TLS=acme` and DNS pointing at the host.
