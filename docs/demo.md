# Demo: Plant Market (10–15 минут)

Демо показывает WYSIWYG-каталог, корзину, OAuth2 PKCE checkout, зимнюю доставку, restock,
претензии, care-напоминания и админку.

Фотографии в демо — реальные фото растений из Unsplash и Wikimedia Commons, скачанные локально
(`clients/storefront/public/images/plants/CREDITS.md`). Это демо-контент; для продакшена нужны
собственные WYSIWYG-фото с соблюдением фотостандарта.

## Подготовка

```bash
./scripts/demo-up.sh
# чистая БД и seed (опционально, удаляет данные):
DEMO_RESET_CONFIRM=yes ./scripts/demo-reset.sh
```

Открыть:

| Что            | URL                                   |
| -------------- | ------------------------------------- |
| Storefront     | http://localhost:3100                 |
| Админка        | http://localhost:5173                 |
| MailHog        | http://localhost:8025                 |
| WireMock       | http://localhost:8089/__admin         |
| Gateway health | http://localhost:8080/actuator/health |

Пароли:

- Customer: `customer@plantmarket.local` / `CustomerPassword123!`
- Admin: `admin@plantmarket.local` / `AdminPassword123!`

## Сцена 1. J1: каталог → корзина → PKCE-логин → оплата

1. Storefront → **Каталог**. Показать sidebar-фильтры: «Только вариегатные» → 6 позиций,
   «Средний свет» → 3, цена до 150 BYN → 1 (IP-001); «Сбросить фильтры» возвращает 11.
2. Открыть карточку растения (например, `IP-001`): галерея 4 ракурсов, код, гарантия 48 ч,
   зимняя доставка, «Что видите — то и получите».
3. **В корзину** → в шапке появился счётчик. Добавить второе растение.
4. Открыть продукт `MON-001` (Monstera Deliciosa) → вариант с остатком → **В корзину**.
5. **Корзина** → 3 позиции → **Перейти к оформлению**.
6. Нажать **Войти** → форма auth-service (OAuth2 Authorization Code + PKCE) → customer.
7. Заполнить адрес, выбрать зону «Минск», проверить итог → **Оплатить**.
8. Ожидаемо: «Заказ оплачен», номер заказа. В MailHog — письмо `PAID`.
9. **Мои заказы** → заказ и timeline `DRAFT → PENDING_PAYMENT → PAID`.

## Сцена 2. J3: зимняя доставка в Гомель

1. Добавить растение в корзину → checkout.
2. Выбрать зону «Гомель (зима)». Появляется обязательная термоупаковка.
3. Без термоупаковки оплата недоступна; с ней сумма увеличивается на стоимость упаковки.
4. Ожидаемо: заказ оплачен, в деталях — адрес и «Термоупаковка включена».

## Сцена 3. J2: sold out → waitlist → уведомление

1. Storefront → открыть `IP-006` (Anthurium Warocqueanum, статус `QUARANTINE`).
2. Форма «Появится снова?» → ввести e-mail → **Сообщить о поступлении**.
3. Админка → **Экземпляры** → `IP-006` → **Опубликовать**.
4. Ожидаемо: в MailHog письмо «снова в наличии»; растение появилось в каталоге.

## Сцена 4. J4: претензия и решение оператора

```bash
./scripts/demo-claim.sh
```

Скрипт создаёт оплаченный заказ и претензию, печатает `Claim` id.

1. Админка → **Претензии** → найти претензию по короткому id.
2. Выбрать «Замена», комментарий → **Решить**.
3. Ожидаемо: статус `REPLACEMENT`; в MailHog письмо о решении.

## Сцена 5. Админка и WYSIWYG-галерея

1. **Экземпляры** → редактировать `IP-001` → блок «Галерея WYSIWYG».
2. Загрузить файл (JPEG/PNG/WebP) или вставить URL → **Сохранить галерею**.
3. Storefront → карточка `IP-001`: новое фото в галерее.

## Сцена 6. Устойчивость

1. MailHog недоступен → order-письма ретраятся outbox (5 попыток, backoff).
2. `GET /api/v1/admin/outbox/dead` (admin JWT) → dead-letter.
3. `POST /api/v1/admin/outbox/{id}/redrive` → повторная доставка.
4. Cookie-баннер: аналитика пишется в `window.dataLayer` только после «Принять аналитику».

## Остановка

```bash
./scripts/demo-down.sh                 # только frontends
DEMO_DOWN_INFRA=1 ./scripts/demo-down.sh  # + docker compose
```

## Если что-то пошло не так

- Storefront log: `/tmp/plant-market-storefront.log`
- Admin log: `/tmp/plant-market-admin.log`
- Gateway после перезапуска сервиса: `docker compose restart api-gateway` (Docker DNS)
- Пустая БД/нет зон: `DEMO_RESET_CONFIRM=yes ./scripts/demo-reset.sh`
- Нет остатков: `./scripts/seed.sh` (top-up варианта MON-001)
