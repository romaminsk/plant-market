# ADR-0017: S3-совместимое хранилище на SeaweedFS для staging

- **Статус:** Accepted
- **Дата:** 2026-11-10
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** ADR-0003, ADR-0011

## Контекст

ADR-0003 зафиксировал, что официальные образы MinIO больше не публикуются, и отложил object
storage до Phase 3. Для staging нужен S3-совместимый backend, который можно запустить локально,
проверить и заменить managed-S3 без изменения кода.

## Решение

- Используется **SeaweedFS** (`chrislusf/seaweedfs`) в single-node режиме с S3 gateway на
  `:8333` и named volume.
- `catalog-service` получил `S3MediaStorage` (AWS SDK v2, path-style) при
  `MEDIA_STORAGE=s3`; файловое хранилище остаётся дефолтом для dev.
- Бакет `plant-market-media` создаётся init-контейнером; он **приватный**.
- Публичный `/media/**` обслуживает `catalog-service`, читая объект из S3 и отдавая байты через
  gateway. Это не требует public bucket policy и работает с managed S3.
- S3-ключи и root-пароль SeaweedFS лежат в `.env.staging` (mode 600), потому что образ SeaweedFS
  не читает Docker secrets. Секреты приложений/БД идут через Docker secrets.

## Альтернативы

- MinIO — официальные образы недоступны (ADR-0003).
- LocalStack/S3Mock — dev-эмуляторы, не подходят для staging-хранения.
- Garage — также S3-совместим, но выбран SeaweedFS из-за простого single-node запуска и
  предсказуемого S3 gateway.

## Последствия

- Single-node SeaweedFS без репликации и бэкапа; перед prod нужен managed S3 или backup volume.
- Media отдаётся через catalog-service, а не напрямую из object storage; это добавляет один hop,
  но сохраняет единый `/media/**` контракт и приватный bucket.
- При смене на AWS S3/MinIO достаточно поменять endpoint/ключи; код и URL не меняются.
- Docker secrets для S3-ключей — follow-up (entrypoint-wrapper для SeaweedFS или внешний secret
  manager).

## Проверка

- `S3MediaStorageTest`: put/get, content type, public URL.
- Staging smoke: upload PNG → URL `/media/<key>` → bytes match → object виден в
  `aws s3 ls s3://plant-market-media`.
- Dev E2E остаётся зелёным с `MEDIA_STORAGE=filesystem`.
