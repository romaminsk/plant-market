# ADR-0011: Загрузка фото на локальный volume вместо MinIO

- **Статус:** Accepted
- **Дата:** 2026-11-06
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** ADR-0003 (MinIO отложен), ADR-0009 (претензии), Phase 3

## Контекст

Phase 3 требует загрузки фото для WYSIWYG-галереи и претензий. В Phase 2 MinIO был отложен
(ADR-0003). Публичные образы MinIO в 2025–2026 нестабильны по тегам, а привязка CI к внешнему
registry повышает риск. При этом магазину на старте достаточно одного узла.

## Решение

- В `catalog-service` добавлен интерфейс `MediaStorage` и реализация `FileSystemMediaStorage`,
  которая пишет файлы в `${MEDIA_ROOT}` (в compose — named volume `media-data`).
- `POST /api/v1/media` принимает multipart `file`, только JPEG/PNG/WebP, максимум 10 MB;
  возвращает публичный URL `${MEDIA_PUBLIC_BASE_URL}/<uuid>.<ext>`.
- Файлы отдаются Spring resource handler `/media/**`; в dev — `http://localhost:8081/media`.
- Загрузка идёт через gateway (роль не важна, нужен валидный JWT); прямой порт catalog-service
  перед prod закрывается.
- MinIO/S3/CDN подключаются заменой bean `MediaStorage` без изменения API и фронтенда.

## Обновление 2026-11-10

Для staging добавлен `S3MediaStorage` (AWS SDK v2) и SeaweedFS как S3-совместимый backend;
`MEDIA_STORAGE=filesystem` остаётся дефолтом для dev. Публичный `/media/**` в S3-режиме
обслуживает catalog-service, читая приватный объект, поэтому bucket не нужно открывать
анонимно. Подробности — ADR-0017.

## Последствия

- Нет ресайза, thumbnails, EXIF-очистки и антивирусной проверки; для MVP допустимо.
- Нет репликации и бэкапа volume; перед prod нужен object storage с версионированием.
- Публичное чтение файлов по прямому URL; приватные документы (если появятся) потребуют
  presigned URL.
- Ограничение 10 MB и три формата проверяются в контроллере; MIME определяется по заголовку
  клиента, поэтому перед prod нужен content sniffing.

## Проверка

- `CatalogFlowIntegrationTest.uploadsImageAndServesItFromMediaPath` — загрузка PNG, выдача по
  `/media/**`, отказ для `text/plain`.
- `docs/status.md` фиксирует, что галерея и претензии принимают URL; UI загрузки — следующий шаг.
