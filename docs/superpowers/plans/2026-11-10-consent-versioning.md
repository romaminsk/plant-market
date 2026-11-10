# Consent Versioning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-prompt for cookie consent when the policy version changes and record the accepted version and timestamp.

**Architecture:** Keep the existing `pm.consent.v1` choice value; add a `pm.consent.meta.v1` JSON record with `version` and `acceptedAt`. `readConsent()` returns a choice only when the metadata version matches the current `CONSENT_VERSION`.

**Tech Stack:** Next.js 16 client components, localStorage, Playwright Java E2E.

**Spec:** `docs/superpowers/specs/2026-11-10-consent-versioning-design.md`

## Global Constraints

- Do not break the existing `pm.consent.v1=all|essential` value; generated scenarios read it.
- Analytics stays gated on `hasAnalyticsConsent()`.
- No server-side audit log in this change.
- Consent version is a single exported constant used by banner, legal page and tests.

## Review Focus

- Legacy consent without metadata re-prompts.
- A stale metadata version re-prompts even when the choice is `all`.
- Accepting writes the current version and an ISO timestamp.
- Declining analytics still stores `essential` with the current version.
- Server-rendered legal page shows the same version without hydration errors.

---

### Task 1: Versioned consent storage and UI

**Files:**

- Modify: `clients/storefront/src/lib/consent.ts`
- Modify: `clients/storefront/src/app/CookieBanner.tsx`
- Modify: `clients/storefront/src/app/legal/cookies/page.tsx`
- Test: `tests/e2e/src/test/java/com/plantmarket/e2e/ConsentAnalyticsE2ETest.java`

**Interfaces:**

- Produces: `CONSENT_VERSION: string`, `ConsentRecord` (`choice`, `version`, `acceptedAt`).
- Produces: `readConsent(): Consent | null`, `writeConsent(consent: Consent): void` with version checks.
- Consumes: existing `subscribeToConsent`, `hasAnalyticsConsent`.

- [ ] **Step 1: Write the failing E2E test**

Add to `ConsentAnalyticsE2ETest`:

```java
@Test
void staleConsentVersionAsksAgain() {
  page.navigate(baseUrl + "/catalog");
  page.evaluate(
      "() => { localStorage.setItem('pm.consent.v1', 'all');"
          + " localStorage.setItem('pm.consent.meta.v1',"
          + " JSON.stringify({version: 'old', acceptedAt: '2025-01-01T00:00:00.000Z'})); }");
  page.reload();
  page.getByRole(
          AriaRole.DIALOG,
          new com.microsoft.playwright.Page.GetByRoleOptions().setName("Cookie и аналитика"))
      .waitFor();
}
```

In `analyticsRequiresConsent`, after accepting, assert:

```java
Object meta =
    page.evaluate("() => JSON.parse(localStorage.getItem('pm.consent.meta.v1'))");
assertThat(((java.util.Map<?, ?>) meta).get("version"))
    .isEqualTo(
        page.getAttribute(
            "aside.cookie-banner", "data-consent-version"));
assertThat(((java.util.Map<?, ?>) meta).get("acceptedAt")).isNotNull();
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 25)" E2E_BASE_URL=http://localhost:3100 ./gradlew --no-daemon :tests:e2e:test --tests '*ConsentAnalyticsE2ETest'`
Expected: stale-version test fails because the banner stays hidden.

- [ ] **Step 3: Implement versioned storage**

In `consent.ts`:

```ts
export const CONSENT_VERSION = '2026-11-10';
const META_KEY = 'pm.consent.meta.v1';

export interface ConsentRecord {
  choice: Consent;
  version: string;
  acceptedAt: string;
}
```

`readConsent()` parses the meta record and returns the choice only when
`meta.version === CONSENT_VERSION`. `writeConsent()` stores the choice plus
`{version: CONSENT_VERSION, acceptedAt: new Date().toISOString()}`. Export `CONSENT_VERSION`.

Add `data-consent-version={CONSENT_VERSION}` to the banner `<aside>` and render the version on
`/legal/cookies` near the consent explanation.

- [ ] **Step 4: Run the test and verify it passes**

Run the same targeted command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add clients/storefront tests/e2e
git commit -m "feat(storefront): version cookie consent"
```

---

### Task 2: Docs and full verification

**Files:**

- Modify: `docs/adr/0014-consent-analytics.md`
- Modify: `docs/status.md`
- Modify: `docs/technical-guide.html`

- [ ] **Step 1: Update ADR/status/guide**

ADR-0014: replace “нет версионирования” with the implemented metadata key/version; keep server
audit as a prod follow-up. Status: mark consent versioning done, next item delivery slots/reviews.
Technical guide: note the version and re-prompt behavior.

- [ ] **Step 2: Build and lint**

Run: `pnpm lint && pnpm format:check && NEXT_PUBLIC_API_URL=http://localhost:8080 pnpm --filter @plant-market/storefront build`
Expected: PASS.

- [ ] **Step 3: Full E2E**

Reset demo data, rebuild the storefront with `NEXT_PUBLIC_SITE_URL=http://localhost:3100`, then:
`E2E_BASE_URL=http://localhost:3100 E2E_ADMIN_URL=http://localhost:5173 ./gradlew --no-daemon :tests:e2e:test --max-workers=1`
Expected: 478/478 (or the current count).

- [ ] **Step 4: Commit**

```bash
git add docs
git commit -m "docs: document consent versioning"
```
