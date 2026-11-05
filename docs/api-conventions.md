# API-конвенции

## REST

- База: `/api/v1`; ресурсы — множественное число, kebab-case.
- Пагинация: `?page=0&size=20&sort=field,asc`; ответ:
  `{items, page, size, totalElements, totalPages}`.
- Ошибки — RFC 7807, `application/problem+json`:
  `type, title, status, detail, instance, traceId, errors[]`.
- Все `POST`, создающие деньги или заказы, требуют `Idempotency-Key`.
- `X-Request-Id` прокидывается сквозь все сервисы и попадает в логи.
- Публичные API возвращают `Optional` вместо `null` там, где это возможно.

Пример ошибки:

```json
{
  "type": "https://plant-market.by/problems/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/auth/register",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "errors": [{ "field": "email", "message": "must be a well-formed email address" }]
}
```

## События

- Топик: `pm.<domain>.<action>.v<N>`; ключ партиции — `aggregateId`.
- Конверт: `eventId, eventType, eventVersion, occurredAt, tenantId, aggregateType, aggregateId, traceId, payload`.
- Публикация — только через outbox-таблицу.
- Потребитель идемпотентен по `eventId`.
- Retry 3× с экспоненциальной задержкой → DLQ `pm.<domain>.<action>.v<N>.dlq`.
- В Phase 0 Kafka не используется; контракты событий готовятся заранее.

## База данных

- Таблицы и колонки — `snake_case`; PK — `id UUID`.
- Обязательные колонки: `tenant_id`, `created_at`, `updated_at`, `version` (optimistic lock).
- Soft delete (`deleted_at`) — только `orders`, `payments`, где есть юридическая причина.
- Миграции Flyway: `V<yyyyMMddHHmm>__<описание>.sql`; только вперёд; `DROP` — только с ADR.
- У каждого сервиса своя БД; межсервисных SQL-запросов нет.

## Безопасность API

- OAuth2 Authorization Code + PKCE; access token 10 минут, refresh с ротацией.
- Cookie: `__Host-`, `Secure`, `HttpOnly`, `SameSite=Lax`; CSRF double-submit для cookie-флоу.
- Пароли — Argon2id.
- Роли: `CUSTOMER`, `OPERATOR`, `MANAGER`, `ADMIN`, `SYSTEM`.
- Rate limit на gateway: 60 req/min/IP публично, 10/min для login/register.
- Заголовки: HSTS, CSP, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`.

## Phase 0 endpoints

| Метод | Путь                            | Сервис  |
| ----- | ------------------------------- | ------- |
| POST  | `/api/v1/auth/register`         | auth    |
| POST  | `/api/v1/auth/login`            | auth    |
| POST  | `/api/v1/auth/refresh`          | auth    |
| GET   | `/api/v1/catalog/categories`    | catalog |
| POST  | `/api/v1/catalog/categories`    | catalog |
| GET   | `/api/v1/catalog/products`      | catalog |
| GET   | `/api/v1/catalog/products/{id}` | catalog |
| POST  | `/api/v1/catalog/products`      | catalog |

## Ссылки

- Конституция: `MASTER_PROMPT.md`, части A10–A11.
- Архитектура: `docs/architecture.md`.
