# Plant Market

Интернет-магазин редких комнатных растений в Беларуси. Продаём конкретный экземпляр (WYSIWYG), а не «фото похожего растения»: фото листьев, корней и дефектов, уникальный код, происхождение и гарантия.

**Текущий статус:** Phase 0 — запуск проекта. Подробности и следующие шаги — в [`docs/status.md`](docs/status.md).

## Стек

Целевые версии фиксируются только после проверки в `docs/adr/0001-technology-versions.md`. Черновой состав:

- Backend: Java 25 LTS, Spring Boot 4.1, Spring Cloud 2025.1, Gradle 9.8.
- Данные: PostgreSQL 18, Flyway; S3-совместимое хранилище — Phase 3 (ADR-0003).
- Frontend: Next.js 16 (storefront), React 19 + Vite (admin), TypeScript strict.
- Тесты: JUnit, Testcontainers, WireMock, Playwright Java, Allure.
- Инфраструктура: Docker Compose, Colima; Kafka/Redis — только по триггерам A14.

## Требования к машине

- macOS на Apple Silicon, Colima с 4 CPU / 4 GiB RAM / 30 GiB disk.
- Java 25: `export JAVA_HOME="$(/usr/libexec/java_home -v 25)"`.
- Node.js 24 LTS и pnpm 11: `export PNPM_HOME="$HOME/Library/pnpm"; export PATH="$PNPM_HOME:$PNPM_HOME/bin:$PATH"`.
- Docker CLI + Compose, `gh` CLI.

## Быстрый старт

> Команды появятся по мере выполнения Phase 0. Актуальный статус — в `docs/status.md`.

```bash
# 1. Инфраструктура (Task 0.3)
cp .env.example .env
./scripts/dev-up.sh

# 2. Сборка backend (Task 0.2–0.4)
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
./gradlew build

# 3. Остановка
./scripts/dev-down.sh
```

## Структура репозитория

```
platform-common/     # ошибки RFC 7807, X-Request-Id, пагинация, тест-утилиты
services/            # api-gateway, auth-service, catalog-service, order-service, payment-service
contracts/           # OpenAPI, AsyncAPI, GraphQL, gRPC
clients/             # storefront (Next.js), admin (React + Vite)
tests/               # e2e, api, db, grpc, kafka, contract
infra/               # postgres, wiremock, mailhog; minio — Phase 3
docs/                # статус, архитектура, домен, API, тесты, compliance, ADR, runbooks
scripts/             # dev-up.sh, dev-down.sh, seed.sh
```

## Документация

- [`docs/status.md`](docs/status.md) — память проекта между сессиями.
- [`docs/architecture.md`](docs/architecture.md) — контекст и границы сервисов.
- [`docs/domain-model.md`](docs/domain-model.md) — сущности, инварианты, state machines.
- [`docs/api-conventions.md`](docs/api-conventions.md) — REST, события, БД.
- [`docs/testing.md`](docs/testing.md) — стратегия тестирования.
- [`docs/compliance.md`](docs/compliance.md) — чек-лист РБ.
- [`docs/runbooks/`](docs/runbooks/) — эксплуатационные сценарии.
- [`MASTER_PROMPT.md`](MASTER_PROMPT.md) — Конституция проекта.

## Конвенции

- Conventional Commits; один законченный шаг — один коммит.
- `main` защищён; слияние только через PR ≤ 400 строк, ≥ 1 review.
- Секреты не коммитятся: `.env` в `.gitignore`, шаблон — `.env.example`.
- Деньги — целые в минимальных единицах; ID — UUIDv7; время — UTC ISO-8601.
