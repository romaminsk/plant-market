# ADR-0015: Slug и code в публичных URL

- **Статус:** Accepted
- **Дата:** 2026-11-10
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** ADR-0002, `docs/status.md`

## Контекст

У продуктов и категорий уже есть уникальные `slug`, у индивидуальных растений — уникальный
человекочитаемый `code`. Но storefront, canonical и sitemap использовали UUID: ссылки были
нечитаемыми, а pre-prod ревью отметило slug-URL как блокер SEO.

## Решение

- `GET /api/v1/products/{id}`, `GET /api/v1/categories/{id}` и
  `GET /api/v1/individual-plants/{id}` принимают UUID **или** slug/code.
- Резолвинг: сначала `UUID.fromString`; если строка не UUID — поиск по уникальному slug/code.
- Storefront генерирует `/catalog/{product.slug}` и `/plants/{plant.code}`; canonical и sitemap
  используют те же значения.
- UUID-URL остаются валидными и не редиректятся: старые ссылки и E2E-навигации не ломаются.
- Записывающие операции (`PUT`/`DELETE`) по-прежнему требуют UUID.

## Последствия

- Нет миграции и backfill: slug/code уже уникальны.
- Если slug совпадёт с UUID-строкой, победит UUID; шаблон slug делает это практически
  невозможным, правило зафиксировано в OpenAPI.
- Смена slug не создаёт редирект со старого адреса — это отдельная задача slug-history.

## Проверка

- `CatalogFlowIntegrationTest`: GET по slug/code возвращает тот же объект, неизвестный slug — 404.
- `J1SmokeTest` / `J1FilterTest`: после клика URL содержит slug/code, а не UUID.
