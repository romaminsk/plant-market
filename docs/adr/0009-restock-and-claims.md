# ADR-0009: Restock-подписки и претензии в Phase 2

- **Статус:** Accepted
- **Дата:** 2026-11-06
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** `MASTER_PROMPT.md` A3 (J2, J4), Phase 2; ADR-0008

## Контекст

Для закрытия Phase 2 нужны сценарии J2 (sold out → подписка → уведомление → покупка) и J4
(повреждённое растение → претензия с фото → решение оператора → письмо). Kafka и отдельные
`notification-service`/`claims-service` в Phase 2 не включаются.

## Решение

### J2 — restock-подписки

- Подписки хранятся в `catalog-service` (`pm_catalog.restock_subscriptions`), потому что
  источник правды об остатках — партии и варианты каталога.
- `POST /api/v1/restock-subscriptions` принимает `targetType` (VARIANT | INDIVIDUAL_PLANT),
  `targetId`, `email`. Подписка публичная, идемпотентная по `(target_type, target_id, email)`:
  повтор возвращает 200, новая — 201.
- Подписаться можно только на отсутствующий товар: если у варианта есть доступный остаток или
  экземпляр `AVAILABLE`, возвращается 409.
- Уведомление отправляется при `release` партии, увеличении остатка через `adjust` и переходе
  экземпляра в `AVAILABLE`. Отправка best-effort через SMTP (MailHog в dev/CI), ошибка SMTP
  логируется и не откатывает изменение остатка; подписка помечается `NOTIFIED`.
- `GET /api/v1/restock-subscriptions/{id}` отдаёт статус `PENDING`/`NOTIFIED` — этого достаточно
  для E2E без доступа к MailHog API в CI.

### J4 — претензии

- Претензии живут в `order-service` (`pm_orders.order_claims` + `order_claim_photos`), потому что
  привязаны к заказу и его e-mail.
- Клиент открывает претензию только по оплаченному заказу (`PAID/FULFILLING/SHIPPED/DELIVERED/
COMPLETED`), обязательны причина и 1–8 URL фото.
- Решение оператора: `REPLACEMENT`, `REFUND` или `REJECT`; статусы `OPEN → REPLACEMENT|REFUND|
REJECTED`, повторное решение — 409.
- Эндпоинты `/api/v1/claims/**` требуют роль `ADMIN`, `OPERATOR` или `MANAGER`; order-service
  маппит claim `roles` из JWT в `ROLE_*`.
- Письмо о решении отправляется после коммита транзакции (`@TransactionalEventListener(AFTER_COMMIT)`),
  ошибки SMTP логируются.

## Последствия

- Фото принимаются как URL: загрузка файлов и MinIO отложены до Phase 3 (ADR-0003).
- Уведомления best-effort, без retry/dead-letter; для Phase 2 допустимо, усиление — вместе с
  outbox retry.
- Отдельный `claims-service` не создаётся до появления SLA/очередей; текущий модуль можно выделить
  без изменения API.
- Юридические правила возврата живых растений (A11 L2) всё ещё требуют юриста; код не фиксирует
  сроки и основания возврата.

## Проверка

- `CatalogFlowIntegrationTest.restockSubscriptionNotifiesWhenBatchIsReleased`.
- `RestockApiE2ETest.soldOutSubscriptionNotifiesAndAllowsPurchase` (J2).
- `OrderFlowIntegrationTest` — claim open/decision/409 + admin role.
- `OrderApiE2ETest.openClaimAndOperatorDecision` (J4).
- `OrderApiE2ETest.winterZoneRequiresThermalPackaging` (J3).
