# Runbook: outbox dead-letter

## Симптомы

- Письма о статусе заказа не приходят, хотя заказ оплачен/отменён.
- В `pm_orders.outbox_events` растут `attempts`, появились строки `dead = true`.
- `order-service` логирует `Could not send order notification` или ошибки SMTP.

## Влияние

Уведомления по заказам (PAID/CANCELLED). Сам заказ и оплата не страдают: outbox не откатывает
бизнес-транзакцию. Письма могут быть потеряны до redrive.

## Диагностика

```bash
docker compose exec -T postgres psql -U plantmarket -d pm_orders -c \
  "select id,event_type,aggregate_id,attempts,last_error,next_attempt_at,dead
   from outbox_events where dead = true order by created_at desc limit 20;"

docker compose logs order-service | grep -iE "smtp|outbox|notification" | tail -50
curl -s http://localhost:8025/api/v2/messages?limit=20 | jq '.items | length'
```

Проверить доступность SMTP: `nc -vz localhost 1025` (MailHog) или корпоративный relay.

## Митигация

1. Восстановить SMTP/MailHog.
2. Посмотреть dead-события: `GET /api/v1/admin/outbox/dead` с admin JWT.
3. Перезапустить доставку: `POST /api/v1/admin/outbox/{id}/redrive`.
4. Убедиться, что `attempts=0`, `dead=false`, `published_at=null`; publisher подхватит событие
   в течение 5 секунд.
5. Проверить письмо в MailHog/ящике.

## Откат

Redrive идемпотентен по смыслу, но письмо может уйти повторно (at-least-once). Если нужен
полный стоп — остановить `order-service`; outbox сохранится.

## Постмортем

Причина SMTP-сбоя, сколько событий попало в dead, был ли redrive, нужен ли автоматический
алерт на `dead = true` (сейчас его нет).
