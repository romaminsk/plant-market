# Plant Market — статус проекта

**Обновлено:** 2026-11-05
**Фаза:** 0 — запуск проекта
**Текущая задача:** Phase 0 gate пройден; следующая — Phase 1

## Состояние окружения

| Компонент         | Факт                                                                      |
| ----------------- | ------------------------------------------------------------------------- |
| Репозиторий       | `romaminsk/plant-market` (private), ветка `main`                          |
| Локальный путь    | `/Users/roma/projects/plant-market`                                       |
| Colima            | запущена: `aarch64`, 4 CPU, 4 GiB RAM, 30 GiB disk, `virtiofs`            |
| Автозапуск Colima | включён: `brew services start colima` → `started`                         |
| Docker            | client 29.9.2 / server 29.5.2, compose 5.6.1                              |
| Java              | 25.0.5.0.3 через `/usr/libexec/java_home -v 25`; в PATH 21.0.6 — ожидаемо |
| Node              | 24.22.2, pnpm 11.24.0; `PNPM_HOME` добавлен в `~/.zshrc`                  |
| Диск              | 29 GiB свободно; аварийный порог — 20 GiB                                 |
| gh CLI            | авторизован как `romaminsk`; scope `workflow` отсутствует                 |

## Phase 0 — gate C3

| Критерий                                       | Статус |
| ---------------------------------------------- | ------ |
| Colima, Docker, hello-world                    | ✅     |
| ADR-0001 с проверенными версиями               | ✅     |
| `./gradlew build` зелёный                      | ✅     |
| compose: инфраструктура + 3 сервиса, health UP | ✅     |
| Регистрация → логин → JWT → каталог            | ✅     |
| Storefront показывает данные из БД             | ✅     |
| Seed идемпотентен                              | ✅     |
| Playwright J1-smoke + Allure                   | ✅     |
| CI зелёный, артефакты доступны                 | ✅     |
| 0 секретов (TruffleHog)                        | ✅     |
| Покрытие доменной логики ≥ 70%                 | ✅     |
| Все 8 документов B5                            | ✅     |
| Отчёт B4                                       | ✅     |

## Сделано

### Task 0.1–0.4 — основа (✅)

- ADR-0001 Accepted, ADR-0003 (MinIO → Phase 3), ADR-0004 (JUnit 6 без расширений), ADR-0005 (in-memory rate limit).
- Gradle multi-module, Java 25, Spotless/Checkstyle, JaCoCo с порогом 70%.
- PostgreSQL 18.6 (5 БД), WireMock, MailHog.
- `platform-common`: UUIDv7, `PageResponse`, RFC 7807, `X-Request-Id`.
- `auth-service`: регистрация, логин, JWT HS256, refresh с ротацией/reuse-детектом, Argon2id, Flyway.
- `catalog-service`: CRUD категорий/продуктов, пагинация/фильтры/сортировка, архивирование, Flyway.
- `api-gateway`: маршруты, JWT-валидация, CORS, rate limit 60/10 rpm, `X-Request-Id`.
- Все сервисы в compose, health UP; сквозной сценарий проверен.

### Task 0.5 — frontend-заглушки (✅)

- Storefront: Next.js 16.4.5, `/catalog` и `/catalog/[id]`, реальные данные из БД через gateway.
- Admin: React 19.3.1 + Vite 8.3.6, логин, read-only таблица, `localStorage` для E2E storageState.
- `pnpm build` зелёный в обоих приложениях.

### Task 0.6 — seed и E2E (✅)

- `scripts/seed.sh`: 3 категории, 10 продуктов (5 индивидуальных растений), admin и customer; идемпотентен.
- `tests/e2e`: Page Objects, env-конфиг, programmatic Playwright, trace/screenshot, storageState.
- J1-smoke: каталог → карточка Monstera Deliciosa → цена 1299 BYN; admin: логин → таблица → storageState.
- Allure 3.33.0 CLI: отчёт с 2 passed.

### Task 0.7 — CI (✅)

- GitHub Actions `ci`: lint → frontend build → backend unit/integration → docker images → compose → seed → frontends → E2E → Allure artifact.
- `security`: Trivy fs, `pnpm audit`, dependency-review, TruffleHog.
- Dependabot: gradle, npm, github-actions, docker.
- **CI run 42063576730 — success**, E2E 2/2 passed, Allure artifact 2.6 MB, e2e-artifacts 447 KB.
- **Security run 42063576990 — success** (trivy, audit, secrets).

### Task 0.8 — документация (✅)

- Все 8 документов B5; `docs/compliance.md` обновлён по факту Phase 0.
- `docs/testing.md` описывает Testcontainers на Colima и запуск E2E/Allure.
- ADR-0001 дополнен Vite, BouncyCastle, Allure CLI.

## Тесты и покрытие

| Модуль            | Тесты | Line coverage |
| ----------------- | ----: | ------------: |
| `platform-common` |     9 |           74% |
| `auth-service`    |     2 |           84% |
| `catalog-service` |     2 |           86% |
| `api-gateway`     |     8 |           96% |
| `tests:e2e`       |     2 |             — |

Итого **21 backend-тест + 2 E2E = 23 теста, 0 падений**. `jacocoTestCoverageVerification` с порогом 70% подключён к `check`.

## В работе

- Phase 1: полный auth/catalog, admin CRUD, storefront-фильтры, SEO-база, a11y.

## Следующие 3 шага

1. Phase 1: довести auth (OAuth2 Authorization Code + PKCE, cookie security) и каталог (варианты, индивидуальные растения, партии).
2. Phase 1: admin CRUD и storefront с фильтрами/карточкой/SEO; Lighthouse A11y ≥ 95.
3. Обновить `docs/compliance.md` по T1/T2/T6 и добавить ArchUnit-тесты границ сервисов.

## Блокеры и риски

- Локальные workflow-файлы обновлены до `actions/*@v7`/`setup-java@v6`, но **не запушены**: у gh-токена нет scope `workflow`, GitHub API отдаёт 404 на `.github/workflows/*`. Remote использует `@v4`; CI на них зелёный. Для обновления: `gh auth refresh -s workflow` и push.
- In-memory rate limit не разделяется между экземплярами gateway (ADR-0005).
- Прямые порты auth/catalog открыты на localhost; авторизация мутаций — на gateway. Перед prod закрыть.
- MailHog `v1.0.1` — только `linux/amd64`, работает через эмуляцию.
- A11 T1/T2 (Authorization Code + PKCE, cookie security) не реализованы в Phase 0 — зафиксировано в `docs/compliance.md`.
- Диск 29 GiB; Playwright cache 1.7 GiB; чистить `docker system prune -f` после сессий.

## Решения сессии

- **Ruling:** создан private repo `romaminsk/plant-market` для запуска CI (автономный режим владельца).
- **Ruling:** Flyway 13.12.1 вместо BOM 12.4.0 — иначе PostgreSQL 18.6 не поддерживается.
- **Ruling:** Argon2id через `Argon2PasswordEncoder` + BouncyCastle 1.84.
- **Ruling:** rate limit Phase 0 — in-memory fixed-window 60/10 rpm (A11), Redis запрещён (ADR-0005).
- **Ruling:** E2E-тесты пропускаются без `E2E_BASE_URL`/`E2E_ADMIN_URL`; `tests:e2e:test` всегда перезапускается (`upToDateWhen false`).
