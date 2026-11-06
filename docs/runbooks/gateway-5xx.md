# Runbook: gateway 5xx и 429

## Симптомы

- Gateway отдаёт 500 `Connection refused` после перезапуска одного из сервисов.
- Массовые 429 `Rate limit exceeded` на публичных или auth-эндпоинтах.
- `X-Request-Id` есть, но downstream-сервис не получил запрос.

## Влияние

Все сценарии через `api-gateway`: каталог, auth, checkout. Внутренние вызовы сервис-сервис
(catalog reservation, payment) не затронуты.

## Диагностика

```bash
docker compose ps
docker compose logs api-gateway | grep -iE "Connection refused|500 Server Error|429" | tail -50
docker compose logs <service> | tail -50
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/actuator/health
```

Если в логах `Connection refused: <service>/172.x.x.x:port` — gateway закэшировал старый IP
Docker-контейнера после его пересоздания.

## Митигация

1. Перезапустить gateway, чтобы он перечитал Docker DNS:
   `docker compose restart api-gateway`.
2. Проверить health downstream-сервисов; дождаться `healthy`.
3. При 429 — проверить окно rate limit (60 rpm публично, 10 rpm login/register, 300 rpm с Bearer).
   Для E2E/CI тестовый compose поднимает лимиты (`docker-compose.test.yml`).
4. Не отключать rate limit в prod; при легитимной нагрузке — увеличить лимит через
   `RATE_LIMIT_*` и зафиксировать решение.

## Откат

Перезапуск gateway безопасен (stateless). Rate-limit — in-memory, сбрасывается при рестарте;
это ожидаемо для одного экземпляра (ADR-0005).

## Постмортем

Причина DNS-кэша/лимита, сколько запросов затронуто, нужен ли service discovery с TTL или
распределённый rate limit (Redis) при масштабировании.
