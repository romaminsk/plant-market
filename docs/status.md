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
- CI run `42191984783` — success, E2E 14/14 с админ-редактором галереи и панелью претензий.
- CI run `42193571529` — success, E2E 15/15 с OAuth2 PKCE checkout через storefront BFF.
- CI run `42201784705` — success, E2E 16/16 с consent-баннером.
- CI run `42203258263` — success, E2E 16/16 с адресом доставки.
- CI run `42204807910` — success, E2E 16/16 с timeline заказа и 400 без адреса.
- CI run `42212547510` — success после ревью: gateway authz, locks, security headers,
  media magic bytes, J3 UI, 17/17 E2E; backend 38 тестов.

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

### Визуальный рефреш и демо-фото (✅)

- Единая визуальная система storefront: липкая шапка с нормальными отступами, serif-заголовки,
  карточки, галерея, футер, адаптив; исправлены «слипшиеся» ссылки навигации и футер не внизу.
- Вместо нарисованных SVG — реальные фото растений (Unsplash/Wikimedia), локально в
  `clients/storefront/public/images/plants/` + `CREDITS.md`.
- Каталог: современная sidebar-панель фильтров (вариегатность, свет, цена), hero, 11 доступных
  экземпляров для проверки фильтра, скрытие нефільтруемых позиций при активном фильтре.
- E2E `CatalogFilterE2ETest`: 11 → 6 (вариегатные) → 3 (+ средний свет) → 1 (+ до 150 BYN).
- Админка: serif-заголовки, sticky topbar/tabs, таблицы на всю ширину, горизонтальный скролл.
- E2E 17/17 после визуальных изменений; Lighthouse A11y 1.0.

### Ревью 2026-11-06 и security-фиксы (✅)

- Полное ревью backend/frontend/E2E: `docs/review-2026-11-06.md`.
- Gateway: роли `ADMIN/OPERATOR/MANAGER` на мутации каталога, stock, shipping, media; reservation
  route убран из внешнего API; тесты на 401/403.
- Order: pessimistic lock на checkout/pay/cancel, тест конкурентной оплаты; `@Valid` checkout.
- Catalog: confirm резерва строже (409 после expiry, проверка plant), locks на reservations,
  restock без e-mail в ответе, media 10 MB + magic bytes, stock adjust без ложных уведомлений.
- Auth: refresh rotation под lock; redirect URI storefront/admin вынесены в env; logout
  отзывает refresh-токены пользователя.
- Payment: матрица переходов статусов; host-порты order/payment больше не публикуются.
- Storefront: open redirect закрыт, `__Host-` cookie при `COOKIE_SECURE=true`, CSP/headers,
  JSON-LD escaping, cart validation, BFF требует зону/адрес и отменяет заказ при сбое.
- Admin: CSP/X-Frame/Referrer/Permissions; JWT в localStorage остаётся pre-prod блокером.
- Restock: NOTIFIED только при успешной отправке; SMTP-сбой оставляет PENDING.
- Ошибки missing header / multipart >10 MB → 400 RFC 7807.

### Phase 3 — timeline заказа (✅)

- `GET /api/v1/orders/{id}/history` — статусы заказа с временем (DRAFT → PENDING_PAYMENT → PAID
  → CANCELLED и т.д.), только владелец.
- Storefront `/account/orders/{id}` показывает timeline; E2E проверяет его после оплаты.
- Integration-тест проверяет порядок статусов для отменённого заказа.

### Phase 3 — адрес доставки (✅)

- `orders.delivery_address` (V6); checkout с зоной требует непустой адрес, иначе 400.
- Storefront checkout: обязательное поле адреса, итог и оплата; адрес виден в деталях заказа.
- E2E checkout заполняет адрес; integration-тест проверяет 400 без адреса и сохранение адреса.
- Поля получателя/телефона и слоты доставки — следующая итерация.

### Phase 3 — cookie-consent и аналитика (✅)

- Баннер согласия (`all`/`essential`), выбор в `localStorage`; необходимые cookie работают без
  согласия (ADR-0014).
- События `view_item`, `add_to_cart`, `begin_checkout`, `purchase`, `restock_subscribe` пишутся
  в `window.dataLayer` только после согласия; сторонние трекеры не подключены.
- E2E `ConsentAnalyticsE2ETest`: до согласия событий нет, после — `add_to_cart` появляется.
- Юридические тексты (политика ПДн, оферта) всё ещё требуют юриста.

### Phase 3 — корзина, варианты и история заказов (✅)

- Storefront: `localStorage`-корзина, счётчик в шапке, страница `/cart`, смешанная корзина
  (индивидуальные растения + варианты), checkout на несколько позиций.
- Карточка продукта показывает варианты с ценой и доступным остатком; `availableQuantity`
  считается из партий и резервов в `catalog-service`.
- `/account/orders` и `/account/orders/{id}` — история и детали заказов; ссылки в шапке.
- BFF checkout обновляет access/refresh токены при 401 и повторяет запрос.
- E2E `CheckoutFlowE2ETest`: 2 растения + вариант → корзина (3) → PKCE-логин → оплата → история.

### Phase 3 — redrive dead-letter (✅)

- `GET /api/v1/admin/outbox/dead` и `POST /api/v1/admin/outbox/{id}/redrive` (ADMIN/OPERATOR/MANAGER).
- Redrive сбрасывает `dead`, `published_at`, `attempts`, `last_error`, `next_attempt_at`; событие
  снова попадает в обычный цикл публикации (ADR-0012).
- Integration-тест: dead-событие → redrive → attempts=0, publishedAt=null; customer получает 403.

### Phase 3 — storefront checkout (✅)

- OAuth2 PKCE через BFF в Next.js: `/api/auth/login` → auth-service `/oauth2/authorize` →
  `/auth/callback` (обмен code → tokens, httpOnly cookies) → `/checkout`.
- `POST /api/checkout`: create → checkout (зона/термоупаковка) → pay; ошибки order-service
  пробрасываются как JSON. POST защищён double-submit `pm.csrf` + `X-CSRF-Token`.
- Страница `/checkout` показывает состав корзины, зоны, зимнюю термоупаковку, адрес и итог;
  корзина и история заказов реализованы (ADR-0013).
- E2E `CheckoutFlowE2ETest`: гость → карточка → логин через форму auth-service → checkout →
  sandbox pay → «Заказ оплачен».
- В шапке storefront — «Войти»/«Выйти»; access/refresh только в httpOnly cookie.
- Auth-service принимает redirect URI для storefront на `:3000` и `:3100` (dev/E2E).

### Phase 3 — A15 perf smoke (✅ локально)

- `scripts/perf-smoke.sh` измеряет p50/p95 через gateway для каталога, растений и зон доставки.
- Локальный прогон (10 запросов на путь): products p95 191 ms, plants p95 28 ms, zones p95 68 ms
  при бюджете 500 ms; скрипт возвращает non-zero при превышении.
- На staging метрики нужно снимать под нагрузкой (k6/Playwright) и сравнивать с A15; это
  следующий шаг после появления staging-окружения.

### Runbooks

- `docs/runbooks/payment-sandbox.md` — сбой WireMock/провайдера.
- `docs/runbooks/outbox-dead-letter.md` — dead-letter и redrive.
- `docs/runbooks/media-storage.md` — 404/500 медиа, volume и диск.
- `docs/runbooks/gateway-5xx.md` — 5xx из-за Docker DNS и 429.

## Тесты и покрытие

| Модуль            | Тесты | Line coverage |
| ----------------- | ----: | ------------: |
| `platform-common` |     9 |           74% |
| `auth-service`    |     3 |           89% |
| `catalog-service` |     8 |           84% |
| `order-service`   |     7 |           82% |
| `payment-service` |     1 |           78% |
| `api-gateway`     |    10 |           97% |
| `tests:e2e`       |    18 |             — |

Итого **38 backend-тестов + 18 E2E = 56 тестов, 0 падений**. JaCoCo-порог 70% проходит.

## Следующие 3 шага

1. Admin BFF (httpOnly cookies) + Idempotency-Key для checkout/pay — закрыть pre-prod блокеры.
2. Storefront: слоты доставки, отзывы, consent-версионирование; slug-URL и sitemap.
3. Staging A1/A15, MinIO/S3, юридический A11 L1–L8; до подписи prod запрещён.

## Блокеры и риски

- Remote workflow использует `actions/*@v4`; обновление до `@v7`/`setup-java@v6` требует `gh auth refresh -s workflow`. CI на `@v4` зелёный.
- A11 T2: double-submit CSRF реализован; `__Host-`/Secure cookie включаются prod-конфигом, проверить в staging.
- In-memory SAS authorizations теряются при рестарте auth-service; для одного экземпляра Phase 1 допустимо (ADR-0006).
- In-memory rate limit не разделяется между экземплярами gateway (ADR-0005).
- Прямые порты auth/catalog открыты на localhost (OAuth и media); order/payment больше не
  публикуются. Перед prod закрыть auth/catalog за reverse proxy.
- MailHog `v1.0.1` — только `linux/amd64`.
- A11 L1–L8 (торговый реестр, СКНО/ЕРИП, ПДн, CITES, оферта, налоги) не закрыты: нужен
  владелец/юрист. Phase 2/3 закрыты по коду, но это не разрешение на prod.
- Уведомления restock/claims best-effort: нет retry/dead-letter; при недоступном SMTP письмо
  теряется (статус подписки/претензии при этом сохраняется). Order-уведомления ретраятся (ADR-0012).
- Outbox at-least-once: возможны дубли писем; dead-letter разбирается вручную через admin
  redrive, автоматического redrive нет.
- CI E2E использует `docker-compose.test.yml`: rate limit 5000/200 и опубликованный auth-service
  `:8090` для PKCE-логина; prod-конфиг не меняется.
- Storefront checkout: адрес есть; слоты доставки и отзывы — следующая итерация. Refresh в BFF
  реализован только в checkout, history-страницы при истёкшем токене просят войти снова.
- **Pre-prod блокеры из ревью:** admin JWT в localStorage (нужен admin BFF), Idempotency-Key для
  create/pay, J6/B2B (Phase 4), slug-URL, tenant_id, revocation refresh при logout.
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
- **Ruling:** checkout — BFF в Next.js с OAuth2 PKCE и httpOnly cookies; корзина на несколько
  позиций, refresh в BFF и история заказов реализованы (ADR-0013).
- **Ruling:** аналитика пишется в `dataLayer` только после consent `all`; необходимые cookie
  работают без согласия (ADR-0014).
- **Ruling:** заказ с зоной доставки требует `deliveryAddress`; адрес хранится в `orders` и
  показывается покупателю в деталях заказа.
- **Ruling:** все мутации каталога/stock/shipping/media требуют staff-роль на gateway; reservation
  API не публикуется наружу.
- **Ruling:** admin JWT в localStorage — известный pre-prod блокер; до admin BFF A11 T1/T2 не
  считать полностью закрытыми.
