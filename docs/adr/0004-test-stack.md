# ADR-0004: Тестовый стек на JUnit 6 без JUnit-расширений

- **Статус:** Accepted
- **Дата:** 2026-11-05
- **Решающие:** владелец проекта, AI-лид
- **Связанные документы:** `MASTER_PROMPT.md` A8, ADR-0001, `docs/testing.md`

## Контекст

A8 требует JUnit 6.x, Playwright Java 1.5x, Testcontainers 2.x и Allure 3.x. Spring Boot 4.1.2 управляет JUnit Jupiter **6.0.3** и Testcontainers **2.0.5** через свой BOM.

Проблема в том, что привычные аннотации тестовых библиотек — это JUnit 5-расширения:

- Playwright Java 1.64.0 поставляет `@UsePlaywright` и `PlaywrightExtension`, собранные под JUnit 5; официальной поддержки JUnit 6 у них нет.
- Testcontainers 2.0.5 поставляет `testcontainers-junit-jupiter` с `@Testcontainers`/`@Container`, также ориентированный на JUnit 5.
- Держать в одном проекте JUnit 5 и JUnit 6 нельзя: разные версии JUnit Platform конфликтуют, а A8 запрещает смешивать версии.

Allure 3.1.1 поставляет `allure-jupiter` (артефакт проверен в Maven Central, HTTP 200) и заявляет поддержку JUnit 6; сквозная проверка отчёта — Task 0.6.

## Решение

1. **JUnit Jupiter 6.0.3** — единственная версия JUnit в проекте, приходит из Spring Boot BOM. JUnit 5-артефакты в тестовый runtime не добавляются.
2. **Testcontainers 2.0.5 — только programmatic API.** Контейнер создаётся и стартует явно:
   ```java
   static PostgreSQLContainer postgres;

   @BeforeAll
   static void startDatabase() {
       postgres = new PostgreSQLContainer("postgres:18.6");
       postgres.start();
   }

   @AfterAll
   static void stopDatabase() {
       postgres.stop();
   }
   ```
   Аннотации `@Testcontainers` и `@Container` не используются.
3. **Playwright Java 1.64.0 — только programmatic API.** `Playwright.create()` и `Browser` создаются в `@BeforeAll`, закрываются в `@AfterAll`; `@UsePlaywright` и `PlaywrightExtension` не используются.
4. **Allure 3.1.1** подключается через `allure-jupiter`; если в Task 0.6 адаптер не заработает с JUnit 6, результаты JUnit экспортируются через Allure CLI без адаптера, а вопрос совместимости выносится в отчёт.
5. Тестовые базовые классы инкапсулируют lifecycle, чтобы каждый тест не дублировал `start()`/`stop()`.

## Рассмотренные альтернативы

| Вариант                                     | Почему отклонён                                                                                                                               |
| ------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| Остаться на JUnit 5.13.x всем проектом      | Отход от A8 и от BOM Boot 4.1.2; JUnit 6 — целевая версия. Остаётся запасным вариантом, если JUnit 6 окажется несовместим с Allure/Playwright |
| Использовать JUnit 5-расширения под JUnit 6 | Неподдерживаемая комбинация, риск падения тестов и конфликта Platform                                                                         |
| Разные версии JUnit в разных модулях        | Прямо запрещено A8                                                                                                                            |

## Последствия

- В тестах больше явного кода lifecycle; ошибки старта контейнера видны сразу в `@BeforeAll`.
- Нет автоматического перезапуска контейнера на каждый тест-класс — контейнеры переиспользуются в пределах класса, при необходимости — `@TestInstance(PER_CLASS)`.
- E2E-тесты Playwright не используют JUnit-интеграцию; трассировки и скриншоты сохраняются вручную в `@AfterEach` при падении.
- Если любой компонент тестового стека не заработает с JUnit 6, проект целиком переходит на JUnit 5.13.x (A8), а этот ADR получает статус Superseded.

## Проверка

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew :platform-common:dependencies --configuration testRuntimeClasspath | grep junit
# ожидается только org.junit.jupiter 6.0.3 и junit-platform 6.x
```

Task 0.6 подтверждает: Testcontainers-тест стартует и останавливает PostgreSQL, Playwright открывает браузер, Allure формирует отчёт.
