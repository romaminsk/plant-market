# ADR-0001: Версии технологического стека

- **Статус:** Accepted
- **Дата:** 2026-11-05
- **Решающие:** владелец проекта, AI-лид
- **Связанные документы:** `MASTER_PROMPT.md` A8, `docs/status.md`, ADR-0004

## Контекст

Проект стартует на macOS Apple Silicon с 8 ГБ RAM, Colima 4 ГБ. Целевой стек задан в A8: Java 25 LTS, Spring Boot 4.x, Spring Cloud под выбранный Boot, Gradle 9.x, PostgreSQL 18, Kafka 4.x KRaft (позже), JUnit 6.x, Playwright Java 1.5x, Testcontainers 2.x, Allure 3.x, Node.js 24 LTS, Next.js 16.x, React 19.

Правило A8: **не писать версию, которая не проверена в текущей сессии**. Все версии ниже проверены 2026-11-05: наличие артефакта в Maven Central / npm registry или манифеста образа в Docker Hub, либо версия приходит из проверенного BOM.

## Решение

### Локальные факты

| Компонент           | Версия                           | Источник                                        | Примечание                                           |
| ------------------- | -------------------------------- | ----------------------------------------------- | ---------------------------------------------------- |
| macOS               | Apple Silicon, 8 CPU / 8 GiB RAM | `uname -m`, `sysctl`                            | ограничение памяти                                   |
| Colima              | 0.10.3                           | `colima status`                                 | 4 CPU, 4 GiB, 30 GiB disk, `virtiofs`                |
| Docker Engine       | client 29.9.2 / server 29.5.2    | `docker version`                                | context `colima`                                     |
| Docker Compose      | 5.6.1                            | `docker compose version`                        |                                                      |
| Java (toolchain)    | 25.0.5.0.3                       | `/usr/libexec/java_home -v 25`, `java -version` | Homebrew, keg-only                                   |
| Java (PATH default) | 21.0.6                           | `java -version`                                 | намеренно не меняем; проект берёт 25 через toolchain |
| Node.js             | 24.22.2                          | `node -v` с `PNPM_HOME`                         | LTS-линия 24                                         |
| pnpm                | 11.24.0                          | `pnpm -v`                                       |                                                      |
| Gradle (глобальный) | 8.13                             | `gradle -v`                                     | не используется; проект берёт wrapper                |
| gh CLI              | 2.102.0                          | `gh --version`                                  | авторизован `romaminsk`                              |

### Целевые версии

| Компонент            | Версия     | Источник проверки                                | Примечание                                                              |
| -------------------- | ---------- | ------------------------------------------------ | ----------------------------------------------------------------------- |
| Java                 | 25.0.5.0.3 | локально                                         | toolchain 25                                                            |
| Gradle (wrapper)     | 9.8.2      | `gradle/wrapper/gradle-wrapper.properties`       | скачан и запущен в этой сессии                                          |
| Spring Boot          | 4.1.2      | Maven Central POM, HTTP 200                      | GA, поддерживает Java 17–27 и Gradle 8.14+/9.x                          |
| Spring Cloud         | 2025.1.4   | Maven Central BOM, HTTP 200                      | совместим с Boot 4.0.x/4.1.x                                            |
| Spring Cloud Gateway | 5.0.4      | BOM 2025.1.4 + артефакт стартера, HTTP 200       | `spring-cloud-starter-gateway-server-webflux`                           |
| PostgreSQL           | 18.6       | `docker manifest inspect postgres:18.6`          | образ официальный                                                       |
| Apache Kafka         | 4.4.0      | `docker manifest inspect apache/kafka:4.4.0`     | Phase 2+, KRaft                                                         |
| JUnit Jupiter        | 6.0.3      | Boot BOM (`junit-jupiter.version`)               | версия BOM, не пинуется вручную                                         |
| Testcontainers       | 2.0.5      | Boot BOM + Maven Central                         | programmatic API, см. ADR-0004                                          |
| Playwright Java      | 1.64.0     | Maven Central, HTTP 200                          | programmatic API, см. ADR-0004                                          |
| Allure               | 3.1.1      | Maven Central (`allure-jupiter`), HTTP 200       | CLI `allure` 3.33.0 (npm)                                               |
| Next.js              | 16.4.5     | npm registry, HTTP 200                           | Task 0.5                                                                |
| React                | 19.3.1     | npm registry, HTTP 200                           | Task 0.5                                                                |
| Vite                 | 8.3.6      | npm registry, HTTP 200                           | admin, Task 0.5                                                         |
| @vitejs/plugin-react | 6.2.1      | npm registry, HTTP 200                           | peer: Vite ^8                                                           |
| TypeScript           | 5.9.3      | npm registry, HTTP 200                           | strict mode                                                             |
| Flyway               | 13.12.1    | Maven Central, HTTP 200                          | Boot BOM даёт 12.4.0 без поддержки PostgreSQL 18; версия переопределена |
| PostgreSQL JDBC      | 42.7.14    | Boot BOM                                         |                                                                         |
| MapStruct            | 1.6.3      | Maven Central, HTTP 200                          | Task 0.4+                                                               |
| Springdoc OpenAPI    | 3.2.0      | Maven Central, HTTP 200                          | линия 3.x под Boot 4                                                    |
| WireMock             | 3.13.2     | Maven Central + `wiremock/wiremock:3.13.2`       | образ проверен                                                          |
| Awaitility           | 4.3.0      | Boot BOM                                         |                                                                         |
| ArchUnit             | 1.5.1      | Maven Central, HTTP 200                          |                                                                         |
| JaCoCo               | 0.8.16     | Maven Central, HTTP 200                          |                                                                         |
| Spotless             | 8.11.0     | Maven Central, HTTP 200                          |                                                                         |
| google-java-format   | 2.0.0      | Maven Central, HTTP 200                          | формат Spotless                                                         |
| Checkstyle           | 14.4.0     | Maven Central, HTTP 200                          |                                                                         |
| BouncyCastle         | 1.84       | Maven Central, HTTP 200                          | runtime для Argon2id                                                    |
| MailHog              | v1.0.1     | `docker manifest inspect mailhog/mailhog:v1.0.1` | только локальная разработка                                             |

### Правила использования версий

- Версии JVM-зависимостей берутся из Spring Boot BOM; в `libs.versions.toml` пинуется только то, что BOM не покрывает.
- Spring Cloud BOM импортируется после Boot BOM. Boot 4.1.2 выше версии 4.0.9, которую тянет Cloud 2025.1.4, поэтому Gradle разрешает конфликт в пользу 4.1.2.
- Точные версии без `^` для критичных npm-зависимостей; lock-файл коммитится.
- Версия, отсутствующая в таблице, не попадает в код. Непроверенное помечается `TBD-VERIFIED` и выносится в отчёт.

## Совместимость

- **Java 25 + Spring Boot 4.1.2:** поддерживается (Boot 4.1: Java 17–27).
- **Spring Boot 4.1.2 + Spring Cloud 2025.1.4:** официальная совместимость 2025.1.x с Boot 4.0.x/4.1.x; проверяется зелёной сборкой и стартом gateway в Task 0.4.
- **Gradle 9.8.2 + Spring Boot plugin 4.1.2 + toolchain 25:** проверяется `./gradlew build` в Task 0.2.
- **JUnit 6 + Playwright Java + Allure + Testcontainers:** отклонение от привычных JUnit-расширений, зафиксировано в ADR-0004.
- **Next.js 16.4.5 + React 19.3.1 + Node 24:** проверяется `pnpm build` в Task 0.5.

## Запасные варианты

| Компонент       | Цель     | Запасной                 |
| --------------- | -------- | ------------------------ |
| Java            | 25 LTS   | 21 LTS (уже установлена) |
| Spring Boot     | 4.1.2    | 3.5.x                    |
| Spring Cloud    | 2025.1.4 | release train под 3.5.x  |
| Gradle          | 9.8.2    | 8.14.x                   |
| PostgreSQL      | 18.6     | 16                       |
| Kafka           | 4.4.0    | 3.9 KRaft                |
| JUnit           | 6.0.3    | 5.13.x (весь проект)     |
| Playwright Java | 1.64.0   | последняя стабильная     |
| Testcontainers  | 2.0.5    | 1.2x                     |
| Allure          | 3.1.1    | 2.30.x                   |
| Node.js         | 24 LTS   | 22 LTS                   |
| Next.js         | 16.4.5   | 15.x                     |
| React           | 19.3.1   | 18                       |

## Последствия

- Все версии JVM-зависимостей берутся из Spring Boot BOM; вручную пинуется только то, что BOM не покрывает.
- Версии хранятся в `gradle/libs.versions.toml` и `package.json`.
- При несовместимости тестового стека проект остаётся на JUnit 5.13.x целиком; разные версии JUnit в одном проекте запрещены.
- MinIO в Phase 0 отсутствует по ADR-0003.

## Проверка

```bash
java -version
/usr/libexec/java_home -v 25
node -v
pnpm -v
docker version
docker compose version
colima status
curl -sS -o /dev/null -w '%{http_code}\n' \
  https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom
docker manifest inspect postgres:18.6 >/dev/null
JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew build
```

## Открытые вопросы

- Нет. Версии Kafka, MinIO и Phase 2+ проверяются повторно перед соответствующей фазой.
