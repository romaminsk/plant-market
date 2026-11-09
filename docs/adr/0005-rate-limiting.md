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
- лимит — 60 запросов в минуту на публичные API, 10 запросов в минуту на `login`/`register`
  (A11) и 300 запросов в минуту для запросов с валидным principal; настраивается через
  `RATE_LIMIT_PUBLIC_RPM`, `RATE_LIMIT_AUTH_RPM` и `RATE_LIMIT_AUTHENTICATED_RPM`;
- при превышении — `429 Too Many Requests` в формате RFC 7807 и заголовок `Retry-After`;
- состояние хранится в `ConcurrentHashMap` процесса; просроченные окна удаляются.

## Последствия

- Лимит не разделяется между экземплярами gateway и сбрасывается при рестарте. Для Phase 0 с одним экземпляром это допустимо.
- Перед горизонтальным масштабированием gateway или при включении Redis по A14 фильтр заменяется на `RequestRateLimiter` с Redis.
- Фильтр не пропускает не-API пути (`/actuator/**`) через лимитер.

## Обновление 2026-11-09

Триггер A14 наступил по итогам ревью: добавлен `redis:7-alpine` в compose, а фильтр получил
`RateLimitStore`. При заданном `RATE_LIMIT_REDIS_URL` окно считается через Redis (`INCR` +
`PEXPIRE`), поэтому лимит общий для нескольких gateway; при недоступности Redis запросы
деградируют до локального in-memory окна. Authenticated-bucket теперь определяется по
principal после проверки JWT, а не по наличию заголовка `Authorization`, поэтому поддельный
Bearer не получает лимит 300/мин.

## Проверка

- `RateLimitFilterTest` — третий запрос при публичном лимите 2 получает 429 и `Retry-After`;
  второй запрос на `login` при auth-лимите 1 получает 429; поддельный Bearer не обходит
  публичный лимит.
- Ручная проверка: 61 запрос к `/api/v1/products` с одного IP даёт 429.
