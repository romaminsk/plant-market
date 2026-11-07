# Мутационное тестирование (PIT)

## Что это

Мутационное тестирование проверяет **качество тестов**, а не только покрытие строк. PIT берёт
production-классы, вносит в байткод небольшие дефекты (мутанты): меняет `+` на `-`, убирает
проверку, инвертирует условие, подменяет возвращаемое значение. Тесты должны «убить» мутанта —
упасть. Если тест проходит на сломанном коде, мутант **выжил**, значит проверка слабая.

- **Mutation score** = killed / generated. 100% — все искусственные дефекты пойманы.
- **Test strength** = killed / (killed + survived) — качество тестов на покрытом коде.
- Line coverage показывает, что код _исполнялся_; mutation score — что тесты _проверяют результат_.

## Как запустить

```bash
./scripts/mutation-test.sh
# или
./gradlew mutationTest
```

PIT 1.30.2, Java 25. Задача `mutationTest` в корне делегирует в `:platform-common:mutationTest`.
Отчёт: `platform-common/build/reports/pitest/index.html` (HTML) и `mutations.xml`.

Цель: `platform-common` — `UuidV7`, `PageResponse`, `ApiException`, `FieldViolation`.

## Результат

| Метрика                         | Значение    |
| ------------------------------- | ----------- |
| Сгенерировано мутантов          | 20          |
| Убито                           | 20          |
| Выжило                          | 0           |
| **Mutation score**              | **100%**    |
| Line coverage (mutated classes) | 97% (29/30) |
| Test strength                   | 100%        |

Первый прогон дал 95%: выжил мутант `PageResponse.of` с удалённой проверкой `size == 0`.
Добавлен тест `zeroSizeWithItemsStillHasZeroPages` (size=0, totalElements=5), который отличает
ветку `0` от `ceil(total/0)`. После фикса — 20/20.

## Почему JUnit 4 bridge

Основной suite использует JUnit 6. PIT-плагин `pitest-junit5-plugin` 1.2.3 не исполняет JUnit 6
тесты (PIT видит тесты, но не связывает их с мутациями: 0 killed). Для mutation-прогона добавлены
небольшие JUnit 4 тесты `*MutationTest`; они не входят в обычный JUnit 6 suite и запускаются
только PIT. Когда PIT получит поддержку JUnit 6 (или проект перейдёт на JUnit 5), bridge можно
удалить.

## Ограничения и следующие шаги

- Сейчас мутационно покрыт только `platform-common`. Сервисные модули используют Spring и
  Testcontainers; для них нужен Arcmutate Spring plugin либо выделенные быстрые unit-тесты.
- PIT не включён в GitHub Actions: workflow-файлы в этой сессии недоступны для записи токеном
  (нет `workflow` scope). Локальная задача и отчёт готовы; в CI нужно добавить шаг
  `./gradlew mutationTest`.
- Порог можно зафиксировать: `--mutationThreshold 90` в задаче, после стабилизации сервисных
  мутационных прогонов.
