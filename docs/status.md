# Plant Market — статус проекта

**Обновлено:** 2026-11-10
**Фаза:** 3 — функции готовы; все P1-находки ревью закрыты
**Текущая задача:** staging hardening (DNS/ACME, backups, Vault) и storefront-отзывы

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
- `payment-service`: sandbox-провайдер через WireMock, webhook с HMAC-SHA256
  (`X-Provider-Timestamp`/`X-Provider-Event-Id`/`X-Provider-Signature`), окно 300 с и replay-
  защитой по `payment_webhook_events`; статусы CREATED/PENDING/CONFIRMED/FAILED/REFUNDED; `/pay`
  подтверждает резерв только после CONFIRMED.
- **P1 TTL:** `OrderExpiryService` каждые 60 с отменяет `PENDING_PAYMENT` с истёкшим
  `reservationExpiresAt`, освобождает резерв и пишет историю/outbox; `/pay` после истечения
  отменяет заказ и возвращает 409 `reservation-expired` без вызова провайдера.
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
- CI run `42233064039` — success после подъёма dev/CI rate limit: E2E 18/18.
- CI run `42237526777` — success после Dark Premium редизайна: E2E 18/18, Lighthouse a11y ≥ 0.95.
- QA-прогон: созданы `qa.customer/qa.operator/qa.manager`, добавлен `QaUiJourneyE2ETest`
  (storefront journey, branded login error/logout, operator claim). Найдены и исправлены:
  белая строка при hover в админ-таблицах, синие ссылки в админке, подчёркивание в заказах,
  hero показывал отфильтрованный ноль вместо общего остатка.

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
- Батч захватывается `FOR UPDATE SKIP LOCKED` и лизится через `next_attempt_at`; уведомление
  отправляется вне транзакции, поэтому два экземпляра не дублируют письмо, а SMTP-зависание не
  держит БД-транзакцию.
- Ошибка SMTP в `OrderNotificationListener` теперь доходит до publisher: до 5 попыток с
  экспоненциальной задержкой 30 с…1 ч, затем dead-letter.
- Integration-тесты: падение SMTP → `attempts=1` и будущий `next_attempt_at`, затем успешная
  публикация после восстановления; параллельный publisher не отправляет событие дважды.

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
- Выбранное направление — **Dark Premium**: тёмный лес + латунь, крупная антиква Cormorant
  Garamond, локальные woff2-шрифты (без внешнего CDN), hero с фото и градиентом.
- Экран входа auth-service (`/login`) оформлен в том же стиле: двухколоночный layout с фото,
  локальные шрифты, CSRF, сообщения об ошибке и выходе; статика `/fonts/**`, `/images/**`
  открыта без аутентификации.
- Каталог: горизонтальная панель фильтров (вариегатность, свет, цена), 11 доступных
  экземпляров для проверки фильтра, компактный индекс позиций, скрытие индекса при активном
  фильтре. Макеты трёх направлений — `docs/mockups/design-options.html`.
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
| `platform-common` |    19 |           83% |
| `auth-service`    |     4 |           74% |
| `catalog-service` |    14 |           84% |
| `order-service`   |    17 |           83% |
| `payment-service` |    12 |           77% |
| `api-gateway`     |    14 |           85% |
| `tests:e2e`       |   479 |             — |

Итого **80 backend-тестов + 479 E2E = 559 тестов, 0 падений**. JaCoCo-порог 70% проходит.

### UI-матрица (100 кейсов)

- `docs/test-cases.md` — 100 кейсов TC-001…TC-100: catalog, plant, product, cart, checkout,
  auth, legal, consent, SEO, admin, responsive, a11y.
- Полный E2E-прогон фиксирует порядок классов JUnit (`ClassOrderer$ClassName`): каталоговые
  проверки идут на seed-данных, 350-сценарный `UserScenarioE2ETest` — последним.
- Генератор: `scripts/generate-ui-cases.py`; данные: `tests/e2e/src/test/resources/ui-cases.json`;
  runner: `UiMatrixE2ETest` (JUnit 5 `@TestFactory`, Playwright, один контекст, сброс cookies/storage).
- Глубокие сценарии checkout/claims остаются в отдельных тестах; матрица закрывает страницы,
  фильтры, вход, админ-вкладки, мобильный overflow и a11y-смоук.
- Найдено и исправлено в ходе прогона: 404 favicon на storefront и в админке; отсутствие title
  у `/cart` и `/checkout`; hero показывал 0 под фильтром; белая строка hover в админ-таблицах;
  синие ссылки в админке; подчёркивание заголовков заказов.

### 350 пользовательских E2E-сценариев

- `docs/e2e-scenarios.md` — 350 сценариев SC-001…SC-350, которые проходят полный путь
  пользователя: каталог → фильтры → карточка → корзина → checkout → оплата → заказы, а также
  вход, restock, админ-CRUD, партии, галерея, претензии, consent, SEO, mobile и a11y.
- Генератор: `scripts/generate-user-scenarios.py`; данные:
  `tests/e2e/src/test/resources/user-scenarios.json`; раннер: `UserScenarioE2ETest`.
- Каждый сценарий получает изолированный browser context, уникальные данные через API и проверяет
  пользовательский результат в UI; для клиентских сценариев сессия обновляется перед запуском.
- Дополнительные 150 сценариев (SC-101…SC-250) покрывают комбинации фильтров, все seeded-экземпляры,
  гостевой checkout, регистрацию, админ-CRUD/партии/галерею/претензии, consent, SEO, mobile и a11y.
- Ещё 100 сценариев (SC-251…SC-350) — сквозные journey: покупка (guest/auth, plant/variant, зоны,
  два товара), полный админ-цикл, вход/регистрация, restock/claims/gallery, consent/SEO/mobile/a11y.
- Найдено и исправлено: разный формат цены на SSR и в Chromium (`120,00 Br` vs `BYN 120.00`);
  admin-списки обрезались на 100 продуктах/экземплярах; истёкший admin-токен в длинном прогоне;
  `SeedData` не находил продукт за пределами первой страницы. `formatPrice` сделан
  детерминированным с разрядами, admin-списки — с пагинацией, токены — с refresh.

### Аудит вёрстки форм и таблиц

- `LayoutAuditE2ETest` проверяет все формы и таблицы на 1280 и 390 px: пересечения
  `input/select/textarea/button`, выход контролов за viewport, горизонтальный overflow страницы и
  таблицы без скролла. Скриншоты — `tests/e2e/build/e2e-artifacts/layout/`.
- Покрыты storefront (каталог, корзина, checkout, карточки, вход, регистрация, legal) и админка
  (вход, продукты, категории, экземпляры, галерея, варианты, партии, претензии).
- Найдено и исправлено: в админ-формах поля вылезали из grid-ячеек и накладывались друг на друга
  на 390 px — добавлены `width:100%; min-width:0; box-sizing:border-box`, `.form-row` переведён на
  `minmax(min(100%, 160px), 1fr)`, чекбоксы исключены из правила ширины. Цены в корзине при
  нескольких товарах не выстраивались в колонку — список переведён на grid с правым выравниванием.
- Регрессии: `expectPricesAligned` в пользовательском сценарии и layout-аудит в полном E2E-прогоне.

### Регистрация и гостевой checkout

- В auth-service добавлена брендированная страница `/register` (имя, e-mail, пароль ≥ 6 символов
  и повтор пароля) со ссылкой с экрана входа; после регистрации e-mail подставляется в форму входа.
- Гостевой checkout: страница checkout предлагает e-mail и ссылку «Войти в аккаунт»; BFF вызывает
  публичный `POST /api/v1/orders/guest`, order-service создаёт заказ с `customer_id = NULL`,
  `customer_email` гостя, резервирует и оплачивает его. CSRF-cookie `pm.csrf` выдаётся middleware.
- Добавлен `RegistrationGuestCheckoutE2ETest` (регистрация, повтор пароля, 6 символов, гостевая
  оплата, валидация e-mail).

### Мутационное тестирование

- PIT 1.30.2 (`./scripts/mutation-test.sh`, `./gradlew mutationTest`) для `platform-common`.
- 20 мутантов, 20 убито, **mutation score 100%**, line coverage 97%.
- Подробности, ограничения и следующий шаг для CI — `docs/mutation-testing.md`.

### Техническое руководство и ревью

- `docs/technical-guide.html` — самодостаточная HTML-страница: архитектура, пять сервисов,
  покупка по шагам, данные, безопасность, фронтенд, тесты, инфраструктура, полное ревью
  с находками P0–P3 и словарь простыми словами.

### P0-фиксы безопасности и денег (2026-11-08)

- **P0-1:** catalog/payment закрыты Spring Security; reservation API требует `ROLE_SYSTEM`,
  staff-мутации — staff-роль; catalog/payment больше не публикуют порты на хост; media идёт через
  gateway `/media/**`; order-service выпускает короткоживущий SYSTEM-JWT для внутренних вызовов.
- **P0-2:** `Idempotency-Key` для `POST /orders` и `/orders/guest`; повтор возвращает тот же заказ;
  `payments.order_id` уникален, повторный `POST /payments` возвращает существующий платёж; при
  сбое `catalog.confirm` order-service делает refund и release, а не оставляет оплату без товара.
- **P0-3:** `JWT_SECRET`/`PAYMENT_WEBHOOK_SECRET`/`POSTGRES_PASSWORD` обязательны; `SecretPolicy`
  не даёт стартовать с dev-секретом при `APP_ENV=prod`; dev-значения остаются только для demo.
- **P0-4:** admin-сессия в httpOnly cookie, gateway cookie→bearer, refresh/logout, CSRF-барьер
  `X-Admin-Request` для не-GET; localStorage-токен удалён.
- **P0-5:** `safeReturnTo` отклоняет `//`, `\`, encoded-backslash, dot-segment
  (`/..//evil`) и внешние origin; покрыт unit-тестами Node.
- **P0-6:** `@Version` на products/variants/plants/batches + миграция V6; конфликт обновления — 409.
- **P0-7:** `PlantGalleryEditor` ключуется по `plant.id`; переключение растения больше не тащит
  фото предыдущего.
- Проверено: backend integration-тесты, admin login/gallery E2E, guest checkout с SYSTEM-токенами,
  повтор guest checkout с тем же `Idempotency-Key`, media upload/download через gateway.

### Постревью и техдолг (2026-11-10)

- **Open redirect:** `safeReturnTo` теперь отклоняет dot-segment вариант `/..//evil.example`,
  который URL-парсер нормализовал в protocol-relative путь.
- **Идемпотентность:** ключ заказа проверяется вместе с владельцем (customerId/email); чужой
  ключ даёт 409, а не чужой заказ. BFF выводит стабильный ключ из корзины/доставки/сессии,
  поэтому double-click и retry не создают дубли.
- **Индивидуальные растения:** API отклоняет quantity > 1, BFF всегда отправляет 1.
- **Rate limit за прокси:** при `RATE_LIMIT_TRUST_FORWARDED=true` ключ берётся из первого
  `X-Forwarded-For`; staging включает это, иначе все клиенты делили bucket Caddy.
- **Media:** S3-ключи принимаются только в формате UUID + png/jpg/webp; неизвестный ключ — 404.
- **Storefront:** неизвестный slug/code отдаёт 404, а не 500; зимняя зона автоматически включает
  термоупаковку.
- **Staging:** SeaweedFS/aws-cli запинены digest'ами; из init убраны неиспользуемые MinIO-переменные.

### P1-хардening — второй и третий батчи (2026-11-09)

- **Outbox:** `SKIP LOCKED` + lease, уведомления вне транзакции; тест на параллельную публикацию.
- **Таймауты:** connect/read для catalog/payment/provider HTTP-клиентов и SMTP
  connection/read/write; настройки `HTTP_*`, `MAIL_SMTP_*`.
- **Гостевые претензии и care:** null-safe ownership check, 404 вместо 500.
- **Disabled user:** `refresh` проверяет статус и отзывает все активные токены.
- **Архивные продукты:** резерв отклоняет не-`PUBLISHED`, карточка отдаёт 404, неактивные
  варианты скрыты.
- **Корзина:** количество клампится до 10, позиция не исчезает.
- **Каталог/sitemap:** серверная пагинация, `collectPages` обходит все страницы для sitemap.
- **OAuth2 SAS:** авторизации и consent хранятся в PostgreSQL (`oauth2_authorization`), рестарт
  auth-service не разлогинивает клиентов; тест проверяет строку в БД и refresh-ротацию.
- **Картинки товаров:** `products.image_url` в API и админке; storefront использует URL из API,
  SKU-map остался только fallback для старых записей.
- **Rate limit:** fixed window вынесен в Redis (`RATE_LIMIT_REDIS_URL`), лимит общий для нескольких
  gateway; authenticated-bucket определяется по валидному principal, а не по заголовку Bearer.
- **Webhook amount check:** payload обязан содержать `amountMinor` и `currency`; несовпадение с
  платежом даёт 400 до применения перехода и до записи event id.
- **Auth за gateway:** `/oauth2/**`, `/.well-known/**`, `/login`, `/register` и статика идут через
  `:8080` с сохранением Host; auth-service больше не публикует порт. CORS ограничен `/api/**`,
  CSP login-страниц разрешает свои стили/шрифты/картинки.
- **Slug-URL:** каталог отдаёт `/catalog/{slug}`, растения — `/plants/{code}`; canonical и sitemap
  используют те же значения, UUID-URL остаются валидными (ADR-0015).
- **Consent-версионирование:** `pm.consent.meta.v1` хранит версию политики и `acceptedAt`; при
  смене версии баннер показывается снова, аналитика выключена до нового согласия (ADR-0014).
- **Окно доставки:** checkout принимает `deliveryDate`/`deliveryWindow`, дата проверяется по
  min/max дням зоны в UTC; без полей используется ближайшая дата и `DAY` (ADR-0016). Ёмкость
  окон — следующий шаг.
- **Staging:** Caddy TLS (`app/admin/api.<domain>`), Docker secrets для JWT/БД/webhook,
  контейнерные storefront/admin, SeaweedFS S3 для media (ADR-0017); проверены HTTPS, OAuth с
  `__Host-` cookie, загрузка/выдача media. Публичный DNS/ACME, бэкапы и Vault — дальше.

## Следующие 3 шага

1. Storefront: отзывы.
2. Staging hardening: публичный DNS/ACME, бэкапы, Vault/SOPS, нагрузочный A15.
3. Юридический A11 L1–L8; до подписи prod запрещён.

## Блокеры и риски

- Remote workflow использует `actions/*@v4`; обновление до `@v7`/`setup-java@v6` требует `gh auth refresh -s workflow`. CI на `@v4` зелёный.
- A11 T2: double-submit CSRF реализован; `__Host-`/Secure cookie включаются prod-конфигом, проверить в staging.
- SAS authorizations теперь в PostgreSQL и переживают рестарт; in-memory остались только
  зарегистрированные OAuth-клиенты (статическая конфигурация).
- Rate limit общий через Redis; если Redis недоступен, gateway деградирует до локального окна.
- Auth, catalog, payment и order не публикуются на хост: OAuth-редиректы и login идут через
  gateway `:8080`, наружу остаются только gateway, storefront, admin и служебные UI.
- MailHog `v1.0.1` — только `linux/amd64`.
- A11 L1–L8 (торговый реестр, СКНО/ЕРИП, ПДн, CITES, оферта, налоги) не закрыты: нужен
  владелец/юрист. Phase 2/3 закрыты по коду, но это не разрешение на prod.
- Уведомления restock/claims best-effort: нет retry/dead-letter; при недоступном SMTP письмо
  теряется (статус подписки/претензии при этом сохраняется). Order-уведомления ретраятся (ADR-0012).
- Outbox at-least-once: возможны дубли писем; dead-letter разбирается вручную через admin
  redrive, автоматического redrive нет.
- CI E2E копирует `.env.example` (dev-лимиты 2000/100) и использует опубликованный auth-service
  `:8080` для PKCE-логина (auth за gateway); prod-лимиты 60/10/300 задаются окружением.
- `docker-compose.test.yml` остаётся опциональным override для ручного прогона тестового стека.
- Storefront checkout: адрес, дата и окно доставки есть; отзывы — следующая итерация. Refresh в
  BFF реализован только в checkout, history-страницы при истёкшем токене просят войти снова.
- **Pre-prod блокеры из ревью:** J6/B2B (Phase 4), tenant_id, revocation refresh при
  logout. Slug-URL закрыт 2026-11-10 (ADR-0015). P0-блокеры безопасности/денег закрыты
  2026-11-08 (см. ниже).
- Фото принимаются как URL и через загрузку в админке; media отдаётся через gateway
  (`/media/**`), CSP storefront разрешает этот origin.
- Staging media — single-node SeaweedFS без репликации/бэкапа; публичные файлы без content
  sniffing. Dev media — локальный volume (ADR-0011/0017).
- Staging проверен локально на `*.localhost` с внутренним CA; публичный DNS/ACME и firewall
  (base compose публикует Postgres/gateway/WireMock/MailHog на localhost) не проверены.
- Галерея в seed использует placehold.co; фотостандарт (нейтральный фон, линейка, 4 ракурса)
  должен соблюдаться оператором при загрузке реальных фото.
- Диск 29 GiB; чистить `docker system prune -f` после сессий.

## Решения сессии

- **Ruling:** Phase 1 каталог хранит Product как описание, продаваемые сущности — Variant и IndividualPlant.
- **Ruling:** партии и экземпляры проходят явные state transitions; недопустимый переход — 409.
- **Ruling:** J1-фильтр реализован на `/api/v1/individual-plants` (variegated + light + maxPriceMinor).
- **Ruling:** Lighthouse запускается `scripts/lighthouse-a11y.sh` и как `LighthouseA11yTest` в E2E-сьюте; порог 0.95.
- **Ruling:** rate limit: 60 rpm публично, 10 rpm login/register, 300 rpm для запросов с Bearer-токеном;
  dev/CI используют 2000/100/300 из `.env.example`, чтобы SSR-каталог не упирался в лимит.
- **Ruling:** визуальное направление — **Dark Premium** (тёмный лес + латунь, Cormorant Garamond);
  шрифты self-hosted в `clients/storefront/public/fonts/`, без внешнего CDN из-за CSP.
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
- **Ruling:** admin-сессия переведена на httpOnly cookies (`pm.admin.access`/`pm.admin.refresh`),
  gateway превращает cookie в bearer; localStorage больше не используется.
