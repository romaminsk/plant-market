# ADR-0012: Outbox retry и dead-letter в Phase 3

- **Статус:** Accepted
- **Дата:** 2026-11-06
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** ADR-0008, ADR-0009, Phase 3

## Контекст

В Phase 2 outbox помечал событие опубликованным сразу после синхронной публикации Spring event,
а `OrderNotificationListener` глотал `MailException`. При недоступном SMTP письмо терялось
без повторов. Для Phase 3 нужен предсказуемый retry.

## Решение

- `outbox_events` получает `attempts`, `last_error`, `next_attempt_at`, `dead`.
- `OutboxPublisher` выбирает события, у которых `published_at is null`, `dead = false` и
  `next_attempt_at is null or <= now`, порциями до 50.
- При ошибке обработки событие получает `attempts += 1`, `last_error` (до 500 символов) и
  `next_attempt_at = now + min(3600, 30 * 2^attempts)` секунд. После 5 попыток — `dead = true`
  и `published_at = now`, чтобы событие больше не выбиралось.
- `OrderNotificationListener` больше не перехватывает `MailException`: ошибка доходит до
  publisher, который делает retry. Остальные события (DRAFT/PENDING_PAYMENT) не отправляют письма
  и публикуются как раньше.
- Claim-уведомления остаются `@TransactionalEventListener(AFTER_COMMIT)` best-effort: претензия
  не должна откатываться из-за SMTP.

## Последствия

- Гарантия at-least-once: если письмо ушло, а транзакция откатилась, возможен дубль. Для писем
  это приемлемо; для денежных операций нужен идемпотентный consumer.
- Dead-letter — флаг в таблице, без отдельной очереди и алерта. Разбор — admin endpoint
  `GET /api/v1/admin/outbox/dead` + `POST /{id}/redrive`; автоматического redrive нет.
- При нескольких экземплярах order-service два publisher могут взять одно событие; до Phase 4
  это допустимо (один экземпляр), позже нужна блокировка `for update skip locked`.

## Проверка

- `OrderFlowIntegrationTest.outboxRetriesFailedNotificationAndPublishesAfterRecovery` — падение
  SMTP, `attempts=1`, `next_attempt_at` в будущем, затем успешная публикация после восстановления.
- `OrderFlowIntegrationTest.createCheckoutPayAndCancelOrder` — все события помечаются published.
