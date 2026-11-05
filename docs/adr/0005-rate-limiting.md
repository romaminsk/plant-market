# ADR-0005: Rate limiting в Phase 0

- **Статус:** Accepted
- **Дата:** 2026-11-05
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** `MASTER_PROMPT.md` C4, `docs/architecture.md`, ADR-0002

## Контекст

Task 0.4 требует rate limit на `api-gateway`. Штатный `RequestRateLimiter` Spring Cloud Gateway использует Redis, а Redis запрещён в Phase 0 (C4) и появляется только по триггерам A14. Gateway в Phase 0 работает в одном экземпляре.

## Решение

В `api-gateway` реализован собственный `GlobalFilter` с fixed-window счётчиком:

- окно — 60 секунд;
- ключ — IP клиента + первый сегмент маршрута (`auth`, `categories`, `products`);
- лимит — 100 запросов в минуту на ключ, настраивается через `RATE_LIMIT_RPM`;
- при превышении — `429 Too Many Requests` в формате RFC 7807 и заголовок `Retry-After`;
- состояние хранится в `ConcurrentHashMap` процесса; просроченные окна удаляются.

## Последствия

- Лимит не разделяется между экземплярами gateway и сбрасывается при рестарте. Для Phase 0 с одним экземпляром это допустимо.
- Перед горизонтальным масштабированием gateway или при включении Redis по A14 фильтр заменяется на `RequestRateLimiter` с Redis.
- Фильтр не пропускает не-API пути (`/actuator/**`) через лимитер.

## Проверка

- `RateLimitFilterTest` — третий запрос при лимите 2 получает 429 и `Retry-After`.
- Ручная проверка: 101 запрос к `/api/v1/products` с одного IP даёт 429.
