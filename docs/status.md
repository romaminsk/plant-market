# Plant Market — статус проекта

**Обновлено:** 2026-11-05
**Фаза:** 2 — orders, резервы, оплата, доставка и уведомления готовы
**Текущая задача:** Phase 2 gate — J2/J3/J4 E2E и A11

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
| J5 order/reserve/cancel                                    | ✅ Phase 2  |

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
- CI run `42176845701` — success, E2E 7/7 passed (Lighthouse, order reservation, sandbox payment); security run `42176845551` — success.
- **Admin update forms:** редактирование продуктов, категорий, вариантов и экземпляров; E2E проверяет обновление продукта.
- **CSRF double-submit:** `XSRF-TOKEN` + `_csrf`, session cookie `PM-SESSION`; prod — `__Host-`/Secure.
- **Auth Phase 1:** OAuth2 Authorization Code + PKCE (SAS 7.1.2), публичные клиенты storefront/admin, refresh rotation, сессионная cookie и CSRF на форме логина; ADR-0006. `OAuth2PkceIntegrationTest` проходит.

### Phase 2 — orders и резервы (✅)

- `order-service`: агрегат Order + OrderItem + status history + outbox, порт 8083, `pm_orders`.
- Checkout-сага: reserve → PENDING_PAYMENT; pay → confirm; cancel → release.
- `catalog-service`: таблица reservations, TTL 15 минут, `reserved_quantity` у вариантов, scheduled expiry; резерв вариантов и индивидуальных растений.
- Gateway route `/api/v1/orders/**`; order-service в compose с healthcheck.
- J5 API E2E: create → checkout → cancel → reservation released; отдельный integration-тест на reserve/confirm/release.
- `payment-service`: sandbox-провайдер через WireMock, webhook с `X-Provider-Secret`, статусы
  CREATED/PENDING/CONFIRMED/FAILED/REFUNDED; `/pay` подтверждает резерв только после CONFIRMED.
- ADR-0007: платёжный sandbox; реальный провайдер и refund flow — перед prod/Phase 3.
- Delivery zones + winter thermal rule; `CheckoutRequest` с `shippingZoneId`/`thermalPack`.
- Outbox publisher на Spring events + MailHog-уведомления на PAID/CANCELLED (ADR-0008).

## Тесты и покрытие

| Модуль            | Тесты | Line coverage |
| ----------------- | ----: | ------------: |
| `platform-common` |     9 |           74% |
| `auth-service`    |     3 |           88% |
| `catalog-service` |     5 |           83% |
| `order-service`   |     2 |           73% |
| `payment-service` |     1 |           77% |
| `api-gateway`     |     8 |           96% |
| `tests:e2e`       |     8 |             — |

Итого **28 backend-тестов + 8 E2E = 36 тестов, 0 падений**. JaCoCo-порог 70% проходит.

## Следующие 3 шага

1. Phase 2 gate: J2 (restock), J3 (зимняя доставка UI), J4 (претензия) E2E.
2. Phase 2: закрыть A11-чеклист и добавить retry/outbox dead-letter.
3. Phase 3: WYSIWYG-галерея, waitlist, care-напоминания.

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
