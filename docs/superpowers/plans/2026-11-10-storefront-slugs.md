# Storefront Slug URLs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make product and plant storefront URLs readable by resolving slugs/codes in the catalog API and emitting them from links, canonical tags and the sitemap.

**Architecture:** Keep the existing `{id}` path shape and accept `String`; resolve a UUID first, then fall back to the unique slug (products/categories) or code (individual plants). Storefront routes keep `[id]` as the parameter name but receive slugs/codes; UUID URLs remain valid.

**Tech Stack:** Spring Boot 4 / JPA, Next.js 16 App Router, JUnit 6, Playwright Java.

**Spec:** `docs/superpowers/specs/2026-11-10-storefront-slugs-design.md`

## Global Constraints

- Prices stay in minor units; no API shape changes beyond accepting slug/code in existing paths.
- UUID lookup must remain supported.
- No new migration; products/categories already have unique slugs, plants have unique codes.
- Keep RFC 7807 errors: unknown identifier returns the existing 404 problem.
- Storefront build must stay static/dynamic as today; no new client-side fetching.

## Review Focus

- Unknown slug and unknown UUID both return 404, not 500.
- A plant code containing uppercase letters/hyphens is accepted as a path value.
- Product page resolves variants by product UUID even when the URL used a slug.
- Sitemap and canonical URLs never emit UUIDs.
- Existing UUID E2E navigations keep working.

---

### Task 1: Catalog API resolves UUID or slug/code

**Files:**

- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/repository/ProductRepository.java`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/repository/IndividualPlantRepository.java`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/service/ProductService.java`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/service/CategoryService.java`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/service/IndividualPlantService.java`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/web/ProductController.java`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/web/CategoryController.java`
- Modify: `services/catalog-service/src/main/java/com/plantmarket/catalog/web/IndividualPlantController.java`
- Test: `services/catalog-service/src/test/java/com/plantmarket/catalog/CatalogFlowIntegrationTest.java`

**Interfaces:**

- Produces: `ProductResponse ProductService.get(String idOrSlug)`, `CategoryResponse CategoryService.get(String idOrSlug)`, `IndividualPlantResponse IndividualPlantService.get(String idOrCode)`.
- Produces: `Optional<ProductEntity> ProductRepository.findBySlug(String slug)`, `Optional<IndividualPlantEntity> IndividualPlantRepository.findByCode(String code)`.
- Consumes: existing `findById` methods and `ApiException.notFound`.

- [ ] **Step 1: Write failing integration assertions**

In `CatalogFlowIntegrationTest`, after creating a product with slug `monstera-deliciosa-<suffix>`, assert:

```java
ResponseEntity<ProductResponse> bySlug =
    restTemplate.getForEntity("/api/v1/products/" + productRequest.slug(), ProductResponse.class);
assertThat(bySlug.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(bySlug.getBody()).isNotNull();
assertThat(bySlug.getBody().id()).isEqualTo(productId);

ResponseEntity<String> missing =
    restTemplate.getForEntity("/api/v1/products/no-such-slug", String.class);
assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
```

Add the same style assertion for the category slug and, in the existing individual-plant fixture, for
`/api/v1/individual-plants/{code}`.

- [ ] **Step 2: Run the catalog test and verify it fails**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew --no-daemon :services:catalog-service:test --tests '*CatalogFlowIntegrationTest'`
Expected: FAIL because the controller rejects the slug as a UUID.

- [ ] **Step 3: Implement resolution**

Add repository finders, then make each service `get(String)` resolve:

```java
private UUID parseUuid(String value) {
  try {
    return UUID.fromString(value);
  } catch (IllegalArgumentException exception) {
    return null;
  }
}
```

`ProductService.get` uses `findById` when parseable, else `findBySlug`; `CategoryService` already has
`findBySlug`; `IndividualPlantService` uses `findByCode`. Unknown values throw the existing
`ApiException.notFound("... not found: " + value)`. Change only the GET controller path variables to
`String`; keep PUT/DELETE on UUID.

- [ ] **Step 4: Run the catalog test and verify it passes**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew --no-daemon :services:catalog-service:test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add services/catalog-service
git commit -m "feat(catalog): resolve products and plants by slug or code"
```

---

### Task 2: Storefront emits slug/code URLs

**Files:**

- Modify: `clients/storefront/src/app/catalog/page.tsx`
- Modify: `clients/storefront/src/app/catalog/[id]/page.tsx`
- Modify: `clients/storefront/src/app/plants/[id]/page.tsx`
- Modify: `clients/storefront/src/app/sitemap.ts`
- Modify: `tests/e2e/src/test/java/com/plantmarket/e2e/J1SmokeTest.java`
- Modify: `tests/e2e/src/test/java/com/plantmarket/e2e/J1FilterTest.java`

**Interfaces:**

- Consumes: catalog endpoints from Task 1.
- Produces: links `/catalog/{product.slug}` and `/plants/{plant.code}`; canonical/sitemap use the same.

- [ ] **Step 1: Update links and metadata**

- Catalog list: `href={`/catalog/${product.slug}`}`, `href={`/plants/${plant.code}`}`.
- Product page: fetch variants with `fetchVariants(product.id)`; canonical `/catalog/${product.slug}`.
- Plant page: canonical `/plants/${plant.code}`.
- Sitemap: `/catalog/${product.slug}`, `/plants/${plant.code}`.

- [ ] **Step 2: Update E2E URL assertions**

`J1SmokeTest` should match a slug, e.g. `.*/catalog/[a-z0-9-]+$`; `J1FilterTest` should match a
plant code, e.g. `.*/plants/[A-Z0-9-]+$`.

- [ ] **Step 3: Build and lint**

Run: `NEXT_PUBLIC_API_URL=http://localhost:8080 pnpm --filter @plant-market/storefront build && pnpm lint && pnpm format:check`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add clients/storefront tests/e2e
git commit -m "feat(storefront): use slug and plant-code URLs"
```

---

### Task 3: Contracts, docs and full verification

**Files:**

- Modify: `contracts/openapi/catalog-service.yaml`
- Modify: `docs/status.md`
- Modify: `docs/technical-guide.html`
- Create: `docs/adr/0015-slug-urls.md`

- [ ] **Step 1: Document the contract**

Update the three GET path parameters from UUID to string with description “UUID or slug/code”.
Add ADR-0015 with the resolution rule and UUID compatibility. Remove slug-URL from the pre-prod
blockers in `docs/status.md`; update the technical guide review row.

- [ ] **Step 2: Full backend and frontend verification**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 25)" JAVA_OPTS="-Xmx2g" DOCKER_HOST="unix:///Users/roma/.colima/default/docker.sock" TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew --no-daemon build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Full E2E**

Run `./scripts/demo-up.sh`, `DEMO_RESET_CONFIRM=yes ./scripts/demo-reset.sh`, then:
`E2E_BASE_URL=http://localhost:3100 E2E_ADMIN_URL=http://localhost:5173 ./gradlew --no-daemon :tests:e2e:test --max-workers=1`
Expected: 478/478.

- [ ] **Step 4: Commit**

```bash
git add contracts docs
git commit -m "docs: document slug URLs"
```
