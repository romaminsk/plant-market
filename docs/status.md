# Plant Market — статус проекта

**Обновлено:** 2026-11-05
**Фаза:** 1 — завершена (кроме J5 order/reserve — Phase 2)
**Текущая задача:** Phase 2 — order-service и резервы

## Состояние окружения

| Компонент      | Факт                                                           |
| -------------- | -------------------------------------------------------------- |
| Репозиторий    | `romaminsk/plant-market` (private), ветка `main`               |
| Локальный путь | `/Users/roma/projects/plant-market`                            |
| Colima         | запущена: `aarch64`, 4 CPU, 4 GiB RAM, 30 GiB disk, `virtiofs` |
| Docker         | client 29.9.2 / server 29.5.2, compose 5.6.1                   |
| Java           | 25.0.5.0.3 через `/usr/libexec/java_home -v 25`                |
| Node           | 24.22.2, pnpm 11.24.0                                          |
| Диск           | 29 GiB свободно; аварийный порог — 20 GiB                      |
| gh CLI         | `romaminsk`; scope `workflow` отсутствует                      |

## Phase 1 — прогресс

| Направление                                                | Статус      |
| ---------------------------------------------------------- | ----------- |
| ProductVariant                                             | ✅          |
| IndividualPlant + state machine                            | ✅          |
| StockBatch + quarantine/release                            | ✅          |
| Seed Phase 1                                               | ✅          |
| Storefront: фильтры + WYSIWYG                              | ✅          |
| SEO (metadata, robots, sitemap)                            | ✅          |
| Admin CRUD (create/read/archive/delete/transition/release) | ✅          |
| Lighthouse A11y                                            | ✅ 1.0 (CI) |
| OAuth2 Authorization Code + PKCE                           | ✅          |
| Cookie security / CSRF                                     | ✅          |
| Admin update forms                                         | ✅          |
| J5 order/reserve/cancel                                    | Phase 2     |

## Сделано

### Phase 0 (✅)

- Три сервиса, compose, JWT, Argon2id, Flyway, rate limit 60/10, CI/security workflows, E2E, Allure, документация.
- CI run `42064636389` и security `42064636402` — success.

### Phase 1 — каталог и UI (✅)

- `catalog-service`: `product_variants`, `individual_plants`, `stock_batches`; миграция `V2`.
- Инварианты: допустимые переходы IndividualPlant проверяются сервисом; `AVAILABLE → DELIVERED` запрещён.
- API: варианты продукта, индивидуальные растения с фильтрами `variegated/light/maxPriceMinor/status`, партии с `release` и `adjust`.
- `scripts/seed.sh`: 3 категории, 10 продуктов, 2 варианта, 5 индивидуальных растений (IP-001…IP-005), партия на 10 единиц; идемпотентен.
- Storefront: фильтры (вариегатные, свет, цена), карточки WYSIWYG, `/plants/[id]`, SEO metadata, `robots.txt`, `sitemap.xml`.
- Admin: вкладки продуктов, категорий, экземпляров, вариантов, партий; создание, архивирование/удаление, перевод в карантин/публикацию, списание, выпуск партии.
- Lighthouse accessibility: **1.0** (порог 0.95).
- Rate limit: 60 rpm публично, 10 rpm login/register, 300 rpm для запросов с Bearer-токеном (admin).
- CI run `42170141399` — success, E2E 5/5 passed (включая Lighthouse 1.0); security run `42170140548` — success.
- **Admin update forms:** редактирование продуктов, категорий, вариантов и экземпляров; E2E проверяет обновление продукта.
- **CSRF double-submit:** `XSRF-TOKEN` + `_csrf`, session cookie `PM-SESSION`; prod — `__Host-`/Secure.
- **Auth Phase 1:** OAuth2 Authorization Code + PKCE (SAS 7.1.2), публичные клиенты storefront/admin, refresh rotation, сессионная cookie и CSRF на форме логина; ADR-0006. `OAuth2PkceIntegrationTest` проходит.

## Тесты и покрытие

| Модуль            | Тесты | Line coverage |
| ----------------- | ----: | ------------: |
| `platform-common` |     9 |           74% |
| `auth-service`    |     3 |           88% |
| `catalog-service` |     3 |           81% |
| `api-gateway`     |     8 |           96% |
| `tests:e2e`       |     5 |             — |

Итого **23 backend-теста + 5 E2E = 28 тестов, 0 падений**. JaCoCo-порог 70% проходит.

## Следующие 3 шага

1. Phase 2: order-service — корзина, checkout, резервы с TTL.
2. Phase 2: payment-service sandbox + WireMock.
3. Phase 2: delivery zones, notifications, outbox на Spring events.

## Блокеры и риски

- Remote workflow использует `actions/*@v4`; обновление до `@v7`/`setup-java@v6` требует `gh auth refresh -s workflow`. CI на `@v4` зелёный.
- A11 T2: double-submit CSRF реализован; `__Host-`/Secure cookie включаются prod-конфигом, проверить в staging.
- In-memory SAS authorizations теряются при рестарте auth-service; для одного экземпляра Phase 1 допустимо (ADR-0006).
- In-memory rate limit не разделяется между экземплярами gateway (ADR-0005).
- Прямые порты auth/catalog открыты на localhost; перед prod закрыть.
- MailHog `v1.0.1` — только `linux/amd64`.
- Диск 29 GiB; чистить `docker system prune -f` после сессий.

## Решения сессии

- **Ruling:** Phase 1 каталог хранит Product как описание, продаваемые сущности — Variant и IndividualPlant.
- **Ruling:** партии и экземпляры проходят явные state transitions; недопустимый переход — 409.
- **Ruling:** J1-фильтр реализован на `/api/v1/individual-plants` (variegated + light + maxPriceMinor).
- **Ruling:** Lighthouse запускается `scripts/lighthouse-a11y.sh` и как `LighthouseA11yTest` в E2E-сьюте; порог 0.95.
- **Ruling:** rate limit: 60 rpm публично, 10 rpm login/register, 300 rpm для запросов с Bearer-токеном.
- **Ruling:** OAuth2 PKCE — SAS 7.1.2 с публичными клиентами и кастомным refresh-token генератором; ADR-0006.
- **Ruling:** CSRF — double-submit cookie `XSRF-TOKEN`; session cookie `PM-SESSION`/`__Host-` prod.
