# Staging Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a one-command staging stack with HTTPS, Docker secrets and MinIO-backed media.

**Architecture:** Caddy terminates TLS and routes app/admin/api subdomains; app containers read Docker secrets through a shared entrypoint; catalog-service writes media to MinIO through an S3 implementation. Dev compose remains the default.

**Tech Stack:** Caddy 2, Docker Compose, Docker secrets, MinIO, AWS SDK for Java S3, Next standalone, nginx.

**Spec:** `docs/superpowers/specs/2026-11-10-staging-infrastructure-design.md`

## Global Constraints

- Dev/demo compose and E2E must keep working.
- `MEDIA_STORAGE` defaults to `filesystem`; S3 is opt-in.
- Secrets never appear in committed files or normal service env in staging.
- TLS local mode uses Caddy's internal CA; ACME is configuration-only here.
- Media public URL stays `${MEDIA_PUBLIC_BASE_URL}/<key>`.

## Review Focus

- An upload with `MEDIA_STORAGE=s3` returns a URL that Caddy can serve from MinIO.
- Missing `/run/secrets` leaves dev startup unchanged.
- A missing required secret fails fast in staging.
- `__Host-`/Secure cookies and OAuth redirects work on HTTPS subdomains.
- Dev E2E still passes after Dockerfile changes.

---

### Task 1: S3 media storage

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `services/catalog-service/build.gradle.kts`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/media/FileSystemMediaStorage.java`
- Create: `services/catalog-service/src/main/java/com/plantmarket/catalog/media/S3MediaStorage.java`
- Create: `services/catalog-service/src/test/java/com/plantmarket/catalog/media/S3MediaStorageTest.java`
- Modify: `services/catalog-service/src/main/resources/application.yml`

**Interfaces:**

- Produces: `S3MediaStorage(MediaStorage)` active when `app.media.storage=s3`.
- Produces: `store(byte[] content, String contentType, String extension) -> String` using key `media/<uuid>.<ext>`.
- Consumes: `S3Client` (AWS SDK v2), bucket and public base URL config.

- [ ] **Step 1: Write the failing unit test**

Mock `S3Client` and construct `S3MediaStorage` with it; call `store(png, "image/png", "png")`; assert the returned URL starts with the configured base and capture `PutObjectRequest` to assert bucket, key prefix, content type.

- [ ] **Step 2: Run it and verify it fails**

`./gradlew :services:catalog-service:test --tests '*S3MediaStorageTest'` → class not found.

- [ ] **Step 3: Implement S3MediaStorage**

AWS SDK `s3` dependency, `@ConditionalOnProperty(name="app.media.storage", havingValue="s3")`, path-style client for MinIO, `putObject` with bytes/content type. Add `@ConditionalOnProperty(name="app.media.storage", havingValue="filesystem", matchIfMissing=true)` to the filesystem bean. Config keys: endpoint, region, bucket, access-key, secret-key, public-base-url.

- [ ] **Step 4: Run the test and the catalog suite**

Expected: PASS.

- [ ] **Step 5: Commit**

`feat(catalog): store media in S3-compatible object storage`

---

### Task 2: Docker secrets entrypoint and staging secrets

**Files:**

- Create: `infra/docker/secret-entrypoint.sh`
- Modify: all five `services/*/Dockerfile`
- Create: `scripts/staging-init.sh`
- Modify: `.gitignore`
- Create: `.env.staging.example`

**Interfaces:**

- Produces: entrypoint that exports `/run/secrets/*` basenames and execs the Java command.
- Produces: `scripts/staging-init.sh` generating `.env.staging` and `infra/staging/secrets/*.txt`.

- [ ] **Step 1: Add the entrypoint and Dockerfile wiring**

Shell script: loop over `/run/secrets/*`, `export "$(basename "$f")=$(cat "$f")"`, then `exec "$@"`. Add to every Java image before `ENTRYPOINT`.

- [ ] **Step 2: Add the init script**

Generate strong random values with `openssl rand -base64 48`; write secret files mode 600 and `.env.staging` with domain/URLs/TLS mode. Refuse to overwrite without `--force`.

- [ ] **Step 3: Verify dev startup is unchanged**

Rebuild one image and start dev compose; health endpoint still OK. No `/run/secrets` means the loop is a no-op.

- [ ] **Step 4: Commit**

`feat(staging): load application secrets from docker secrets`

---

### Task 3: Caddy TLS, frontends and MinIO compose

**Files:**

- Create: `infra/staging/Caddyfile`
- Create: `infra/staging/admin-nginx.conf`
- Create: `clients/storefront/Dockerfile`
- Create: `clients/admin/Dockerfile`
- Modify: `clients/storefront/next.config.ts` (standalone + tracing root)
- Modify: `clients/admin/vite.config.ts` (CSP from `VITE_API_URL`)
- Create: `docker-compose.staging.yml`
- Modify: `scripts/staging-init.sh` (build args)

**Interfaces:**

- Produces: `docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file .env.staging up`.
- Caddy routes `app.<domain>` to storefront, `admin.<domain>` to admin, `/api|/oauth2|/login|/media` to gateway/MinIO.
- Catalog uses `MEDIA_STORAGE=s3`, `S3_ENDPOINT=http://minio:9000`, bucket `plant-market-media`.

- [ ] **Step 1: Add frontend images**

Storefront standalone Dockerfile with build args `NEXT_PUBLIC_API_URL`/`NEXT_PUBLIC_SITE_URL`; admin static nginx image with `VITE_API_URL`.

- [ ] **Step 2: Add Caddy and MinIO**

Caddy internal TLS for local; media rewrite to `/plant-market-media{uri}`; MinIO + `mc` init creating the bucket and anonymous download.

- [ ] **Step 3: Add the staging overlay**

Docker secrets for app/DB, secure cookies, `__Host-` session, HTTPS issuer/redirect/CORS, S3 media env, storefront/admin services.

- [ ] **Step 4: Smoke test**

Start the stack, then verify:
`curl -k https://app.<domain>/`, `/api/v1/products`, `/login`, `https://admin.<domain>/`;
upload a PNG through `/api/v1/media` and fetch the returned `/media/...` URL;
`mc ls` shows the object.

- [ ] **Step 5: Commit**

`feat(staging): add TLS edge, container frontends and MinIO`

---

### Task 4: Docs and regression verification

**Files:**

- Create: `docs/runbooks/staging.md`
- Modify: `docs/status.md`
- Modify: `docs/technical-guide.html`
- Modify: `docs/adr/0011-media-storage.md`

- [ ] **Step 1: Document the runbook**

Init, start, TLS trust, ACME switch, secret rotation, MinIO bucket check, known limits.

- [ ] **Step 2: Regression**

Run full backend build and the dev full E2E after `demo-up`; expected green.

- [ ] **Step 3: Commit**

`docs: add staging runbook and object-storage notes`
