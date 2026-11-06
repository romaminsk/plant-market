# Runbook: payment sandbox

## Симптомы

- `/api/v1/orders/{id}/pay` возвращает 409 `Payment is not confirmed` или 500.
- `payment-service` не может достучаться до WireMock.
- Заказ остаётся `PENDING_PAYMENT`, резерв истекает через 15 минут.

## Влияние

Оплата в sandbox и подтверждение резерва. Каталог и корзина работают.

## Диагностика

```bash
docker compose ps wiremock payment-service
docker compose logs payment-service | grep -iE "provider|wiremock|error" | tail -50
curl -s http://localhost:8089/__admin/mappings | jq '.mappings | length'
curl -s -X POST http://localhost:8089/__admin/requests/reset
```

Проверить, что mapping `infra/wiremock/mappings/payment-provider.json` загружен и возвращает
`CONFIRMED`.

## Митигация

1. Перезапустить WireMock после изменения mapping:
   `docker compose restart wiremock`.
2. Перезапустить `payment-service` при потере соединения.
3. Повторить `/pay` для заказа в `PENDING_PAYMENT` (резерв ещё активен).
4. Если резерв истёк — создать новый заказ; старый отменить.

## Откат

Sandbox не хранит PAN. Реальный провайдер и refund flow не подключены (ADR-0007); откат
сводится к остановке `payment-service` и переводу checkout в maintenance.

## Постмортем

Причина (mapping, сеть, секрет webhook), сколько заказов зависло, нужен ли retry/идемпотентность
платежей перед prod.
