# Plant Market — статус проекта

**Обновлено:** 2026-11-05
**Фаза:** 0 — запуск проекта
**Текущая задача:** Task 0.4 — три сервиса (следующая)

## Состояние окружения

| Компонент         | Факт                                                                      |
| ----------------- | ------------------------------------------------------------------------- |
| Репозиторий       | `/Users/roma/projects/plant-market`, ветка `main`                         |
| Коммитов          | базовый `29830ea` + 5 коммитов сессии (ADR, docs, build, infra, status)            |
| Colima            | запущена: `aarch64`, 4 CPU, 4 GiB RAM, 30 GiB disk, `virtiofs`            |
| Автозапуск Colima | включён: `brew services start colima` → `started`                         |
| Docker            | client 29.9.2 / server 29.5.2, compose 5.6.1                              |
| Java              | 25.0.5.0.3 через `/usr/libexec/java_home -v 25`; в PATH 21.0.6 — ожидаемо |
| Node              | 24.22.2, pnpm 11.24.0; `PNPM_HOME` добавлен в `~/.zshrc`                  |
| Диск              | 34 GiB свободно после pull образов и сборки; аварийный порог — 20 GiB     |
| gh CLI            | авторизован как `romaminsk`                                               |

## Сделано

### Task 0.1 — версии (✅)

- `docs/adr/0001-technology-versions.md` — Accepted: Boot 4.1.2, Spring Cloud 2025.1.4, Gateway 5.0.4, Gradle 9.8.2, Java 25, PostgreSQL 18.6, JUnit 6.0.3, Testcontainers 2.0.5, Playwright 1.64.0, Allure 3.1.1, Next.js 16.4.5, React 19.3.1.
- `docs/adr/0003-object-storage.md` — Accepted, вариант D: MinIO отложен до Phase 3.
- `docs/adr/0004-test-stack.md` — Accepted: JUnit 6 без JUnit-расширений; Testcontainers и Playwright только через programmatic API.
- Все версии проверены наличием артефакта/образа 2026-11-05.

### Task 0.2 — каркас (✅)

- `settings.gradle.kts`, `gradle/libs.versions.toml`, корневой `build.gradle.kts`, wrapper 9.8.2.
- Модули: `platform-common`, `services/api-gateway`, `services/auth-service`, `services/catalog-service`; у каждого — `@SpringBootApplication`.
- Java toolchain 25, Spotless 8.11.0 + google-java-format 2.0.0, Checkstyle 14.4.0, JUnit Platform из Boot BOM.
- Spring Cloud BOM импортируется в `api-gateway`; стартер — `spring-cloud-starter-gateway-server-webflux` (версия 5.0.4 приходит из BOM, не 2025.1.4).
- `pnpm` workspace: ESLint 10, Prettier 3, TypeScript 5.9; `pnpm lint` и `pnpm format:check` зелёные.
- `./gradlew build` → **BUILD SUCCESSFUL** (27 задач), артефакты собраны.

### Task 0.3 — инфраструктура (✅)

- `docker-compose.yml`: PostgreSQL 18.6 (5 БД), WireMock 3.13.2, MailHog v1.0.1, healthchecks, volume.
- `docker-compose.test.yml`: CI-override без host-портов и с tmpfs; `docker compose config` валиден.
- `./scripts/dev-up.sh` → все три контейнера **healthy**.
- `pg_isready` → `accepting connections`; созданы `pm_auth`, `pm_catalog`, `pm_inventory`, `pm_orders`, `pm_payments`; PostgreSQL 18.6.
- WireMock `/__admin/health` → 200; MailHog UI → 200.
- Урок PostgreSQL 18: data volume монтируется в `/var/lib/postgresql`, а не `/var/lib/postgresql/data`.

## В работе

- Task 0.4 — три сервиса: `platform-common` (RFC 7807, X-Request-Id, пагинация), auth (регистрация/логин/JWT/refresh + Flyway), catalog (CRUD категорий/продуктов + Flyway), gateway (маршруты, CORS, rate limit, health).

## Следующие 3 шага

1. Task 0.4: начать с `platform-common` — общие DTO ошибок, фильтр `X-Request-Id`, пагинация; затем auth и catalog, затем маршруты gateway.
2. Task 0.4: Flyway-миграции и integration-тесты на Testcontainers (programmatic API).
3. Task 0.5: каркасы storefront (Next.js) и admin (React + Vite).

## Блокеры и риски

- Диск: после pull образов и сборок проверить `df -h`; чистить `docker system prune -f` и Gradle-кэш при необходимости. При < 20 GiB — стоп.
- MailHog `v1.0.1` — образ только `linux/amd64`, запускается через эмуляцию; для dev допустимо, health подтверждён.
- Spring Cloud 2025.1.4 официально тянет Boot 4.0.9; проект использует Boot 4.1.2. Совместимость подтверждена зелёной сборкой, окончательно — стартом gateway в Task 0.4.
- Java 25 не в PATH по умолчанию: все команды запускать с `JAVA_HOME="$(/usr/libexec/java_home -v 25)"`.

## Решения сессии

- **Ruling:** MinIO исключён из Phase 0 (ADR-0003, вариант D); порты 9100/9101 зарезервированы.
- **Ruling:** тестовый стек — JUnit 6.0.3 + programmatic Testcontainers/Playwright (ADR-0004). JUnit 5-расширения не подключаются.
- **Ruling:** версии Spring Boot 4.1.2 и Spring Cloud 2025.1.4 зафиксированы владельцем; откат — только через ADR.
- **Ruling:** работа продолжается в `main`; один законченный шаг — один commit.
