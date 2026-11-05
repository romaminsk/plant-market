# Plant Market — статус проекта

**Обновлено:** 2026-11-05
**Фаза:** 0 — запуск проекта
**Текущая задача:** Phase 0 gate — CI не запускался (нет git remote)

## Состояние окружения

| Компонент         | Факт                                                                      |
| ----------------- | ------------------------------------------------------------------------- |
| Репозиторий       | `/Users/roma/projects/plant-market`, ветка `main`, remote не настроен     |
| Коммитов          | 15 коммитов сессии + базовый `29830ea`                                    |
| Colima            | запущена: `aarch64`, 4 CPU, 4 GiB RAM, 30 GiB disk, `virtiofs`            |
| Автозапуск Colima | включён: `brew services start colima` → `started`                         |
| Docker            | client 29.9.2 / server 29.5.2, compose 5.6.1                              |
| Java              | 25.0.5.0.3 через `/usr/libexec/java_home -v 25`; в PATH 21.0.6 — ожидаемо |
| Node              | 24.22.2, pnpm 11.24.0; `PNPM_HOME` добавлен в `~/.zshrc`                  |
| Диск              | 29 GiB свободно; аварийный порог — 20 GiB                                 |
| gh CLI            | авторизован как `romaminsk`                                               |

## Сделано

### Task 0.1–0.3 — версии, каркас, инфраструктура (✅)

- ADR-0001 Accepted, ADR-0003 (MinIO → Phase 3), ADR-0004 (JUnit 6 без расширений).
- Gradle multi-module, wrapper 9.8.2, Java 25, Spotless/Checkstyle, pnpm workspace.
- PostgreSQL 18.6 (5 БД), WireMock, MailHog; healthchecks и 5 БД проверены.

### Task 0.4 — три сервиса (✅)

- `platform-common`: UUIDv7, `PageResponse`, RFC 7807, `X-Request-Id`.
- `auth-service`: регистрация, логин, JWT HS256, refresh с ротацией и reuse-детектом, Argon2id, Flyway.
- `catalog-service`: CRUD категорий/продуктов, пагинация/фильтры/сортировка, архивирование, Flyway.
- `api-gateway`: маршруты, JWT-валидация, CORS, rate limit 60/10 rpm (A11), `X-Request-Id`.
- Все сервисы в compose, health = UP; сквозной сценарий register → login → JWT → catalog проверен.

### Task 0.5 — frontend-заглушки (✅)

- `clients/storefront`: Next.js 16.4.5, `/catalog` и `/catalog/[id]`, данные из catalog-service через gateway.
- `clients/admin`: React 19.3.1 + Vite 8.3.6, логин, read-only таблица продуктов, токен в `localStorage` для E2E storageState.
- `pnpm build` зелёный в обоих приложениях; storefront отдаёт реальные данные из БД.

### Task 0.6 — seed и E2E (✅)

- `scripts/seed.sh`: 3 категории, 10 продуктов (5 индивидуальных растений с photo-placeholder), admin и customer; повторный запуск не создаёт дублей.
- `tests/e2e`: Page Objects, конфиг через env, programmatic Playwright, trace + screenshot, storageState для admin.
- J1-smoke: каталог → карточка Monstera Deliciosa → цена 1299 BYN. Admin: логин → таблица → повторный вход через storageState.
- Allure 3.33.0 CLI: отчёт `tests/e2e/build/allure-report`, 2 теста passed.

### Task 0.7 — CI (⚠️ подготовлен, не запускался)

- `.github/workflows/ci.yml`: lint → frontend build → backend unit/integration → docker images → compose → seed → frontends → E2E → Allure artifact.
- `.github/workflows/security.yml`: Trivy fs, `pnpm audit`, dependency-review, TruffleHog.
- `.github/dependabot.yml`: gradle, npm, github-actions, docker.
- Локально проверены все шаги, кроме самого GitHub Actions: **нет remote**, workflow не запускался.

### Task 0.8 — документация (✅)

- Все 8 документов B5 существуют; `docs/compliance.md` обновлён по факту Phase 0.
- `docs/testing.md` описывает Testcontainers на Colima и запуск E2E/Allure.
- ADR-0005 — rate limiting; ADR-0001 дополнен Vite, BouncyCastle, Allure CLI.

## Тесты и покрытие

| Модуль            | Тесты | Line coverage |
| ----------------- | ----: | ------------: |
| `platform-common` |     9 |           74% |
| `auth-service`    |     2 |           84% |
| `catalog-service` |     2 |           86% |
| `api-gateway`     |     8 |           96% |
| `tests:e2e`       |     2 |             — |

Итого **23 теста, 0 падений**. JaCoCo `jacocoTestCoverageVerification` с порогом 70% подключён к `check`; `./gradlew build` зелёный.

## В работе

- Phase 0 gate: запустить CI на GitHub и закрыть оставшиеся пункты C3.

## Следующие 3 шага

1. Создать private GitHub repo `romaminsk/plant-market`, добавить remote и выполнить `git push -u origin main` (нужно решение владельца о публикации).
2. Дождаться CI, исправить падения; проверить артефакт Allure и TruffleHog.
3. После зелёного CI закрыть C3 и перейти к Phase 1 (auth/catalog полностью, admin CRUD, storefront).

## Блокеры и риски

- **CI не запускался**: в репозитории нет remote. Это единственный незакрытый пункт C3, который нельзя выполнить локально.
- Диск 29 GiB; Playwright cache 1.7 GiB. Чистить `docker system prune -f` после сессий; при < 20 GiB — стоп.
- In-memory rate limit не разделяется между экземплярами gateway (ADR-0005).
- Прямые порты auth/catalog открыты на localhost; авторизация мутаций — на gateway. Перед prod закрыть.
- MailHog `v1.0.1` — только `linux/amd64`, работает через эмуляцию.
- A11 T1/T2 (Authorization Code + PKCE, cookie security) не реализованы в Phase 0 — зафиксировано в `docs/compliance.md`.

## Решения сессии

- **Ruling:** Flyway 13.12.1 вместо BOM 12.4.0 — иначе PostgreSQL 18.6 не поддерживается.
- **Ruling:** Argon2id через `Argon2PasswordEncoder` + BouncyCastle 1.84.
- **Ruling:** rate limit Phase 0 — in-memory fixed-window 60/10 rpm (A11), Redis запрещён (ADR-0005).
- **Ruling:** E2E-тесты пропускаются без `E2E_BASE_URL`/`E2E_ADMIN_URL`, чтобы `./gradlew build` не зависел от запущенного compose.
- **Ruling:** GitHub remote не создавался без явного разрешения владельца на публикацию кода.
