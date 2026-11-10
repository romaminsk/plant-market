# Consent versioning — design

- **Date:** 2026-11-10
- **Status:** Accepted (autonomous owner mode)
- **Scope:** storefront consent storage, banner, legal cookie page

## Intent

Consent is stored as a bare `all`/`essential` string under `pm.consent.v1`. When the cookie policy
changes, returning visitors are never asked again, and analytics keeps running under an old
consent. ADR-0014 calls for `consent_version` and `accepted_at` before prod.

## Goals

- Store a policy version and acceptance timestamp next to the existing choice.
- Treat a missing or stale version as no consent: show the banner and disable analytics.
- Keep the existing `pm.consent.v1=all|essential` value for backward compatibility and existing
  E2E scenarios.
- Show the current policy version on `/legal/cookies`.
- E2E proves a stale version re-prompts and a fresh choice records the current version.

## Non-goals

- Server-side consent audit log or consent revocation API (remains a prod follow-up).
- Consent expiry by time; version changes are the re-consent trigger.
- Changing analytics event names or third-party scripts.

## Approach

Keep `pm.consent.v1` as the choice and add `pm.consent.meta.v1` as a JSON record:

```json
{ "version": "2026-11-10", "acceptedAt": "2026-11-10T09:00:00.000Z" }
```

`readConsent()` returns the choice only when the metadata exists and its `version` equals the
current `CONSENT_VERSION`. `writeConsent()` writes both keys and dispatches the existing event.
This forces one re-prompt for users with legacy consent (no metadata) and for users whose version
is old, without touching the generated scenario assertions that read `pm.consent.v1`.

The banner exposes `data-consent-version` and the legal page renders the same constant.

## Consequences

- Existing users see the banner once after deploy; declining keeps essential-only behavior.
- The choice key stays readable for support and tests; the metadata key is additive.
- Server audit/expiry are still required before prod and stay documented in ADR-0014.
