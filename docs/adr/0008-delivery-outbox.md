# ADR-0008: Delivery zones, outbox и уведомления в Phase 2

- **Статус:** Accepted
- **Дата:** 2026-11-06
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** `MASTER_PROMPT.md` A4/A14, Phase 2; ADR-0005, ADR-0007

## Контекст

Phase 2 требует расчёт доставки, зимнюю термоупаковку, уведомления и outbox. Kafka в Phase 2
не включается; `shipping-service` и `notification-service` остаются модулями до триггеров A14.
Нужно проверить сценарий J3 и не потерять события при сбое.

## Решение

- `shipping_zones` живут в `order-service` (`pm_orders`), API `/api/v1/shipping/zones`.
- `CheckoutRequest` принимает `shippingZoneId` и `thermalPack`; при `winterRequiresThermal=true`
  без термоупаковки checkout возвращает 409. Стоимость зоны и термоупаковки входит в
  `shippingMinor` и `totalMinor`.
- Outbox: `order-service` пишет `outbox_events` в той же транзакции, что и смену статуса.
  `OutboxPublisher` раз в 5 секунд читает неопубликованные события, публикует Spring
  `OrderStatusChangedEvent` и ставит `published_at`.
- Уведомления: `OrderNotificationListener` отправляет письмо через MailHog/SMTP на `PAID` и
  `CANCELLED`; ошибки SMTP логируются и не откатывают заказ.
- Статус для уведомления берётся из payload outbox, а не из текущего состояния заказа — это
  предотвращает дубли писем при обработке нескольких событий одного заказа.

## Последствия

- Kafka не нужна; при появлении нескольких экземпляров order-service outbox требует
  распределённой блокировки или отдельного publisher-сервиса.
- Уведомления best-effort: нет retry/dead-letter; при недоступном SMTP письмо теряется.
- Shipping-правила — простые зоны и фиксированная цена; интеграция с реальным перевозчиком
  и расчёт по адресу — Phase 3+.
- J3 покрыт API-тестом (зимняя зона без термоупаковки → 409), UI checkout появится позже.

## Проверка

- `OrderFlowIntegrationTest.checkoutAppliesShippingZoneAndThermalRule`.
- `OrderApiE2ETest.winterZoneRequiresThermalPackaging`.
- MailHog: после `pay` приходит одно письмо на `customer@plantmarket.local`.
- `OutboxPublisher` помечает события published; integration-тест проверяет `publishedAt`.
