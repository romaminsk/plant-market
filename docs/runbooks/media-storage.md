# Runbook: media storage

## Симптомы

- В галерее/претензии изображения отдают 404.
- `POST /api/v1/media` возвращает 500 `Could not store media file`.
- `catalog-service` не может писать в `/app/data/media`.

## Влияние

Загрузка и показ фото WYSIWYG-галереи и претензий. Каталог, заказы и оплата работают.

## Диагностика

```bash
docker compose ps catalog-service
docker compose exec -T catalog-service ls -la /app/data/media | tail
docker compose exec -T catalog-service df -h /app/data
docker volume inspect plant-market_media-data
curl -sI http://localhost:8081/media/<file>.png
```

Проверить, что volume `media-data` смонтирован и не заполнен. В логах искать
`Could not store media file`.

## Митигация

1. Освободить место или увеличить volume.
2. Проверить права: контейнер пишет от root; при смене пользователя — `chown`.
3. Перезапустить `catalog-service`: `docker compose restart catalog-service`.
4. Для потерянных файлов — восстановить из бэкапа volume (сейчас автоматического бэкапа нет).

## Откат

Файлы адресуются по UUID; замена volume на пустой не ломает БД, но старые URL станут 404.
Перед prod нужен S3/MinIO с версионированием (ADR-0011).

## Постмортем

Причина (диск, права, volume), сколько файлов потеряно, нужен ли переход на object storage
и политика бэкапа.
