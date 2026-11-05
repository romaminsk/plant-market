# Plant Market — статус проекта

**Обновлено:** 2026-11-05
**Фаза:** 0 — запуск проекта
**Текущая задача:** Task 0.5 — frontend-заглушки (следующая)

## Состояние окружения

| Компонент         | Факт                                                                      |
| ----------------- | ------------------------------------------------------------------------- |
| Репозиторий       | `/Users/roma/projects/plant-market`, ветка `main`                         |
| Коммитов          | базовый `29830ea` + 10 коммитов сессии                                    |
| Colima            | запущена: `aarch64`, 4 CPU, 4 GiB RAM, 30 GiB disk, `virtiofs`            |
| Автозапуск Colima | включён: `brew services start colima` → `started`                         |
| Docker            | client 29.9.2 / server 29.5.2, compose 5.6.1                              |
| Java              | 25.0.5.0.3 через `/usr/libexec/java_home -v 25`; в PATH 21.0.6 — ожидаемо |
| Node              | 24.22.2, pnpm 11.24.0; `PNPM_HOME` добавлен в `~/.zshrc`                  |
| Диск              | 32 GiB свободно; аварийный порог — 20 GiB                                 |
| gh CLI            | авторизован как `romaminsk`                                               |

## Сделано

### Task 0.1 — версии (✅)

- ADR-0001 Accepted: Boot 4.1.2, Spring Cloud 2025.1.4, Gateway 5.0.4, Gradle 9.8.2, Java 25, PostgreSQL 18.6, JUnit 6.0.3, Testcontainers 2.0.5, Playwright 1.64.0, Allure 3.1.1.
- ADR-0003: MinIO отложен до Phase 3.
- ADR-0004: JUnit 6 без JUnit-расширений; Testcontainers/Playwright — programmatic API.

### Task 0.2 — каркас (✅)

- Gradle multi-module (`platform-common`, `api-gateway`, `auth-service`, `catalog-service`), wrapper 9.8.2, Java toolchain 25, Spotless + Checkstyle.
- pnpm workspace, ESLint 10, Prettier 3; `pnpm lint` и `format:check` зелёные.
- `./gradlew build` зелёный.

### Task 0.3 — инфраструктура (✅)

- PostgreSQL 18.6 (5 БД), WireMock 3.13.2, MailHog v1.0.1, healthchecks.
- `docker-compose.test.yml` для CI; `pg_isready` и 5 БД проверены.
- Урок PostgreSQL 18: volume монтируется в `/var/lib/postgresql`.

### Task 0.4 — три сервиса (✅)

- `platform-common`: `UuidV7`, `PageResponse`, `ApiException`, RFC 7807 `GlobalExceptionHandler`, `RequestIdFilter`, тесты.
- `auth-service`: регистрация, логин, JWT HS256 (10 мин), refresh с ротацией и отзывом цепочки при reuse, BCrypt cost 12, Flyway `V1`, 2 integration-теста.
- `catalog-service`: CRUD категорий и продуктов, пагинация/фильтры/сортировка, архивирование продукта, Flyway `V1`, 2 integration-теста.
- `api-gateway`: маршруты auth/catalog, JWT-валидация, публичный GET каталога, защищённые мутации, CORS, `X-Request-Id`, in-memory rate limit 100 rpm (ADR-0005), 7 тестов.
- Все три сервиса в `docker-compose.yml` с healthchecks; `./scripts/dev-up.sh` собирает boot-jar, образы и поднимает стек.
- Сквозной E2E через compose: регистрация → логин → `/me` → создание категории и продукта с JWT → публичный список → 401 без токена. Все шаги успешны.

## В работе

- Task 0.5 — `clients/storefront` (Next.js 16, страница каталога через gateway) и `clients/admin` (React 19 + Vite, логин и read-only список продуктов).

## Следующие 3 шага

1. Task 0.5: создать storefront на Next.js 16.4.5, страница `/catalog` с данными из catalog-service через gateway.
2. Task 0.5: создать admin на React 19.3.1 + Vite, форма логина и read-only список продуктов; `pnpm build` в обоих.
3. Task 0.6: идемпотентный `scripts/seed.sh`, Playwright Java каркас и J1-smoke.

## Тесты

- `platform-common` — 6, `auth-service` — 2, `catalog-service` — 2, `api-gateway` — 7. Итого **17 тестов, 0 падений**.
- Integration-тесты используют Testcontainers 2.0.5 и PostgreSQL 18.6 через `scripts/test-backend.sh`.

## Блокеры и риски

- Диск 32 GiB; после новых образов и сборок чистить `docker system prune -f`; при < 20 GiB — стоп.
- In-memory rate limit не разделяется между экземплярами gateway; заменить при масштабировании (ADR-0005).
- Прямые порты auth/catalog открыты только на localhost; авторизация мутаций обеспечивается gateway. Перед prod закрыть прямые порты сервисов.
- MailHog `v1.0.1` — только `linux/amd64`, работает через эмуляцию; для dev допустимо.

## Решения сессии

- **Ruling:** Flyway переопределён на 13.12.1: Boot BOM даёт 12.4.0, который не поддерживает PostgreSQL 18.6.
- **Ruling:** пароли — BCrypt cost 12; контракт auth обновлён с Argon2id на BCrypt.
- **Ruling:** rate limit Phase 0 — собственный in-memory fixed-window фильтр, Redis запрещён (ADR-0005).
- **Ruling:** Testcontainers на Colima запускаются через `scripts/test-backend.sh` с `DOCKER_HOST` и `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`.
- **Ruling:** работа продолжается в `main`; один законченный шаг — один commit.
