# Архитектура Plant Market

**Статус:** Phase 2. Реализованы `api-gateway`, `auth-service`, `catalog-service`, `order-service`; `payment-service` — следующий шаг.

## Контекст

```
storefront (Next.js) ─┐
                      ├─→ api-gateway :8080 ─→ auth-service :8090  → pm_auth
admin (React + Vite) ─┘                     ─→ catalog-service :8081 → pm_catalog
                                             ─→ order-service :8083   → pm_orders   (Phase 2)
                                             ─→ payment-service :8084 → pm_payments (Phase 2)
```

Внешний трафик идёт только через `api-gateway`. У каждого сервиса своя БД; общих таблиц и запросов к чужой БД нет (AR-1).

## Сервисы MVP

| Сервис            | Порт | БД            | API                                          | Фаза |
| ----------------- | ---: | ------------- | -------------------------------------------- | ---- |
| `api-gateway`     | 8080 | —             | HTTP proxy, CORS, rate limit, `X-Request-Id` | 0    |
| `auth-service`    | 8090 | `pm_auth`     | REST, OIDC                                   | 0    |
| `catalog-service` | 8081 | `pm_catalog`  | REST                                         | 0    |
| `order-service`   | 8083 | `pm_orders`   | REST                                         | 2    |
| `payment-service` | 8084 | `pm_payments` | REST + webhooks                              | 2    |

Позже по триггерам A14: `inventory-service`, `shipping-service`, `notification-service`, `customer-service`, `media-service`, `bff`. До триггеров они остаются модулями внутри существующих сервисов.

## Правила взаимодействия

- Синхронно (REST/gRPC) — только когда нужен немедленный ответ: резерв остатка, расчёт цены и доставки (AR-3).
- Асинхронно — всё остальное: Kafka + transactional outbox + idempotency keys (AR-4). В Phase 0 Kafka не поднимается: работаем на Spring events + outbox-таблице.
- Checkout — сага, оркестратор в `order-service`, с компенсирующими действиями (AR-5).
- GraphQL — только в BFF для storefront/admin, не внутри сервисов (AR-6).
- Распределённых транзакций, 2PC и XA нет (AR-8).

## Потоки Phase 0–2

1. **Логин:** storefront/admin → gateway `/api/v1/auth/**` → auth-service → JWT + refresh.
2. **Каталог:** storefront → gateway `/api/v1/catalog/**` → catalog-service → `pm_catalog`.
3. **Защищённый запрос:** gateway проверяет JWT, прокидывает `X-Request-Id` и данные пользователя.
4. **Checkout:** order-service создаёт DRAFT, вызывает catalog reservation (TTL 15 минут), переводит заказ в PENDING_PAYMENT; pay подтверждает резерв, cancel освобождает его.

## Ограничения

- 8 ГБ RAM хоста, 4 ГБ у Colima. JVM-сервисы: `-XX:InitialHeapSize=256m -XX:MaxHeapSize=384m`.
- Kafka, Redis, gRPC, GraphQL в Phase 0 не добавляются (C4).
- Секреты — только через переменные окружения; `.env` не коммитится.

## Ссылки

- Конституция: `MASTER_PROMPT.md`, части A6–A7, A14–A16.
- Доменная модель: `docs/domain-model.md`.
- API-конвенции: `docs/api-conventions.md`.
