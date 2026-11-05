# Стратегия тестирования

## Уровни

| Уровень       | Инструменты                                                                | Норма                                                          |
| ------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------- |
| Unit          | JUnit, Mockito                                                             | ≥ 70% строк; ≥ 80% для денег, остатков, статусов               |
| Slice         | Spring Test, MockMvc/WebTestClient, Testcontainers                         | все контроллеры и repository                                   |
| Integration   | Spring Boot Test + Testcontainers (Postgres; Kafka/Redis по триггерам A14) | каждый сервис стартует и проходит happy path                   |
| Contract      | WireMock, OpenAPI/GraphQL schema diff, protobuf compatibility              | все внешние интеграции                                         |
| E2E           | Playwright Java                                                            | 6 сценариев A3 + smoke                                         |
| Accessibility | axe-core                                                                   | 0 critical на ключевых страницах                               |
| Visual        | Playwright screenshots                                                     | карточка, каталог, checkout                                    |
| Performance   | k6 или Gatling                                                             | каталог p95 < 300 мс, checkout p95 < 800 мс, 50 RPS без ошибок |
| Security      | OWASP ZAP baseline, Trivy, dependency-check                                | 0 high/critical                                                |

## Правила

- Тестовые данные — билдеры и фабрики, не хардкод. Тест сам создаёт и сам убирает данные.
- `Thread.sleep` запрещён. Только явные ожидания Playwright/Awaitility.
- Время — через инжектируемый `Clock`; случайность — через инжектируемый `Random`.
- E2E не зависят от порядка и идут параллельно; изоляция через уникальные email и SKU.
- Платежи и доставка в E2E — только через WireMock.
- Flaky: максимум 1 повторный прогон в CI. Тест, упавший повторно без изменения кода, в тот же день уходит в карантин + issue.
- Архитектурные правила проверяются ArchUnit-тестом: сервис не импортирует пакет другого сервиса и не обращается к чужой БД.

## Локальный запуск тестов

Testcontainers на Colima требует явного Docker-сокета и override для Ryuk:

```bash
./scripts/test-backend.sh build
```

Скрипт выставляет `JAVA_HOME` (Java 25), `DOCKER_HOST` из текущего docker context и
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`. В CI с обычным
`/var/run/docker.sock` достаточно `./gradlew build`.

## Definition of Done

Тест написан до или вместе с кодом → `./gradlew build` зелёный → покрытие не упало → статический анализ без новых предупреждений → токены и i18n соблюдены → a11y проверен для UI → документация и ADR обновлены → миграция обратима или это задокументировано → в diff нет секретов → коммит в Conventional Commits → PR ≤ 400 строк.

## Phase 0

- Unit-тесты доменной логики каталога и auth.
- Slice-тесты всех контроллеров.
- Интеграционные тесты с Testcontainers: сервис стартует, Flyway применяется, happy path проходит.
- Testcontainers и Playwright используются через programmatic API, без JUnit-расширений (ADR-0004).
- E2E J1-smoke: каталог → товар из seed → карточка → цена.
- Allure-отчёт формируется и сохраняется как артефакт.

## Ссылки

- Конституция: `MASTER_PROMPT.md`, часть A13.
- API-конвенции: `docs/api-conventions.md`.
