# Plant Market — статус проекта

**Обновлено:** 2026-11-06
**Фаза:** 2 — gate закрыт по коду: J2/J3/J4 зелёные, A11-техника закрыта
**Текущая задача:** Phase 3 — галерея и care-напоминания готовы, далее MinIO и outbox retry

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
- **J2 restock:** `restock_subscriptions` в `catalog-service`, публичная идемпотентная подписка,
  уведомление при release/adjust/переходе в AVAILABLE, статус `PENDING → NOTIFIED` (ADR-0009).
- **J4 claims:** `order_claims` + фото-URL в `order-service`, решения `REPLACEMENT/REFUND/REJECT`,
  роли `ADMIN/OPERATOR/MANAGER`, письмо после коммита (ADR-0009).
- **J3:** зимняя зона без термоупаковки → 409, с термоупаковкой — доплата в `shippingMinor`.
- **A11 техника:** gateway отдаёт CSP, Referrer-Policy, Permissions-Policy, X-Frame-Options,
  X-Content-Type-Options; HSTS — только на HTTPS. Логи новых модулей без e-mail.
- CI run `42182462585` — success, E2E 8/8 на тот момент; security run `42182462495` — success
  (trivy, secrets/TruffleHog, dependency audit).
- CI run `42186081173` — success, E2E 10/10 с WYSIWYG-галереей и общими API-токенами.
- CI run `42187187601` — success, E2E 11/11 с care-напоминаниями.
- CI run `42188535932` — success, E2E 12/12 с media uploads и waitlist UI.
- CI run `42189452781` — success, E2E 12/12 с outbox retry.

### Phase 3 — WYSIWYG-галерея (✅ первый шаг)

- `individual_plant_photos`: 4 ракурса на экземпляр (`GENERAL/LEAF/ROOTS/DEFECTS`), миграция `V5`.
- `PUT /api/v1/individual-plants/{id}/photos` заменяет галерею; требуется минимум один `GENERAL`,
  максимум 8 фото. `photoUrl` синхронизируется с первым фото для старых клиентов.
- Storefront: интерактивная галерея с подписями, alt-текстами, бейджем «Что видите — то и
  получите», гарантией 48 ч и заметкой о зимней доставке; карточки каталога показывают первое фото.
- Seed добавляет по 4 placeholder-ракурса для IP-001…IP-005, не перезаписывая реальные фото.
- E2E J1 проверяет переход в карточку и цену; integration-тест — замену галереи и 400 без GENERAL.

### Phase 3 — админ-панель претензий (✅)

- Вкладка «Претензии»: список, фото-ссылки, решение `REPLACEMENT/REFUND/REJECT` и комментарий.
- E2E `AdminClaimsUiTest`: создаёт оплаченный заказ и претензию через API, оператор решает её в UI.
- Admin UI-тесты используют общий API-токен в `localStorage` вместо повторного логина — suite
  укладывается в лимит 10 login/мин (seed + один UI-логин + два API-логина).

### Phase 3 — админ-редактор галереи (✅)

- В админке при редактировании экземпляра появляется блок «Галерея WYSIWYG»: 4 ракурса,
  загрузка файла в `/api/v1/media`, сохранение через `PUT /individual-plants/{id}/photos`.
- E2E `AdminGalleryUiTest`: оператор открывает экземпляр, сохраняет галерею, видит подтверждение.
- Загрузка ограничена JPEG/PNG/WebP до 10 MB; кнопки блокируются во время запроса.

### Phase 3 — outbox retry (✅)

- `outbox_events`: `attempts`, `last_error`, `next_attempt_at`, `dead`; publisher выбирает только
  готовые к попытке события (ADR-0012).
- Ошибка SMTP в `OrderNotificationListener` теперь доходит до publisher: до 5 попыток с
  экспоненциальной задержкой 30 с…1 ч, затем dead-letter.
- Integration-тест: падение SMTP → `attempts=1` и будущий `next_attempt_at`, затем успешная
  публикация после восстановления.

### Phase 3 — загрузка фото (✅)

- `POST /api/v1/media` в `catalog-service`: JPEG/PNG/WebP до 10 MB, файлы на volume `media-data`,
  публичная выдача `/media/**` (ADR-0011).
- `MediaStorage` — точка замены на MinIO/S3/CDN без изменения API.
- Gateway маршрутизирует `/api/v1/media/**`; загрузка требует JWT.
- Integration-тест проверяет загрузку PNG, выдачу по URL и 400 для `text/plain`.

### Phase 3 — waitlist UI (✅)

- На карточке недоступного растения (`status != AVAILABLE`) показывается форма «Появится снова?»
  с e-mail; подписка уходит в `POST /api/v1/restock-subscriptions` (`INDIVIDUAL_PLANT`).
- Успех/ошибка объявляются через `role="status"`, поле e-mail доступно с клавиатуры.
- E2E `WaitlistUiTest`: создаёт недоступный экземпляр через API, открывает карточку, подписывается
  и проверяет подтверждение.
- CORS gateway по умолчанию разрешает storefront на `:3100` (production-сборка Next в E2E/CI).

### Phase 3 — care-напоминания (✅)

- `care_reminders` в `order-service`: одна запись на позицию оплаченного заказа, интервал 1–365 дней.
- Opt-in `POST /api/v1/orders/{orderId}/care-reminders`, список, opt-out `DELETE /api/v1/care-reminders/{id}`.
- Планировщик раз в 60 секунд отправляет до 50 писем; при ошибке SMTP напоминание повторяется
  на следующем цикле (ADR-0010).
- E2E: создание, список, отключение; integration-тест проверяет отправку due-напоминания через
  мок `JavaMailSender` и 409 для неоплаченного заказа.

## Тесты и покрытие

| Модуль            | Тесты | Line coverage |
| ----------------- | ----: | ------------: |
| `platform-common` |     9 |           74% |
| `auth-service`    |     3 |           89% |
| `catalog-service` |     8 |           85% |
| `order-service`   |     5 |           77% |
| `payment-service` |     1 |           77% |
| `api-gateway`     |     8 |           97% |
| `tests:e2e`       |    14 |             — |

Итого **34 backend-теста + 14 E2E = 48 тестов, 0 падений**. JaCoCo-порог 70% проходит.

## Следующие 3 шага

1. Phase 3: redrive dead-letter и UI загрузки фото для претензий.
2. Phase 3: staging-метрики A1 и p95 в бюджете A15; замена volume на MinIO/S3.
3. Юридический A11 (L1–L8): передать владельцу/юристу; до подписи prod запрещён.

## Блокеры и риски

- Remote workflow использует `actions/*@v4`; обновление до `@v7`/`setup-java@v6` требует `gh auth refresh -s workflow`. CI на `@v4` зелёный.
- A11 T2: double-submit CSRF реализован; `__Host-`/Secure cookie включаются prod-конфигом, проверить в staging.
- In-memory SAS authorizations теряются при рестарте auth-service; для одного экземпляра Phase 1 допустимо (ADR-0006).
- In-memory rate limit не разделяется между экземплярами gateway (ADR-0005).
- Прямые порты auth/catalog открыты на localhost; перед prod закрыть.
- MailHog `v1.0.1` — только `linux/amd64`.
- A11 L1–L8 (торговый реестр, СКНО/ЕРИП, ПДн, CITES, оферта, налоги) не закрыты: нужен
  владелец/юрист. Phase 2 закрыт по коду, но не является разрешением на prod.
- Уведомления restock/claims best-effort: нет retry/dead-letter; при недоступном SMTP письмо
  теряется (статус подписки/претензии при этом сохраняется). Order-уведомления ретраятся (ADR-0012).
- Outbox at-least-once: возможны дубли писем; dead-letter без автоматического redrive.
- Фото принимаются как URL; API загрузки есть, UI загрузки в админке — следующий шаг.
- Media volume не реплицируется и не бэкапится; публичные файлы без content sniffing (ADR-0011).
- Галерея в seed использует placehold.co; фотостандарт (нейтральный фон, линейка, 4 ракурса)
  должен соблюдаться оператором при загрузке реальных фото.
- Диск 29 GiB; чистить `docker system prune -f` после сессий.

## Решения сессии

- **Ruling:** Phase 1 каталог хранит Product как описание, продаваемые сущности — Variant и IndividualPlant.
- **Ruling:** партии и экземпляры проходят явные state transitions; недопустимый переход — 409.
- **Ruling:** J1-фильтр реализован на `/api/v1/individual-plants` (variegated + light + maxPriceMinor).
- **Ruling:** Lighthouse запускается `scripts/lighthouse-a11y.sh` и как `LighthouseA11yTest` в E2E-сьюте; порог 0.95.
- **Ruling:** rate limit: 60 rpm публично, 10 rpm login/register, 300 rpm для запросов с Bearer-токеном.
- **Ruling:** OAuth2 PKCE — SAS 7.1.2 с публичными клиентами и кастомным refresh-token генератором; ADR-0006.
- **Ruling:** CSRF — double-submit cookie `XSRF-TOKEN`; session cookie `PM-SESSION`/`__Host-` prod.
- **Ruling:** restock-подписки — в `catalog-service` (источник правды об остатках), претензии —
  в `order-service` (привязка к заказу); отдельные сервисы не создаём до Phase 3+.
- **Ruling:** роли из JWT claim `roles` маппятся в `ROLE_*` в order-service; `/api/v1/claims/**`
  только `ADMIN/OPERATOR/MANAGER`.
- **Ruling:** security-заголовки выставляет gateway; CSP `default-src 'none'` для API, HSTS
  включается только на HTTPS.
- **Ruling:** care-напоминания — в `order-service`, opt-in на оплаченный заказ, интервал в днях;
  при ошибке SMTP письмо повторяется, `next_send_at` не сдвигается.
- **Ruling:** медиа — локальный volume за интерфейсом `MediaStorage`; MinIO/S3 подключается
  заменой bean, API `/api/v1/media` не меняется (ADR-0011).
- **Ruling:** order-уведомления ретраятся через outbox (5 попыток, backoff, dead-letter);
  claim/restock остаются best-effort (ADR-0012).
