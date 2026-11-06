# ADR-0007: Платёжный sandbox на WireMock

- **Статус:** Accepted
- **Дата:** 2026-11-06
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** `MASTER_PROMPT.md` A11, Phase 2; ADR-0001

## Контекст

Phase 2 требует платёжный провайдер в sandbox, hosted page и webhook, но реальный провайдер
ещё не выбран, а юридический чек-лист РБ не закрыт. Нужен воспроизводимый контур без хранения
PAN и без реальных списаний.

## Решение

- `payment-service` — отдельный сервис, БД `pm_payments`.
- Провайдерский адаптер `HttpPaymentProviderClient` вызывает `POST /provider/payments`.
- В локальном compose провайдер эмулируется WireMock (`infra/wiremock/mappings/payment-provider.json`),
  ответ: `AUTHORIZED`, `providerRef`, `redirectUrl`.
- `AUTHORIZED`/`CONFIRMED` маппятся в `PaymentStatus.CONFIRMED`; `PENDING`, `FAILED`, `REFUNDED`
  сохраняются отдельно.
- Webhook `POST /api/v1/payments/webhook` требует `X-Provider-Secret`; секрет — env
  `PAYMENT_WEBHOOK_SECRET`.
- `order-service.pay` создаёт платёж через payment-service и подтверждает резерв только при
  `CONFIRMED`; иначе заказ остаётся `PENDING_PAYMENT`.

## Последствия

- PAN и платёжные данные не хранятся; это sandbox-контур, не production-платежи.
- Перед prod нужен реальный адаптер провайдера (hosted page / redirect), проверка подписи webhook
  и идемпотентность по `providerRef`.
- WireMock mapping загружается при старте контейнера; при изменении mapping нужен restart.
- Refund flow и частичные возвраты — следующий шаг.

## Проверка

- `PaymentFlowIntegrationTest`: create → CONFIRMED, webhook REFUNDED, wrong secret 401.
- `OrderApiE2ETest`: order checkout → pay → PAID через payment-service + WireMock.
