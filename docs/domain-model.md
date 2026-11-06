# Доменная модель Plant Market

## Сущности

| Термин              | Что это                                                                                                      |
| ------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Product**         | Позиция каталога: «Monstera deliciosa Albo Variegata». Описание, категория, care-гид. Не продаётся напрямую. |
| **ProductVariant**  | Продаваемая конфигурация: размер, горшок, форма. SKU и цена.                                                 |
| **IndividualPlant** | Конкретный живой экземпляр с уникальным кодом и фото. 1 штука = 1 экземпляр.                                 |
| **StockBatch**      | Партия поставки: количество, себестоимость, поставщик, дата, срок карантина.                                 |
| **Reservation**     | Временная блокировка остатка под корзину или заказ. Имеет TTL.                                               |
| **PropagationUnit** | Черенок или детка от маточника: родитель, дата отделения, стадия, прогноз готовности.                        |
| **MortalityRecord** | Списание растения: причина, себестоимость, партия, дата.                                                     |
| **Order**           | Заказ: позиции, суммы, статусы оплаты и доставки.                                                            |
| **Claim**           | Претензия: тип, фото, решение.                                                                               |

## Инварианты

Нарушение — баг, а не фича.

1. `IndividualPlant` нельзя продать дважды. Из `SOLD` обратно в `AVAILABLE` — только через отмену заказа.
2. Сумма зарезервированного никогда не превышает доступный остаток.
3. `Order.total = subtotal + shipping + tax − discounts`. Пересчёт только на сервере.
4. Растение в статусе `QUARANTINE` недоступно к продаже.
5. Зимняя доставка не предлагается, если температура в регионе назначения ниже порога и термоупаковка не выбрана.
6. Деньги — целые в минимальных единицах (`amount_minor` + `currency`). Никаких `float`/`double`.

## State machines

**IndividualPlant:** `ACQUIRED → QUARANTINE → AVAILABLE → RESERVED → SOLD → DELIVERED`.
Ветки: `→ MORTALITY`, `→ RETURNED → AVAILABLE`.

**Reservation:** `ACTIVE → CONFIRMED | RELEASED | EXPIRED`. TTL корзины — 15 минут.

**Order:** `DRAFT → PENDING_PAYMENT → PAID → FULFILLING → SHIPPED → DELIVERED → COMPLETED`.
Ветки: `CANCELLED` (только до `SHIPPED`), `REFUNDED`, `FAILED`.

**Payment:** `CREATED → PENDING → CONFIRMED → CAPTURED`.
Ветки: `FAILED`, `REFUNDED`, `PARTIALLY_REFUNDED`.

**Claim:** `OPENED → UNDER_REVIEW → RESOLVED(REPLACEMENT | REFUND | REJECTED)`.

**Shipment:** `PLANNED → HANDED_TO_CARRIER → IN_TRANSIT → DELIVERED | RETURNED_TO_SENDER | LOST`.

Каждый переход — явная команда сервиса, запись в `*_status_history` и событие.

## Общие правила

- ID — UUIDv7; время — UTC ISO-8601.
- `tenant_id` — во всех бизнес-таблицах с первого дня (AR-7).
- Оптимистичные блокировки — колонка `version`.
- В Phase 2 реализуются `Product`, `ProductVariant`, `Category`, `IndividualPlant`, `StockBatch`, `Reservation`, `Order` и `Payment` (sandbox); `Shipment`, `Claim` — следующие шаги.

## Ссылки

- Конституция: `MASTER_PROMPT.md`, части A4–A5.
- Архитектура: `docs/architecture.md`.
