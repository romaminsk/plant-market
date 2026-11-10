# Storefront slug URLs — design

- **Date:** 2026-11-10
- **Status:** Accepted (autonomous owner mode)
- **Scope:** catalog-service read paths + storefront links/SEO

## Intent

Product and category records already have unique slugs, and individual plants have unique
human-readable codes, but every storefront URL uses a UUID. This hurts SEO, makes links
unreadable, and blocks the pre-prod slug-URL item.

## Goals

- `GET /api/v1/products/{idOrSlug}`, `GET /api/v1/categories/{idOrSlug}` and
  `GET /api/v1/individual-plants/{idOrCode}` resolve either form.
- Storefront product links use `product.slug`; plant links use `plant.code`.
- Canonical URLs and sitemap use slugs/codes.
- Existing UUID URLs keep working (no breaking change, no redirects).
- Integration/E2E tests prove both forms resolve.

## Non-goals

- Category landing pages or category routes in the storefront.
- Slug history, redirects after a slug change, localized slugs.
- Changing admin APIs or write paths.

## Approach

Keep the existing `{id}` path shape and widen the parameter to `String`. In the service, parse a
UUID first and fall back to `findBySlug` / `findByCode`; this preserves every existing caller and
avoids a second endpoint. The storefront route parameter is still called `[id]`, but links and
sitemap now emit slugs/codes. The product page fetches variants with `product.id` after resolving
the product, because the variants endpoint remains UUID-based.

Individual plants have no slug column; `code` is already unique and human-readable, so it is the
canonical public identifier. UUIDs remain accepted.

## Consequences

- A slug that is also a valid UUID would resolve as an ID first; the slug pattern makes this
  practically impossible and it is documented.
- Canonical metadata changes for existing pages; old UUID URLs still render with a canonical slug.
- No migration or data backfill is required.
