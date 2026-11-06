# Plant Market

Интернет-магазин редких комнатных растений в Беларуси. Продаём конкретный экземпляр (WYSIWYG), а не «фото похожего растения»: фото листьев, корней и дефектов, уникальный код, происхождение и гарантия.

**Текущий статус:** Phase 3 — каталог, WYSIWYG-галерея, корзина, checkout (OAuth2 PKCE),
оплата в sandbox, доставка, претензии, care-напоминания и outbox retry. Подробности и следующие
шаги — в [`docs/status.md`](docs/status.md).

## Стек

Целевые версии фиксируются только после проверки в `docs/adr/0001-technology-versions.md`. Черновой состав:

- Backend: Java 25 LTS, Spring Boot 4.1, Spring Cloud 2025.1, Gradle 9.8.
- Auth: OAuth2 Authorization Code + PKCE, Argon2id, JWT/refresh rotation.
- Данные: PostgreSQL 18, Flyway; медиа — локальный volume за `MediaStorage` (ADR-0011).
- Frontend: Next.js 16 (storefront), React 19 + Vite (admin), TypeScript strict.
- Тесты: JUnit, Testcontainers, WireMock, Playwright Java, Allure.
- Инфраструктура: Docker Compose, Colima; Kafka/Redis — только по триггерам A14.

## Требования к машине

- macOS на Apple Silicon, Colima с 4 CPU / 4 GiB RAM / 30 GiB disk.
- Java 25: `export JAVA_HOME="$(/usr/libexec/java_home -v 25)"`.
- Node.js 24 LTS и pnpm 11: `export PNPM_HOME="$HOME/Library/pnpm"; export PATH="$PNPM_HOME:$PNPM_HOME/bin:$PATH"`.
- Docker CLI + Compose, `gh` CLI.

## Быстрый старт

```bash
# 1. Собрать jar-файлы, поднять инфраструктуру и пять сервисов
cp .env.example .env
./scripts/dev-up.sh

# 2. Проверить health
curl http://localhost:8080/actuator/health

# 3. Наполнить БД seed-данными (идемпотентно)
./scripts/seed.sh

# 4. Прогнать backend-тесты (Testcontainers + Colima)
./scripts/test-backend.sh build

# 5. Запустить storefront (Next.js, :3000) и админку (:5173)
pnpm dev:storefront
pnpm dev:admin

# 6. Проверить доступность каталога
./scripts/lighthouse-a11y.sh http://localhost:3000/catalog

# 7. E2E (нужны запущенные сервисы, storefront :3100 и админка :5173)
./scripts/test-e2e.sh

# 8. Остановить
./scripts/dev-down.sh
```

Первый запуск скачивает Gradle-зависимости и Docker-образы; это занимает несколько минут.
`dev-up.sh` собирает boot-jar и пересобирает образы сервисов при изменении кода.

## Структура репозитория

```
platform-common/     # ошибки RFC 7807, X-Request-Id, пагинация, тест-утилиты
services/            # api-gateway, auth-service, catalog-service, order-service, payment-service
contracts/           # OpenAPI, AsyncAPI, GraphQL, gRPC
clients/             # storefront (Next.js), admin (React + Vite)
tests/               # e2e, api, db, grpc, kafka, contract
infra/               # postgres, wiremock, mailhog
docs/                # статус, архитектура, домен, API, тесты, compliance, ADR, runbooks
scripts/             # dev-up.sh, dev-down.sh, test-backend.sh, seed.sh
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
