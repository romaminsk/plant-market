#!/usr/bin/env python3
"""100 full user scenarios for Plant Market.

Generates:
  tests/e2e/src/test/resources/user-scenarios.json  (executed by UserScenarioE2ETest)
  docs/e2e-scenarios.md                             (human-readable catalog)

Run from the repo root: python3 scripts/generate-user-scenarios.py
"""

from __future__ import annotations

import json
from pathlib import Path

SCENARIOS: list[dict] = []


def sc(
    sid: str,
    area: str,
    name: str,
    expected: str,
    steps: list[dict],
    *,
    auth: str = "none",
    viewport: list[int] | None = None,
) -> None:
    def flatten(items: list) -> list[dict]:
        flat: list[dict] = []
        for item in items:
            if isinstance(item, list):
                flat.extend(flatten(item))
            else:
                flat.append(item)
        return flat

    entry: dict = {
        "id": sid,
        "area": area,
        "name": name,
        "expected": expected,
        "auth": auth,
        "steps": flatten(steps),
    }
    if viewport:
        entry["viewport"] = viewport
    SCENARIOS.append(entry)


def api(operation: str, store: str | None = None, **params) -> dict:
    step: dict = {"action": "api", "operation": operation, "params": params}
    if store:
        step["store"] = store
    return step


def goto(path: str) -> dict:
    return {"action": "goto", "value": path}


def goto_admin(path: str = "/") -> dict:
    return {"action": "gotoAdmin", "value": path}


def click(selector: str) -> dict:
    return {"action": "click", "selector": selector}


def fill(selector: str, value: str) -> dict:
    return {"action": "fill", "selector": selector, "value": value}


def select(selector: str, value: str) -> dict:
    return {"action": "select", "selector": selector, "value": value}


def check(selector: str) -> dict:
    return {"action": "check", "selector": selector}


def press(selector: str, key: str) -> dict:
    return {"action": "press", "selector": selector, "value": key}


def hover(selector: str) -> dict:
    return {"action": "hover", "selector": selector}


def upload(selector: str, path: str) -> dict:
    return {"action": "upload", "selector": selector, "value": path}


def wait_text(text: str) -> dict:
    return {"action": "waitText", "value": text}


def wait_sel(selector: str) -> dict:
    return {"action": "waitSelector", "selector": selector}


def wait_fn(js: str) -> dict:
    return {"action": "waitForFunction", "value": js}


def wait_url(path: str) -> dict:
    return {"action": "waitUrl", "value": path}


def text(*values: str) -> list[dict]:
    return [{"action": "expectText", "value": value} for value in values]


def gone(value: str) -> dict:
    return {"action": "expectTextGone", "value": value}


def count(selector: str, value: int) -> dict:
    return {"action": "expectCount", "selector": selector, "value": value}


def min_count(selector: str, value: int) -> dict:
    return {"action": "expectMinCount", "selector": selector, "value": value}


def visible(*selectors: str) -> list[dict]:
    return [{"action": "expectVisible", "selector": selector} for selector in selectors]


def hidden(selector: str) -> dict:
    return {"action": "expectHidden", "selector": selector}


def disabled(selector: str) -> dict:
    return {"action": "expectDisabled", "selector": selector}


def enabled(selector: str) -> dict:
    return {"action": "expectEnabled", "selector": selector}


def attr(selector: str, name: str, value: str, match: str = "equals") -> dict:
    return {
        "action": "expectAttribute",
        "selector": selector,
        "name": name,
        "value": value,
        "match": match,
    }


def url(value: str) -> dict:
    return {"action": "expectUrl", "value": value}


def title(value: str) -> dict:
    return {"action": "expectTitle", "value": value}


def storage(key: str, value: str) -> dict:
    return {"action": "expectStorage", "key": key, "value": value}


def overflow() -> dict:
    return {"action": "expectNoOverflow"}


def a11y() -> dict:
    return {"action": "expectA11y"}


def cart_plant(price: int = 12000) -> dict:
    return {
        "action": "setCart",
        "items": [
            {
                "type": "INDIVIDUAL_PLANT",
                "id": "{plant.id}",
                "name": "{plant.productName} ({plant.code})",
                "priceMinor": price,
                "currency": "BYN",
                "quantity": 1,
            }
        ],
    }


def cart_variant(price: int = 15000, quantity: int = 1) -> dict:
    return {
        "action": "setCart",
        "items": [
            {
                "type": "VARIANT",
                "id": "{variant.id}",
                "name": "{variant.name}",
                "priceMinor": price,
                "currency": "BYN",
                "quantity": quantity,
            }
        ],
    }


def cookies() -> dict:
    return {"action": "acceptCookies"}


def tab(name: str) -> dict:
    return {"action": "adminTab", "value": name}


def plant_fixture(price: int = 12000, sold_out: bool = False, store: str = "plant") -> dict:
    return api(
        "createSoldOutPlant" if sold_out else "createPlant",
        store,
        productSku="PLANT-001",
        code=f"SC-{store}-{{uid}}",
        priceMinor=price,
    )


def variant_fixture(price: int = 15000) -> list[dict]:
    return [
        api(
            "createProduct",
            "product",
            sku="SC-P-{uid}",
            name="SC Product {uid}",
            slug="sc-product-{uid}",
            priceMinor=10000,
        ),
        api(
            "createVariant",
            "variant",
            productId="{product.id}",
            sku="SC-V-{uid}",
            name="12 cm pot",
            priceMinor=price,
        ),
        api("createStock", "batch", variantId="{variant.id}", quantity=5),
    ]


def paid_order(price: int = 12000) -> list[dict]:
    return [
        plant_fixture(price),
        api(
            "createOrder",
            "order",
            items=[{"individualPlantId": "{plant.id}", "quantity": 1}],
        ),
        api("payOrder", "paid", orderId="{order.id}"),
    ]


# =============================================================================
# A. Catalog and filters — SC-001 .. SC-015
# =============================================================================
sc(
    "SC-001",
    "catalog",
    "Покупатель фильтрует каталог по вариегатности",
    "URL содержит variegated=true, минимум 6 карточек, тумблер нажат",
    [
        goto("/catalog"),
        click("button:has-text('Только вариегатные')"),
        wait_fn("() => new URL(location.href).searchParams.get('variegated') === 'true'"),
        min_count(".plant-card", 6),
        attr(".filter-toggle", "aria-pressed", "true"),
    ],
)
sc(
    "SC-002",
    "catalog",
    "Покупатель добавляет фильтр по среднему свету",
    "URL содержит light=MEDIUM, минимум 3 карточки",
    [
        goto("/catalog?variegated=true"),
        click("button:has-text('Средний')"),
        wait_fn("() => new URL(location.href).searchParams.get('light') === 'MEDIUM'"),
        min_count(".plant-card", 3),
        attr("button:has-text('Средний')", "aria-pressed", "true"),
    ],
)
sc(
    "SC-003",
    "catalog",
    "Покупатель ограничивает цену до 150 BYN",
    "URL содержит maxPrice=150, минимум 1 карточка",
    [
        goto("/catalog?variegated=true&light=MEDIUM"),
        fill("#filter-price", "150"),
        click("button:has-text('ОК')"),
        wait_fn("() => new URL(location.href).searchParams.get('maxPrice') === '150'"),
        min_count(".plant-card", 1),
    ],
)
sc(
    "SC-004",
    "catalog",
    "Комбинация фильтров сужает каталог",
    "Найдено и минимум одна карточка",
    [
        goto("/catalog?variegated=true&light=MEDIUM&maxPrice=150"),
        wait_text("Найдено"),
        min_count(".plant-card", 1),
    ],
)
sc(
    "SC-005",
    "catalog",
    "Покупатель сбрасывает все фильтры",
    "URL без query, «Любой» активен, минимум 11 карточек",
    [
        goto("/catalog?variegated=true&light=MEDIUM&maxPrice=150"),
        click("button:has-text('Сбросить фильтры')"),
        wait_fn("() => location.search === ''"),
        attr("button:has-text('Любой')", "aria-pressed", "true"),
        min_count(".plant-card", 11),
    ],
)
sc(
    "SC-006",
    "catalog",
    "Пустой результат подсказывает сбросить фильтры",
    "Текст пустого состояния и ссылка сброса",
    [
        goto("/catalog?maxPrice=1"),
        text("Под эти фильтры растений нет"),
        visible(".empty a[href='/catalog']"),
    ],
)
sc(
    "SC-007",
    "catalog",
    "Покупатель снимает фильтр света кнопкой «Любой»",
    "light удалён из URL, «Любой» активен",
    [
        goto("/catalog?light=MEDIUM"),
        click("button:has-text('Любой')"),
        wait_fn("() => new URL(location.href).searchParams.get('light') === null"),
        attr("button:has-text('Любой')", "aria-pressed", "true"),
    ],
)
sc(
    "SC-008",
    "catalog",
    "Покупатель выключает тумблер вариегатности",
    "variegated удалён, aria-pressed=false",
    [
        goto("/catalog?variegated=true"),
        click(".filter-toggle"),
        wait_fn("() => new URL(location.href).searchParams.get('variegated') === null"),
        attr(".filter-toggle", "aria-pressed", "false"),
    ],
)
sc(
    "SC-009",
    "catalog",
    "Покупатель открывает экземпляр из карточки",
    "Страница /plants/ и бейдж WYSIWYG",
    [
        goto("/catalog"),
        click(".plant-card h2 a"),
        wait_url("/plants/"),
        visible(".wysiwyg-badge"),
    ],
)
sc(
    "SC-010",
    "catalog",
    "Покупатель открывает продукт из индекса",
    "Страница /catalog/ и характеристики продукта",
    [
        goto("/catalog"),
        click(".product-row h2 a"),
        wait_url("/catalog/"),
        text("Свет", "Уход"),
    ],
)
sc(
    "SC-011",
    "catalog",
    "CTA в hero прокручивает к каталогу",
    "hash #catalog и видимая панель фильтров",
    [
        goto("/catalog"),
        click("a.hero-cta"),
        wait_fn("() => location.hash === '#catalog'"),
        visible(".catalog-toolbar"),
    ],
)
sc(
    "SC-012",
    "catalog",
    "Индекс позиций скрывается при активном фильтре",
    "Нет строк product-row",
    [
        goto("/catalog?variegated=true"),
        hidden(".product-row"),
    ],
)
sc(
    "SC-013",
    "catalog",
    "Счётчик результатов обновляется после фильтра",
    "«Найдено» и минимум 6 карточек",
    [
        goto("/catalog?variegated=true"),
        text("Найдено"),
        min_count(".plant-card", 6),
    ],
)
sc(
    "SC-014",
    "catalog",
    "Карточка показывает характеристики и цену",
    "Свет, Уход, Питомцы, цена",
    [
        goto("/catalog"),
        text("Свет", "Уход", "Питомцы", "120,00"),
    ],
)
sc(
    "SC-015",
    "catalog",
    "Пилюли света доступны и «Любой» активен",
    "5 пилюль, aria-pressed у «Любой»",
    [
        goto("/catalog"),
        count(".filter-pill", 5),
        attr("button:has-text('Любой')", "aria-pressed", "true"),
    ],
)

# =============================================================================
# B. Plant detail and gallery — SC-016 .. SC-025
# =============================================================================
sc(
    "SC-016",
    "plant",
    "Покупатель открывает карточку экземпляра",
    "Название, цена и WYSIWYG",
    [
        plant_fixture(),
        goto("/plants/{plant.id}"),
        text("{plant.productName}", "{plant.code}", "WYSIWYG"),
    ],
)
sc(
    "SC-017",
    "plant",
    "Покупатель переключает ракурс галереи",
    "После клика виден ракурс «Лист крупно»",
    [
        api("findPlant", "seeded", code="IP-001"),
        goto("/plants/{seeded.id}"),
        click(".gallery-thumbs li:nth-child(2) .gallery-thumb"),
        wait_text("Лист крупно"),
    ],
)
sc(
    "SC-018",
    "plant",
    "Блок доверия содержит три пункта",
    "3 пункта trust-list",
    [
        plant_fixture(),
        goto("/plants/{plant.id}"),
        count(".trust-list li", 3),
    ],
)
sc(
    "SC-019",
    "plant",
    "Покупатель добавляет экземпляр в корзину",
    "Кнопка «В корзине», товар в корзине",
    [
        plant_fixture(),
        goto("/plants/{plant.id}"),
        click("button:has-text('В корзину')"),
        wait_text("В корзине"),
        click("header a[href='/cart']"),
        wait_url("/cart"),
        wait_text("{plant.code}"),
        text("{plant.productName}"),
    ],
)
sc(
    "SC-020",
    "plant",
    "Покупатель использует «Купить сейчас»",
    "Открывается checkout с товаром",
    [
        plant_fixture(),
        goto("/plants/{plant.id}"),
        click("button:has-text('Купить сейчас')"),
        wait_text("Оформление заказа"),
        text("{plant.code}"),
    ],
    auth="customer",
)
sc(
    "SC-021",
    "plant",
    "Повторное добавление экземпляра не увеличивает количество",
    "В шапке «Корзина (1)»",
    [
        plant_fixture(),
        goto("/plants/{plant.id}"),
        click("button:has-text('В корзину')"),
        click("button:has-text('В корзине')"),
        wait_text("Корзина (1)"),
    ],
)
sc(
    "SC-022",
    "plant",
    "Покупатель подписывается на поступление",
    "Сообщение «Готово: напишем»",
    [
        plant_fixture(sold_out=True),
        goto("/plants/{plant.id}"),
        text("Появится снова?"),
        fill("#restock-email", "sc-restock@example.test"),
        click("button:has-text('Сообщить о поступлении')"),
        wait_text("Готово: напишем на sc-restock@example.test"),
    ],
)
sc(
    "SC-023",
    "plant",
    "Форма restock не отправляется с невалидным e-mail",
    "Поле :invalid",
    [
        plant_fixture(sold_out=True),
        goto("/plants/{plant.id}"),
        fill("#restock-email", "not-an-email"),
        click("button:has-text('Сообщить о поступлении')"),
        visible("#restock-email:invalid"),
    ],
)
sc(
    "SC-024",
    "plant",
    "Ссылка «Назад в каталог» возвращает в каталог",
    "URL /catalog",
    [
        plant_fixture(),
        goto("/plants/{plant.id}"),
        click(".back-link"),
        wait_url("/catalog"),
    ],
)
sc(
    "SC-025",
    "plant",
    "Покупатель добавляет вариант продукта в корзину",
    "Корзина (1)",
    [
        *variant_fixture(),
        goto("/catalog/{product.id}"),
        click(".variant-card button"),
        wait_text("Корзина (1)"),
    ],
)

# =============================================================================
# C. Cart — SC-026 .. SC-035
# =============================================================================
sc(
    "SC-026",
    "cart",
    "Пустая корзина показывает пустое состояние",
    "Текст «Корзина пуста» и ссылка",
    [goto("/cart"), text("Корзина пуста"), visible(".empty a[href='/catalog']")],
)
sc(
    "SC-027",
    "cart",
    "Экземпляр отображается в корзине",
    "Код, название и цена",
    [
        plant_fixture(),
        cart_plant(),
        goto("/cart"),
        wait_text("{plant.code}"),
        text("{plant.productName}", "120,00"),
    ],
)
sc(
    "SC-028",
    "cart",
    "Корзина с двумя экземплярами считает итог",
    "Итого 270,00",
    [
        plant_fixture(12000, store="plantA"),
        plant_fixture(15000, store="plantB"),
        {
            "action": "setCart",
            "items": [
                {
                    "type": "INDIVIDUAL_PLANT",
                    "id": "{plantA.id}",
                    "name": "{plantA.productName} ({plantA.code})",
                    "priceMinor": 12000,
                    "currency": "BYN",
                    "quantity": 1,
                },
                {
                    "type": "INDIVIDUAL_PLANT",
                    "id": "{plantB.id}",
                    "name": "{plantB.productName} ({plantB.code})",
                    "priceMinor": 15000,
                    "currency": "BYN",
                    "quantity": 1,
                },
            ],
        },
        goto("/cart"),
        wait_text("270,00"),
        {"action": "expectPricesAligned"},
    ],
)
sc(
    "SC-029",
    "cart",
    "Удаление товара очищает корзину",
    "После «Удалить» — «Корзина пуста»",
    [
        plant_fixture(),
        cart_plant(),
        goto("/cart"),
        click("button:has-text('Удалить')"),
        wait_text("Корзина пуста"),
    ],
)
sc(
    "SC-030",
    "cart",
    "Корзина сохраняется после перезагрузки",
    "Товар виден после reload",
    [
        plant_fixture(),
        cart_plant(),
        goto("/cart"),
        {"action": "reload"},
        wait_text("{plant.code}"),
    ],
)
sc(
    "SC-031",
    "cart",
    "Счётчик корзины в шапке",
    "«Корзина (2)» после двух товаров",
    [
        plant_fixture(12000, store="plantA"),
        plant_fixture(15000, store="plantB"),
        {
            "action": "setCart",
            "items": [
                {
                    "type": "INDIVIDUAL_PLANT",
                    "id": "{plantA.id}",
                    "name": "{plantA.productName} ({plantA.code})",
                    "priceMinor": 12000,
                    "currency": "BYN",
                    "quantity": 1,
                },
                {
                    "type": "INDIVIDUAL_PLANT",
                    "id": "{plantB.id}",
                    "name": "{plantB.productName} ({plantB.code})",
                    "priceMinor": 15000,
                    "currency": "BYN",
                    "quantity": 1,
                },
            ],
        },
        goto("/catalog"),
        wait_text("Корзина (2)"),
    ],
)
sc(
    "SC-032",
    "cart",
    "Переход из корзины в checkout",
    "Страница «Оформление заказа»",
    [
        plant_fixture(),
        cart_plant(),
        goto("/cart"),
        click("a:has-text('Перейти к оформлению')"),
        wait_text("Оформление заказа"),
    ],
    auth="customer",
)
sc(
    "SC-033",
    "cart",
    "Вариант отображается в корзине",
    "Название варианта и цена",
    [
        *variant_fixture(),
        cart_variant(),
        goto("/cart"),
        wait_text("{variant.name}"),
        text("150,00"),
    ],
)
sc(
    "SC-034",
    "cart",
    "Удаление всех товаров показывает пустую корзину",
    "После удаления — пусто",
    [
        plant_fixture(),
        cart_plant(),
        goto("/cart"),
        click("button:has-text('Удалить')"),
        wait_text("Корзина пуста"),
    ],
)
sc(
    "SC-035",
    "cart",
    "Количество варианта увеличивает итог",
    "Итого 300,00 за 2 шт.",
    [
        *variant_fixture(),
        cart_variant(quantity=2),
        goto("/cart"),
        wait_text("300,00"),
    ],
)

# =============================================================================
# D. Checkout and orders — SC-036 .. SC-055
# =============================================================================
sc(
    "SC-036",
    "checkout",
    "Гость видит форму оформления без регистрации",
    "Заголовок checkout и поле e-mail",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        wait_text("Оформление заказа"),
        visible("input[name='guestEmail']"),
    ],
)
sc(
    "SC-037",
    "checkout",
    "Логин из checkout возвращает к оформлению",
    "После входа — форма checkout",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        click(".checkout-login"),
        fill("input[name='username']", "qa.customer@plantmarket.local"),
        fill("input[name='password']", "QaCustomer123!"),
        click("button[type='submit']"),
        wait_text("Оформление заказа"),
    ],
)
sc(
    "SC-038",
    "checkout",
    "Покупатель оплачивает экземпляр",
    "«Заказ оплачен»",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_text("Заказ оплачен"),
    ],
    auth="customer",
)
sc(
    "SC-039",
    "orders",
    "Оплаченный заказ виден в истории",
    "Короткий ID и PAID",
    [
        *paid_order(),
        goto("/account/orders"),
        text("{order.shortUpper}", "PAID"),
    ],
    auth="customer",
)
sc(
    "SC-040",
    "orders",
    "Детали заказа показывают адрес и статусы",
    "Адрес доставки и timeline",
    [
        *paid_order(),
        goto("/account/orders"),
        click("a[href='/account/orders/{order.id}']"),
        wait_url("/account/orders/"),
        wait_text("Адрес доставки"),
        text("PAID"),
    ],
    auth="customer",
)
sc(
    "SC-041",
    "checkout",
    "Checkout двух экземпляров оплачивает общий итог",
    "«Заказ оплачен»",
    [
        plant_fixture(12000, store="plantA"),
        plant_fixture(15000, store="plantB"),
        {
            "action": "setCart",
            "items": [
                {
                    "type": "INDIVIDUAL_PLANT",
                    "id": "{plantA.id}",
                    "name": "{plantA.productName} ({plantA.code})",
                    "priceMinor": 12000,
                    "currency": "BYN",
                    "quantity": 1,
                },
                {
                    "type": "INDIVIDUAL_PLANT",
                    "id": "{plantB.id}",
                    "name": "{plantB.productName} ({plantB.code})",
                    "priceMinor": 15000,
                    "currency": "BYN",
                    "quantity": 1,
                },
            ],
        },
        goto("/checkout"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_text("Заказ оплачен"),
    ],
    auth="customer",
)
sc(
    "SC-042",
    "checkout",
    "Покупатель оплачивает вариант",
    "«Заказ оплачен»",
    [
        *variant_fixture(),
        cart_variant(),
        goto("/checkout"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_text("Заказ оплачен"),
    ],
    auth="customer",
)
sc(
    "SC-043",
    "checkout",
    "Зимняя зона требует термоупаковку",
    "Текст про термоупаковку и disabled-кнопка",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        fill(".checkout-form textarea", "Гомель, Советская 1"),
        select(".checkout-form select", "Гомель (зима) · 2–4 дн."),
        text("Термоупаковка обязательна"),
        disabled("button:has-text('Оплатить')"),
    ],
    auth="customer",
)
sc(
    "SC-044",
    "checkout",
    "Бесплатная доставка от 100 BYN",
    "Доставка 0,00",
    [
        plant_fixture(12000),
        cart_plant(),
        goto("/checkout"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        text("0,00"),
    ],
    auth="customer",
)
sc(
    "SC-045",
    "checkout",
    "Термоупаковка добавляет стоимость",
    "Итого 132,00 для Гомеля",
    [
        plant_fixture(12000),
        cart_plant(),
        goto("/checkout"),
        fill(".checkout-form textarea", "Гомель, Советская 1"),
        select(".checkout-form select", "Гомель (зима) · 2–4 дн."),
        check(".checkout-thermal input"),
        text("132,00"),
    ],
    auth="customer",
)
sc(
    "SC-046",
    "checkout",
    "Кнопка оплаты заблокирована без адреса",
    "disabled до адреса, enabled после",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        disabled("button:has-text('Оплатить')"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        enabled("button:has-text('Оплатить')"),
    ],
    auth="customer",
)
sc(
    "SC-047",
    "checkout",
    "Пустой checkout показывает пустую корзину",
    "«Корзина пуста»",
    [goto("/checkout"), text("Корзина пуста")],
)
sc(
    "SC-048",
    "checkout",
    "Checkout с товаром предлагает гостевую форму",
    "Поле guestEmail и ссылка входа",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        wait_text("Оформление заказа"),
        visible("input[name='guestEmail']", ".checkout-login"),
    ],
)
sc(
    "SC-049",
    "orders",
    "Новый покупатель видит пустую историю",
    "«Заказов пока нет»",
    [
        api("registerCustomer", "other"),
        goto("/api/auth/login?returnTo=/account/orders"),
        fill("input[name='username']", "{other.email}"),
        fill("input[name='password']", "{other.password}"),
        click("button[type='submit']"),
        wait_text("Мои заказы"),
        text("Заказов пока нет"),
    ],
)
sc(
    "SC-050",
    "orders",
    "История заказа показывает все статусы",
    "DRAFT, PENDING_PAYMENT, PAID",
    [
        *paid_order(),
        goto("/account/orders"),
        click("a[href='/account/orders/{order.id}']"),
        wait_url("/account/orders/"),
        wait_text("DRAFT"),
        text("PENDING_PAYMENT", "PAID"),
    ],
    auth="customer",
)
sc(
    "SC-051",
    "orders",
    "Чужой заказ недоступен",
    "«Заказ не найден»",
    [
        api("registerCustomer", "other"),
        plant_fixture(),
        api(
            "createOrder",
            "order",
            token="{other.token}",
            items=[{"individualPlantId": "{plant.id}", "quantity": 1}],
        ),
        api("payOrder", "paid", orderId="{order.id}", token="{other.token}"),
        goto("/account/orders/{order.id}"),
        text("Заказ не найден"),
    ],
    auth="customer",
)
sc(
    "SC-052",
    "orders",
    "Список заказов показывает PAID",
    "PAID в истории",
    [*paid_order(), goto("/account/orders"), text("PAID")],
    auth="customer",
)
sc(
    "SC-053",
    "checkout",
    "Оплата недоступного экземпляра показывает ошибку",
    "Видимое сообщение об ошибке",
    [
        plant_fixture(sold_out=True),
        cart_plant(),
        goto("/checkout"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_sel(".form-message.error"),
    ],
    auth="customer",
)
sc(
    "SC-054",
    "checkout",
    "Checkout с вариантом и количеством 2",
    "«Заказ оплачен»",
    [
        *variant_fixture(),
        cart_variant(quantity=2),
        goto("/checkout"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_text("Заказ оплачен"),
    ],
    auth="customer",
)
sc(
    "SC-055",
    "orders",
    "Ссылка «Мои заказы» в шапке ведёт в историю",
    "Заголовок «Мои заказы»",
    [*paid_order(), goto("/catalog"), click("header a[href='/account/orders']"), wait_text("Мои заказы")],
    auth="customer",
)

# =============================================================================
# E. Auth — SC-056 .. SC-065
# =============================================================================
sc(
    "SC-056",
    "auth",
    "Неверный пароль показывает ошибку",
    "«Неверный e-mail или пароль»",
    [
        goto("/api/auth/login?returnTo=/catalog"),
        fill("input[name='username']", "qa.customer@plantmarket.local"),
        fill("input[name='password']", "WrongPassword123!"),
        click("button[type='submit']"),
        wait_text("Неверный e-mail или пароль"),
    ],
)
sc(
    "SC-057",
    "auth",
    "Верный пароль открывает каталог",
    "Ссылки «Мои заказы» и «Выйти»",
    [
        goto("/api/auth/login?returnTo=/catalog"),
        fill("input[name='username']", "qa.customer@plantmarket.local"),
        fill("input[name='password']", "QaCustomer123!"),
        click("button[type='submit']"),
        wait_text("Выйти"),
        text("Мои заказы"),
    ],
)
sc(
    "SC-058",
    "auth",
    "Выход возвращает в каталог",
    "Ссылка «Войти»",
    [
        goto("/api/auth/login?returnTo=/catalog"),
        fill("input[name='username']", "qa.customer@plantmarket.local"),
        fill("input[name='password']", "QaCustomer123!"),
        click("button[type='submit']"),
        wait_text("Выйти"),
        click("button.nav-button"),
        wait_url("/catalog"),
        wait_text("Войти"),
    ],
)
sc(
    "SC-059",
    "auth",
    "Защищённые заказы просят войти",
    "«Войдите, чтобы увидеть заказы»",
    [goto("/account/orders"), text("Войдите, чтобы увидеть заказы")],
)
sc(
    "SC-060",
    "auth",
    "Экран входа содержит форму и CSRF",
    "E-MAIL, ПАРОЛЬ, _csrf",
    [
        goto("/api/auth/login?returnTo=/catalog"),
        text("E-MAIL", "ПАРОЛЬ"),
        count("input[name='_csrf']", 1),
        visible("button[type='submit']"),
    ],
)
sc(
    "SC-061",
    "auth",
    "Сессия сохраняется после перезагрузки",
    "После reload всё ещё «Выйти»",
    [
        goto("/api/auth/login?returnTo=/catalog"),
        fill("input[name='username']", "qa.customer@plantmarket.local"),
        fill("input[name='password']", "QaCustomer123!"),
        click("button[type='submit']"),
        wait_text("Выйти"),
        {"action": "reload"},
        wait_text("Выйти"),
    ],
)
sc(
    "SC-062",
    "auth",
    "Неверный пароль админки показывает ошибку",
    "Invalid email or password",
    [
        goto_admin("/"),
        fill("input[type='email']", "admin@plantmarket.local"),
        fill("input[type='password']", "WrongPassword123!"),
        click("button[type='submit']"),
        wait_text("Invalid email or password"),
    ],
)
sc(
    "SC-063",
    "auth",
    "Вход администратора открывает продукты",
    "Раздел «Продукты»",
    [
        goto_admin("/"),
        fill("input[type='email']", "admin@plantmarket.local"),
        fill("input[type='password']", "AdminPassword123!"),
        click("button[type='submit']"),
        wait_text("Продукты"),
    ],
)
sc(
    "SC-064",
    "auth",
    "Выход из админки возвращает на вход",
    "Заголовок «Админка»",
    [
        tab("Продукты"),
        click("button:has-text('Выйти')"),
        wait_text("Админка"),
    ],
    auth="admin",
)
sc(
    "SC-065",
    "auth",
    "Админская сессия сохраняется после reload",
    "После reload «Продукты»",
    [
        {"action": "reload"},
        wait_text("Продукты"),
    ],
    auth="admin",
)

# =============================================================================
# F. Admin catalog CRUD — SC-066 .. SC-090
# =============================================================================
sc(
    "SC-066",
    "admin",
    "Администратор создаёт категорию",
    "«Категория создана» и строка таблицы",
    [
        tab("Категории"),
        fill("input[name='name']", "SC Category {uid}"),
        fill("input[name='slug']", "sc-category-{uid}"),
        click("button:has-text('Создать')"),
        wait_text("Категория создана"),
        wait_text("SC Category"),
    ],
    auth="admin",
)
sc(
    "SC-067",
    "admin",
    "Администратор создаёт продукт",
    "«Продукт создан» и строка таблицы",
    [
        tab("Продукты"),
        fill("input[name='sku']", "SC-P-{uid}"),
        fill("input[name='name']", "SC Product {uid}"),
        fill("input[name='slug']", "sc-product-{uid}"),
        select("select[name='categoryId']", "Монстеры"),
        fill("input[name='price']", "100"),
        check("input[name='variegated']"),
        click("button:has-text('Создать продукт')"),
        wait_text("Продукт создан"),
        wait_text("SC Product"),
    ],
    auth="admin",
)
sc(
    "SC-068",
    "admin",
    "Администратор редактирует цену продукта",
    "«Продукт обновлён» и новая цена",
    [
        api(
            "createProduct",
            "product",
            sku="SC-EDIT-{uid}",
            name="SC Editable {uid}",
            slug="sc-editable-{uid}",
            priceMinor=10000,
        ),
        {"action": "reload"},
        tab("Продукты"),
        click("tr:has-text('{product.sku}') button:has-text('Редактировать')"),
        fill("input[name='price']", "111"),
        click("button:has-text('Сохранить продукт')"),
        wait_text("Продукт обновлён"),
        wait_text("111,00"),
    ],
    auth="admin",
)
sc(
    "SC-069",
    "admin",
    "Администратор архивирует продукт",
    "«Продукт архивирован»",
    [
        api(
            "createProduct",
            "product",
            sku="SC-ARCH-{uid}",
            name="SC Archive {uid}",
            slug="sc-archive-{uid}",
        ),
        {"action": "reload"},
        tab("Продукты"),
        click("tr:has-text('{product.sku}') button:has-text('Архивировать')"),
        wait_text("Продукт архивирован"),
    ],
    auth="admin",
)
sc(
    "SC-070",
    "admin",
    "Администратор создаёт экземпляр",
    "«Экземпляр создан (ACQUIRED)»",
    [
        tab("Экземпляры"),
        select("select[name='productId']", "Philodendron Pink Princess"),
        fill("input[name='code']", "SC-IP-{uid}"),
        fill("input[name='price']", "120"),
        click("button:has-text('Создать экземпляр')"),
        wait_text("Экземпляр создан (ACQUIRED)"),
        wait_text("SC-IP-"),
    ],
    auth="admin",
)
sc(
    "SC-071",
    "admin",
    "Администратор переводит экземпляр в карантин",
    "«Переведён в карантин»",
    [
        plant_fixture(sold_out=True),
        {"action": "reload"},
        tab("Экземпляры"),
        click("tr:has-text('{plant.code}') button:has-text('В карантин')"),
        wait_text("Переведён в карантин"),
    ],
    auth="admin",
)
sc(
    "SC-072",
    "admin",
    "Администратор публикует экземпляр",
    "«Опубликован»",
    [
        plant_fixture(sold_out=True),
        {"action": "reload"},
        tab("Экземпляры"),
        click("tr:has-text('{plant.code}') button:has-text('В карантин')"),
        wait_text("Переведён в карантин"),
        click("tr:has-text('{plant.code}') button:has-text('Опубликовать')"),
        wait_text("Опубликован"),
    ],
    auth="admin",
)
sc(
    "SC-073",
    "admin",
    "Администратор создаёт вариант",
    "«Вариант создан» и строка таблицы",
    [
        tab("Варианты"),
        select("select[name='productId']", "Monstera Deliciosa"),
        fill("input[name='sku']", "SC-V-{uid}"),
        fill("input[name='name']", "12 cm pot"),
        fill("input[name='price']", "150"),
        click("button:has-text('Создать вариант')"),
        wait_text("Вариант создан"),
        wait_text("SC-V-"),
    ],
    auth="admin",
)
sc(
    "SC-074",
    "admin",
    "Администратор редактирует цену варианта",
    "«Вариант обновлён»",
    [
        *variant_fixture(15000),
        {"action": "reload"},
        tab("Варианты"),
        select("select[name='productId']", "{product.name}"),
        click("tr:has-text('{variant.sku}') button:has-text('Редактировать')"),
        fill("input[name='price']", "175"),
        click("button:has-text('Сохранить вариант')"),
        wait_text("Вариант обновлён"),
        wait_text("175,00"),
    ],
    auth="admin",
)
sc(
    "SC-075",
    "admin",
    "Администратор архивирует вариант",
    "«Вариант архивирован»",
    [
        *variant_fixture(),
        {"action": "reload"},
        tab("Варианты"),
        select("select[name='productId']", "{product.name}"),
        click("tr:has-text('{variant.sku}') button:has-text('Архивировать')"),
        wait_text("Вариант архивирован"),
    ],
    auth="admin",
)
sc(
    "SC-076",
    "admin",
    "Администратор создаёт партию",
    "«Партия создана (QUARANTINE)»",
    [
        tab("Партии"),
        select("select", "Monstera Deliciosa"),
        select("select[name='productVariantId']", "MON-001-12 · 12 cm pot"),
        fill("input[name='supplier']", "SC Supplier"),
        fill("input[name='quantity']", "5"),
        fill("input[name='unitCost']", "40"),
        click("button:has-text('Создать партию')"),
        wait_text("Партия создана (QUARANTINE)"),
    ],
    auth="admin",
)
sc(
    "SC-077",
    "admin",
    "Администратор выпускает партию",
    "«Партия опубликована»",
    [
        tab("Партии"),
        select("select", "Monstera Deliciosa"),
        select("select[name='productVariantId']", "MON-001-12 · 12 cm pot"),
        fill("input[name='supplier']", "SC Supplier"),
        fill("input[name='quantity']", "5"),
        fill("input[name='unitCost']", "40"),
        click("button:has-text('Создать партию')"),
        wait_text("Партия создана (QUARANTINE)"),
        click("tr:has-text('SC Supplier') button:has-text('Выпустить')"),
        wait_text("Партия опубликована"),
    ],
    auth="admin",
)
sc(
    "SC-078",
    "admin",
    "Администратор открывает все вкладки",
    "6 вкладок и заголовки разделов",
    [
        count(".tab", 6),
        tab("Категории"),
        wait_text("Категории"),
        tab("Экземпляры"),
        wait_text("Экземпляры"),
        tab("Варианты"),
        wait_text("Варианты продукта"),
        tab("Партии"),
        wait_text("Новая партия"),
        tab("Претензии"),
        wait_text("Претензии"),
    ],
    auth="admin",
)
sc(
    "SC-079",
    "admin",
    "Таблица продуктов показывает бейдж статуса",
    "Виден .badge",
    [wait_sel(".badge"), visible(".badge")],
    auth="admin",
)
sc(
    "SC-080",
    "admin",
    "Верхняя панель содержит обновление и выход",
    "Кнопки «Обновить» и «Выйти»",
    [visible("button:has-text('Обновить')", "button:has-text('Выйти')")],
    auth="admin",
)
sc(
    "SC-081",
    "admin",
    "Обновление админки сохраняет данные",
    "После reload «Продукты»",
    [
        {"action": "reload"},
        wait_text("Продукты"),
    ],
    auth="admin",
)
sc(
    "SC-082",
    "admin",
    "Строка таблицы при hover остаётся тёмной",
    "background-color surface-2",
    [
        wait_sel("tbody tr"),
        hover("tbody tr"),
        {
            "action": "expectCss",
            "selector": "tbody tr",
            "property": "background-color",
            "value": "rgb(23, 40, 31)",
        },
    ],
    auth="admin",
)
sc(
    "SC-083",
    "admin",
    "Галерея сохраняет URL общего плана",
    "«Галерея сохранена»",
    [
        plant_fixture(),
        {"action": "reload"},
        tab("Экземпляры"),
        click("tr:has-text('{plant.code}') button:has-text('Редактировать')"),
        wait_text("Галерея WYSIWYG"),
        fill("input[placeholder='https://…']", "https://example.test/sc-general.jpg"),
        click("button:has-text('Сохранить галерею')"),
        wait_text("Галерея сохранена"),
    ],
    auth="admin",
)
sc(
    "SC-084",
    "admin",
    "Галерея принимает загруженный файл",
    "«Фото загружено»",
    [
        plant_fixture(),
        {"action": "reload"},
        tab("Экземпляры"),
        click("tr:has-text('{plant.code}') button:has-text('Редактировать')"),
        wait_text("Галерея WYSIWYG"),
        upload("input[type='file']", "../../clients/storefront/public/images/plants/leaf.jpg"),
        wait_text("Фото загружено — не забудьте сохранить галерею"),
    ],
    auth="admin",
)
sc(
    "SC-085",
    "admin",
    "Галерея требует общий план",
    "Ошибка при сохранении пустой галереи",
    [
        plant_fixture(),
        {"action": "reload"},
        tab("Экземпляры"),
        click("tr:has-text('{plant.code}') button:has-text('Редактировать')"),
        wait_text("Галерея WYSIWYG"),
        click("button:has-text('Сохранить галерею')"),
        wait_sel(".form-message"),
    ],
    auth="admin",
)
sc(
    "SC-086",
    "admin",
    "Оператор подтверждает замену по претензии",
    "REPLACEMENT в строке",
    [
        *paid_order(),
        api("createClaim", "claim", orderId="{order.id}", reason="SC damaged leaf"),
        tab("Претензии"),
        wait_sel("tr:has-text('{claim.short}')"),
        click("tr:has-text('{claim.short}') button:has-text('Решить')"),
        wait_text("REPLACEMENT"),
    ],
    auth="admin",
)
sc(
    "SC-087",
    "admin",
    "Оператор выбирает возврат и комментарий",
    "REFUND и комментарий",
    [
        *paid_order(),
        api("createClaim", "claim", orderId="{order.id}", reason="SC refund claim"),
        tab("Претензии"),
        wait_sel("tr:has-text('{claim.short}')"),
        select("tr:has-text('{claim.short}') select", "Возврат"),
        fill("tr:has-text('{claim.short}') input", "Возврат согласован"),
        click("tr:has-text('{claim.short}') button:has-text('Решить')"),
        wait_text("REFUND"),
        wait_text("Возврат согласован"),
    ],
    auth="admin",
)
sc(
    "SC-088",
    "admin",
    "Оператор отклоняет претензию",
    "REJECT",
    [
        *paid_order(),
        api("createClaim", "claim", orderId="{order.id}", reason="SC reject claim"),
        tab("Претензии"),
        wait_sel("tr:has-text('{claim.short}')"),
        select("tr:has-text('{claim.short}') select", "Отказ"),
        click("tr:has-text('{claim.short}') button:has-text('Решить')"),
        wait_text("REJECT"),
    ],
    auth="admin",
)
sc(
    "SC-089",
    "admin",
    "Таблица претензий показывает фото",
    "Ссылка «фото 1»",
    [
        *paid_order(),
        api("createClaim", "claim", orderId="{order.id}", reason="SC photo claim"),
        tab("Претензии"),
        wait_sel("tr:has-text('{claim.short}')"),
        visible("tr:has-text('{claim.short}') a:has-text('фото 1')"),
    ],
    auth="admin",
)
sc(
    "SC-090",
    "admin",
    "Пустая галерея не ломает редактор",
    "Заголовок «Галерея WYSIWYG»",
    [
        plant_fixture(),
        {"action": "reload"},
        tab("Экземпляры"),
        click("tr:has-text('{plant.code}') button:has-text('Редактировать')"),
        wait_text("Галерея WYSIWYG"),
        text("Минимум общий план"),
    ],
    auth="admin",
)

# =============================================================================
# G. Consent, SEO, responsive, a11y — SC-091 .. SC-100
# =============================================================================
sc(
    "SC-091",
    "consent",
    "Отказ от аналитики скрывает баннер",
    "Баннер исчез",
    [
        goto("/catalog"),
        wait_sel(".cookie-banner"),
        visible(".cookie-banner"),
        click(".cookie-banner button:has-text('Только необходимые')"),
        hidden(".cookie-banner"),
    ],
)
sc(
    "SC-092",
    "consent",
    "Принятие аналитики сохраняется",
    "pm.consent.v1=all",
    [
        goto("/catalog"),
        wait_sel(".cookie-banner"),
        click(".cookie-banner button:has-text('Принять аналитику')"),
        storage("pm.consent.v1", "all"),
    ],
)
sc(
    "SC-093",
    "seo",
    "robots.txt содержит Sitemap",
    "HTTP 200 и Sitemap",
    [goto("/robots.txt"), text("Sitemap")],
)
sc(
    "SC-094",
    "seo",
    "sitemap.xml содержит каталог",
    "URL /catalog",
    [goto("/sitemap.xml"), text("/catalog")],
)
sc(
    "SC-095",
    "seo",
    "Карточка экземпляра содержит JSON-LD",
    "Один JSON-LD",
    [
        plant_fixture(),
        goto("/plants/{plant.id}"),
        count("script[type='application/ld+json']", 1),
    ],
)
sc(
    "SC-096",
    "responsive",
    "Каталог без горизонтального скролла на мобильном",
    "Нет overflow",
    [goto("/catalog"), overflow()],
    viewport=[390, 844],
)
sc(
    "SC-097",
    "responsive",
    "Checkout без горизонтального скролла на мобильном",
    "Нет overflow",
    [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel(".checkout-form"), overflow()],
    auth="customer",
    viewport=[390, 844],
)
sc(
    "SC-098",
    "responsive",
    "Экран входа админки без горизонтального скролла",
    "Нет overflow",
    [goto_admin("/"), overflow()],
    viewport=[390, 844],
)
sc(
    "SC-099",
    "a11y",
    "Экран входа проходит a11y-смоук",
    "alt/labels/h1/lang",
    [goto("/api/auth/login?returnTo=/catalog"), a11y()],
)
sc(
    "SC-100",
    "a11y",
    "Checkout проходит a11y-смоук",
    "alt/labels/h1/lang",
    [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel(".checkout-form"), a11y()],
    auth="customer",
)


# =============================================================================
# H. Additional 150 scenarios — SC-101 .. SC-250
# =============================================================================

# --- Filters (25) ------------------------------------------------------------
filter_cases = [
    ("", False, None, 11, "весь каталог"),
    ("", True, None, 6, "только вариегатные"),
    ("LOW", False, None, 1, "низкий свет"),
    ("MEDIUM", False, None, 3, "средний свет"),
    ("BRIGHT", False, None, 1, "яркий свет"),
    ("DIRECT", False, None, 1, "прямой свет"),
    ("LOW", True, None, 0, "вариегатные + низкий свет"),
    ("MEDIUM", True, None, 3, "вариегатные + средний свет"),
    ("BRIGHT", True, None, 1, "вариегатные + яркий свет"),
    ("DIRECT", True, None, 1, "вариегатные + прямой свет"),
    ("", False, 50, 1, "цена до 50 BYN"),
    ("", False, 100, 1, "цена до 100 BYN"),
    ("", False, 150, 1, "цена до 150 BYN"),
    ("", False, 500, 5, "цена до 500 BYN"),
    ("", False, 1000, 8, "цена до 1000 BYN"),
    ("", False, 2000, 10, "цена до 2000 BYN"),
    ("MEDIUM", False, 150, 1, "средний свет + цена до 150"),
    ("MEDIUM", True, 150, 1, "вариегатные + средний + 150"),
    ("BRIGHT", False, 1000, 1, "яркий свет + 1000"),
    ("DIRECT", False, 100, 0, "прямой свет + 100"),
    ("LOW", False, 100, 1, "низкий свет + 100"),
    ("", True, 150, 1, "вариегатные + 150"),
    ("", True, 1000, 3, "вариегатные + 1000"),
    ("", False, 1, 0, "пустая выдача до 1 BYN"),
    ("MEDIUM", True, 1, 0, "пустая выдача medium + variegated"),
]
for index, (light, variegated, max_price, minimum, label) in enumerate(filter_cases, start=101):
    params = []
    if variegated:
        params.append("variegated=true")
    if light:
        params.append(f"light={light}")
    if max_price is not None:
        params.append(f"maxPrice={max_price}")
    path = "/catalog" + (("?" + "&".join(params)) if params else "")
    if minimum == 0:
        steps = [goto(path), wait_text("Под эти фильтры растений нет"), visible(".empty a[href='/catalog']")]
        expected = "Пустое состояние и ссылка сброса"
    else:
        steps = [goto(path), wait_text("Найдено"), min_count(".plant-card", minimum)]
        expected = f"URL {path} и минимум {minimum} карточек"
    sc(f"SC-{index}", "catalog", f"Фильтр: {label}", expected, steps)

# --- Plant detail, gallery, restock, variants (20) ---------------------------
seed_plants = [
    ("IP-001", "Philodendron Pink Princess", "120,00"),
    ("IP-002", "Anthurium Clarinervium", "1 899,00"),
    ("IP-003", "Alocasia Frydek", "899,00"),
    ("IP-004", "Syngonium Albo", "1 599,00"),
    ("IP-005", "Scindapsus Treubii Moonlight", "749,00"),
]
index = 126
for code, name, price in seed_plants:
    sc(
        f"SC-{index}",
        "plant",
        f"Экземпляр {code} показывает название и цену",
        f"{name} и {price}",
        [
            api("findPlant", "plant", code=code),
            goto("/plants/{plant.id}"),
            text(name, price),
            visible(".wysiwyg-badge"),
        ],
    )
    index += 1
for code, name, _price in seed_plants:
    sc(
        f"SC-{index}",
        "plant",
        f"Галерея {code} переключает ракурс",
        "Ракурс «Лист крупно»",
        [
            api("findPlant", "plant", code=code),
            goto("/plants/{plant.id}"),
            click(".gallery-thumbs li:nth-child(2) .gallery-thumb"),
            wait_text("Лист крупно"),
        ],
    )
    index += 1
for code, name, price in seed_plants:
    sc(
        f"SC-{index}",
        "plant",
        f"Добавление {code} в корзину",
        "Товар в корзине",
        [
            api("findPlant", "plant", code=code),
            goto("/plants/{plant.id}"),
            click("button:has-text('В корзину')"),
            wait_text("В корзине"),
            click("header a[href='/cart']"),
            wait_url("/cart"),
            wait_text(name),
        ],
    )
    index += 1
sc(
    "SC-141",
    "plant",
    "Restock для проданного экземпляра",
    "Сообщение «Готово: напишем»",
    [
        plant_fixture(sold_out=True),
        goto("/plants/{plant.id}"),
        fill("#restock-email", "sc-restock-141@example.test"),
        click("button:has-text('Сообщить о поступлении')"),
        wait_text("Готово: напишем на sc-restock-141@example.test"),
    ],
)
sc(
    "SC-142",
    "plant",
    "Restock отклоняет невалидный e-mail",
    "Поле :invalid",
    [
        plant_fixture(sold_out=True),
        goto("/plants/{plant.id}"),
        fill("#restock-email", "bad"),
        click("button:has-text('Сообщить о поступлении')"),
        visible("#restock-email:invalid"),
    ],
)
sc(
    "SC-143",
    "plant",
    "Вариант продукта добавляется в корзину",
    "Корзина (1)",
    [*variant_fixture(), goto("/catalog/{product.id}"), click(".variant-card button"), wait_text("Корзина (1)")],
)
sc(
    "SC-144",
    "plant",
    "Страница продукта показывает вариант и цену",
    "Вариант и 150,00",
    [*variant_fixture(), goto("/catalog/{product.id}"), text("Варианты", "150,00")],
)
sc(
    "SC-145",
    "plant",
    "Экземпляр показывает три пункта доверия",
    "3 пункта",
    [plant_fixture(), goto("/plants/{plant.id}"), count(".trust-list li", 3)],
)

# --- Cart (10) ---------------------------------------------------------------
sc("SC-146", "cart", "Пустая корзина", "Корзина пуста", [goto("/cart"), text("Корзина пуста")])
sc(
    "SC-147",
    "cart",
    "Один экземпляр в корзине",
    "Код и цена",
    [plant_fixture(), cart_plant(), goto("/cart"), wait_text("{plant.code}"), text("120,00")],
)
sc(
    "SC-148",
    "cart",
    "Два разных экземпляра в корзине",
    "Итого 270,00",
    [
        plant_fixture(12000, store="plantA"),
        plant_fixture(15000, store="plantB"),
        {
            "action": "setCart",
            "items": [
                {"type": "INDIVIDUAL_PLANT", "id": "{plantA.id}", "name": "{plantA.productName} ({plantA.code})", "priceMinor": 12000, "currency": "BYN", "quantity": 1},
                {"type": "INDIVIDUAL_PLANT", "id": "{plantB.id}", "name": "{plantB.productName} ({plantB.code})", "priceMinor": 15000, "currency": "BYN", "quantity": 1},
            ],
        },
        goto("/cart"),
        wait_text("270,00"),
    ],
)
sc(
    "SC-149",
    "cart",
    "Удаление товара",
    "Корзина пуста",
    [plant_fixture(), cart_plant(), goto("/cart"), click("button:has-text('Удалить')"), wait_text("Корзина пуста")],
)
sc(
    "SC-150",
    "cart",
    "Корзина переживает reload",
    "Код после reload",
    [plant_fixture(), cart_plant(), goto("/cart"), {"action": "reload"}, wait_text("{plant.code}")],
)
sc(
    "SC-151",
    "cart",
    "Счётчик двух товаров",
    "Корзина (2)",
    [
        plant_fixture(12000, store="plantA"),
        plant_fixture(15000, store="plantB"),
        {
            "action": "setCart",
            "items": [
                {"type": "INDIVIDUAL_PLANT", "id": "{plantA.id}", "name": "{plantA.productName} ({plantA.code})", "priceMinor": 12000, "currency": "BYN", "quantity": 1},
                {"type": "INDIVIDUAL_PLANT", "id": "{plantB.id}", "name": "{plantB.productName} ({plantB.code})", "priceMinor": 15000, "currency": "BYN", "quantity": 1},
            ],
        },
        goto("/catalog"),
        wait_text("Корзина (2)"),
    ],
)
sc(
    "SC-152",
    "cart",
    "Переход в checkout из корзины",
    "Оформление заказа",
    [plant_fixture(), cart_plant(), goto("/cart"), click("a:has-text('Перейти к оформлению')"), wait_text("Оформление заказа")],
    auth="customer",
)
sc(
    "SC-153",
    "cart",
    "Вариант в корзине",
    "Название и 150,00",
    [*variant_fixture(), cart_variant(), goto("/cart"), wait_text("{variant.name}"), text("150,00")],
)
sc(
    "SC-154",
    "cart",
    "Количество варианта 2",
    "Итого 300,00",
    [*variant_fixture(), cart_variant(quantity=2), goto("/cart"), wait_text("300,00")],
)
sc(
    "SC-155",
    "cart",
    "Удаление последнего товара",
    "Корзина пуста",
    [plant_fixture(), cart_plant(), goto("/cart"), click("button:has-text('Удалить')"), wait_text("Корзина пуста")],
)

# --- Checkout and orders (20) ------------------------------------------------
guest_checkout_cases = [
    ("MINSK", "Минск · 1–2 дн.", False, "120,00", "guest-minsk"),
    ("MINSK", "Минск · 1–2 дн.", False, "120,00", "guest-minsk-2"),
    ("GOMEL", "Гомель (зима) · 2–4 дн.", True, "132,00", "guest-gomel"),
    ("GOMEL", "Гомель (зима) · 2–4 дн.", True, "132,00", "guest-gomel-2"),
]
index = 156
for zone, label, thermal, total, email in guest_checkout_cases:
    steps = [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        wait_text("Оформление заказа"),
        {"action": "reload"},
        fill("input[name='guestEmail']", f"{email}@example.test"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", label),
    ]
    if thermal:
        steps.append(check(".checkout-thermal input"))
    steps += [text(total), click("button:has-text('Оплатить')"), wait_text("Заказ оплачен")]
    sc(f"SC-{index}", "checkout", f"Гостевая оплата: {email}", f"Итого {total}", steps)
    index += 1
sc(
    "SC-160",
    "checkout",
    "Гостевая форма требует e-mail",
    "Кнопка disabled без e-mail",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        wait_sel("input[name='guestEmail']"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        disabled("button:has-text('Оплатить')"),
        fill("input[name='guestEmail']", "guest-valid@example.test"),
        enabled("button:has-text('Оплатить')"),
    ],
)
sc(
    "SC-161",
    "checkout",
    "Гостевая форма отклоняет невалидный e-mail",
    "Кнопка disabled",
    [
        plant_fixture(),
        cart_plant(),
        goto("/checkout"),
        wait_sel("input[name='guestEmail']"),
        fill("input[name='guestEmail']", "not-an-email"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        disabled("button:has-text('Оплатить')"),
    ],
)
sc(
    "SC-162",
    "checkout",
    "Гость видит ссылку входа",
    "Ссылка «Войти в аккаунт»",
    [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel(".checkout-login"), visible(".checkout-login")],
)
sc(
    "SC-163",
    "checkout",
    "Гость может войти из checkout",
    "Форма входа",
    [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel(".checkout-login"), click(".checkout-login"), wait_text("Вход")],
)
sc(
    "SC-164",
    "checkout",
    "Оплата двух экземпляров гостем",
    "Заказ оплачен",
    [
        plant_fixture(12000, store="plantA"),
        plant_fixture(15000, store="plantB"),
        {
            "action": "setCart",
            "items": [
                {"type": "INDIVIDUAL_PLANT", "id": "{plantA.id}", "name": "{plantA.productName} ({plantA.code})", "priceMinor": 12000, "currency": "BYN", "quantity": 1},
                {"type": "INDIVIDUAL_PLANT", "id": "{plantB.id}", "name": "{plantB.productName} ({plantB.code})", "priceMinor": 15000, "currency": "BYN", "quantity": 1},
            ],
        },
        goto("/checkout"),
        wait_text("Оформление заказа"),
        {"action": "reload"},
        fill("input[name='guestEmail']", "guest-two@example.test"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_text("Заказ оплачен"),
    ],
)
sc(
    "SC-165",
    "checkout",
    "Гостевая оплата варианта",
    "Заказ оплачен",
    [
        *variant_fixture(),
        cart_variant(),
        goto("/checkout"),
        wait_text("Оформление заказа"),
        {"action": "reload"},
        fill("input[name='guestEmail']", "guest-variant@example.test"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_text("Заказ оплачен"),
    ],
)
sc(
    "SC-166",
    "checkout",
    "Пустой checkout гостя",
    "Корзина пуста",
    [goto("/checkout"), text("Корзина пуста")],
)
sc(
    "SC-167",
    "checkout",
    "Гостевая оплата недоступного экземпляра",
    "Ошибка",
    [
        plant_fixture(sold_out=True),
        cart_plant(),
        goto("/checkout"),
        fill("input[name='guestEmail']", "guest-soldout@example.test"),
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Минск · 1–2 дн."),
        click("button:has-text('Оплатить')"),
        wait_sel(".form-message.error"),
    ],
)
sc("SC-168", "orders", "Оплаченный заказ в истории", "PAID", [*paid_order(), goto("/account/orders"), wait_text("PAID")], auth="customer")
sc(
    "SC-169",
    "orders",
    "Детали оплаченного заказа",
    "Адрес доставки",
    [*paid_order(), goto("/account/orders"), click("a[href='/account/orders/{order.id}']"), wait_url("/account/orders/"), wait_text("Адрес доставки")],
    auth="customer",
)
sc(
    "SC-170",
    "orders",
    "Чужой заказ недоступен",
    "Заказ не найден",
    [
        api("registerCustomer", "other"),
        plant_fixture(),
        api("createOrder", "order", token="{other.token}", items=[{"individualPlantId": "{plant.id}", "quantity": 1}]),
        api("payOrder", "paid", orderId="{order.id}", token="{other.token}"),
        goto("/account/orders/{order.id}"),
        wait_text("Заказ не найден"),
    ],
    auth="customer",
)
sc(
    "SC-171",
    "orders",
    "Новый покупатель не имеет заказов",
    "Заказов пока нет",
    [
        api("registerCustomer", "other"),
        goto("/api/auth/login?returnTo=/account/orders"),
        fill("input[name='username']", "{other.email}"),
        fill("input[name='password']", "{other.password}"),
        click("button[type='submit']"),
        wait_text("Мои заказы"),
        text("Заказов пока нет"),
    ],
)
sc(
    "SC-172",
    "orders",
    "Timeline заказа",
    "DRAFT, PENDING_PAYMENT, PAID",
    [*paid_order(), goto("/account/orders"), click("a[href='/account/orders/{order.id}']"), wait_url("/account/orders/"), wait_text("DRAFT"), text("PENDING_PAYMENT", "PAID")],
    auth="customer",
)
sc(
    "SC-173",
    "checkout",
    "Бесплатная доставка гостем",
    "0,00",
    [plant_fixture(12000), cart_plant(), goto("/checkout"), fill("input[name='guestEmail']", "guest-free@example.test"), fill(".checkout-form textarea", "Минск, Независимости 1"), select(".checkout-form select", "Минск · 1–2 дн."), text("0,00")],
)
sc(
    "SC-174",
    "checkout",
    "Термоупаковка гостем",
    "132,00",
    [plant_fixture(12000), cart_plant(), goto("/checkout"), fill("input[name='guestEmail']", "guest-thermal@example.test"), fill(".checkout-form textarea", "Гомель, Советская 1"), select(".checkout-form select", "Гомель (зима) · 2–4 дн."), check(".checkout-thermal input"), text("132,00")],
)
sc(
    "SC-175",
    "checkout",
    "Кнопка оплаты гостя без адреса disabled",
    "disabled",
    [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel("input[name='guestEmail']"), fill("input[name='guestEmail']", "guest-address@example.test"), select(".checkout-form select", "Минск · 1–2 дн."), disabled("button:has-text('Оплатить')")],
)

# --- Auth and registration (20) ---------------------------------------------
sc("SC-176", "auth", "Вход с неверным паролем", "Ошибка", [goto("/api/auth/login?returnTo=/catalog"), fill("input[name='username']", "qa.customer@plantmarket.local"), fill("input[name='password']", "WrongPassword123!"), click("button[type='submit']"), wait_text("Неверный e-mail или пароль")])
sc("SC-177", "auth", "Вход с верным паролем", "Выйти", [goto("/api/auth/login?returnTo=/catalog"), fill("input[name='username']", "qa.customer@plantmarket.local"), fill("input[name='password']", "QaCustomer123!"), click("button[type='submit']"), wait_text("Выйти")])
sc("SC-178", "auth", "Выход из storefront", "Войти", [goto("/api/auth/login?returnTo=/catalog"), fill("input[name='username']", "qa.customer@plantmarket.local"), fill("input[name='password']", "QaCustomer123!"), click("button[type='submit']"), wait_text("Выйти"), click("button.nav-button"), wait_url("/catalog"), wait_text("Войти")])
sc("SC-179", "auth", "Сессия переживает reload", "Выйти после reload", [goto("/api/auth/login?returnTo=/catalog"), fill("input[name='username']", "qa.customer@plantmarket.local"), fill("input[name='password']", "QaCustomer123!"), click("button[type='submit']"), wait_text("Выйти"), {"action": "reload"}, wait_text("Выйти")])
sc("SC-180", "auth", "Login содержит CSRF и форму", "CSRF и поля", [goto("/api/auth/login?returnTo=/catalog"), count("input[name='_csrf']", 1), visible("input[name='username']", "input[name='password']")])
sc("SC-181", "auth", "Login содержит ссылку регистрации", "Ссылка «Зарегистрироваться»", [goto("/api/auth/login?returnTo=/catalog"), visible("a[href='/register']")])
sc("SC-182", "auth", "Защищённые заказы просят вход", "Войдите", [goto("/account/orders"), text("Войдите, чтобы увидеть заказы")])
sc("SC-183", "auth", "Login из checkout возвращает к заказу", "Оформление заказа", [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel(".checkout-login"), click(".checkout-login"), fill("input[name='username']", "qa.customer@plantmarket.local"), fill("input[name='password']", "QaCustomer123!"), click("button[type='submit']"), wait_text("Оформление заказа")])
sc("SC-184", "auth", "Админ входит как operator", "Продукты", [goto_admin("/"), fill("input[type='email']", "qa.operator@plantmarket.local"), fill("input[type='password']", "QaOperator123!"), click("button[type='submit']"), wait_text("Продукты")])
sc("SC-185", "auth", "Админ входит как manager", "Продукты", [goto_admin("/"), fill("input[type='email']", "qa.manager@plantmarket.local"), fill("input[type='password']", "QaManager123!"), click("button[type='submit']"), wait_text("Продукты")])
sc("SC-186", "auth", "Админ выходит", "Админка", [tab("Продукты"), click("button:has-text('Выйти')"), wait_text("Админка")], auth="admin")
sc("SC-187", "auth", "Админская сессия переживает reload", "Продукты", [{"action": "reload"}, wait_text("Продукты")], auth="admin")
sc("SC-188", "auth", "Неверный пароль админа", "Invalid email or password", [goto_admin("/"), fill("input[type='email']", "admin@plantmarket.local"), fill("input[type='password']", "WrongPassword123!"), click("button[type='submit']"), wait_text("Invalid email or password")])
sc("SC-189", "auth", "Ссылка регистрации открывает страницу", "Регистрация", [goto("/api/auth/login?returnTo=/catalog"), click("a[href='/register']"), wait_text("Регистрация")])
sc("SC-190", "auth", "Регистрация с паролем 6 символов", "Аккаунт создан", [goto("/api/auth/login?returnTo=/catalog"), click("a[href='/register']"), wait_text("Регистрация"), fill("input[name='fullName']", "QA Six"), fill("input[name='email']", "qa-six-{uid}@example.test"), fill("input[name='password']", "Qa1234"), fill("input[name='confirmPassword']", "Qa1234"), click("button[type='submit']"), wait_text("Аккаунт создан. Войдите.")])
sc("SC-191", "auth", "Регистрация отклоняет разные пароли", "Пароли не совпадают", [goto("/api/auth/login?returnTo=/catalog"), click("a[href='/register']"), wait_text("Регистрация"), fill("input[name='fullName']", "QA Mismatch"), fill("input[name='email']", "qa-mismatch-{uid}@example.test"), fill("input[name='password']", "Qa1234"), fill("input[name='confirmPassword']", "Qa5678"), click("button[type='submit']"), wait_text("Пароли не совпадают.")])
sc("SC-192", "auth", "Регистрация отклоняет короткий пароль", "Поле :invalid", [goto("/api/auth/login?returnTo=/catalog"), click("a[href='/register']"), wait_text("Регистрация"), fill("input[name='fullName']", "QA Short"), fill("input[name='email']", "qa-short-{uid}@example.test"), fill("input[name='password']", "Qa123"), fill("input[name='confirmPassword']", "Qa123"), click("button[type='submit']"), visible("input[name='password']:invalid")])
sc("SC-193", "auth", "Регистрация отклоняет занятый e-mail", "Этот e-mail уже зарегистрирован", [api("registerCustomer", "other"), goto("/api/auth/login?returnTo=/catalog"), click("a[href='/register']"), wait_text("Регистрация"), fill("input[name='fullName']", "QA Duplicate"), fill("input[name='email']", "{other.email}"), fill("input[name='password']", "Qa1234"), fill("input[name='confirmPassword']", "Qa1234"), click("button[type='submit']"), wait_text("Этот e-mail уже зарегистрирован.")])
sc("SC-194", "auth", "Регистрация с невалидным e-mail", "Поле :invalid", [goto("/api/auth/login?returnTo=/catalog"), click("a[href='/register']"), wait_text("Регистрация"), fill("input[name='fullName']", "QA Bad Email"), fill("input[name='email']", "bad-email"), fill("input[name='password']", "Qa1234"), fill("input[name='confirmPassword']", "Qa1234"), click("button[type='submit']"), visible("input[name='email']:invalid")])
sc("SC-195", "auth", "Login без аккаунта", "Ссылка регистрации", [goto("/api/auth/login?returnTo=/catalog"), text("Нет аккаунта?")])

# --- Admin CRUD and operations (30) -----------------------------------------
sc("SC-196", "admin", "Создание категории с описанием", "Категория создана", [tab("Категории"), fill("input[name='name']", "SC Cat {uid}"), fill("input[name='slug']", "sc-cat-{uid}"), fill("textarea[name='description']", "Описание"), click("button:has-text('Создать')"), wait_text("Категория создана")], auth="admin")
sc("SC-197", "admin", "Редактирование категории", "Категория обновлена", [api("createCategory", "category", name="SC Edit Cat {uid}", slug="sc-edit-cat-{uid}"), {"action": "reload"}, tab("Категории"), click("tr:has-text('{category.slug}') button:has-text('Редактировать')"), fill("input[name='name']", "SC Updated {uid}"), click("button:has-text('Сохранить категорию')"), wait_text("Категория обновлена")], auth="admin")
sc("SC-198", "admin", "Удаление категории", "Категория удалена", [api("createCategory", "category", name="SC Del Cat {uid}", slug="sc-del-cat-{uid}"), {"action": "reload"}, tab("Категории"), click("tr:has-text('{category.slug}') button:has-text('Удалить')"), wait_text("Категория удалена")], auth="admin")
sc("SC-199", "admin", "Создание продукта со всеми полями", "Продукт создан", [tab("Продукты"), fill("input[name='sku']", "SC-ALL-{uid}"), fill("input[name='name']", "SC All {uid}"), fill("input[name='slug']", "sc-all-{uid}"), select("select[name='categoryId']", "Монстеры"), fill("input[name='price']", "99"), select("select[name='light']", "BRIGHT"), select("select[name='careLevel']", "HARD"), check("input[name='variegated']"), check("input[name='petSafe']"), fill("textarea[name='description']", "SC description"), click("button:has-text('Создать продукт')"), wait_text("Продукт создан")], auth="admin")
sc("SC-200", "admin", "Редактирование названия продукта", "Продукт обновлён", [api("createProduct", "product", sku="SC-NAME-{uid}", name="SC Name {uid}", slug="sc-name-{uid}"), {"action": "reload"}, click("tr:has-text('{product.sku}') button:has-text('Редактировать')"), fill("input[name='name']", "SC Renamed {uid}"), click("button:has-text('Сохранить продукт')"), wait_text("Продукт обновлён")], auth="admin")
sc("SC-201", "admin", "Архивация продукта", "Продукт архивирован", [api("createProduct", "product", sku="SC-ARC2-{uid}", name="SC Arc2 {uid}", slug="sc-arc2-{uid}"), {"action": "reload"}, click("tr:has-text('{product.sku}') button:has-text('Архивировать')"), wait_text("Продукт архивирован")], auth="admin")
sc("SC-202", "admin", "Создание экземпляра", "Экземпляр создан", [tab("Экземпляры"), select("select[name='productId']", "Philodendron Pink Princess"), fill("input[name='code']", "SC-IP2-{uid}"), fill("input[name='price']", "121"), click("button:has-text('Создать экземпляр')"), wait_text("Экземпляр создан (ACQUIRED)")], auth="admin")
sc("SC-203", "admin", "Карантин экземпляра", "Переведён в карантин", [plant_fixture(sold_out=True), {"action": "reload"}, tab("Экземпляры"), click("tr:has-text('{plant.code}') button:has-text('В карантин')"), wait_text("Переведён в карантин")], auth="admin")
sc("SC-204", "admin", "Публикация экземпляра", "Опубликован", [plant_fixture(sold_out=True), {"action": "reload"}, tab("Экземпляры"), click("tr:has-text('{plant.code}') button:has-text('В карантин')"), wait_text("Переведён в карантин"), click("tr:has-text('{plant.code}') button:has-text('Опубликовать')"), wait_text("Опубликован")], auth="admin")
sc("SC-205", "admin", "Создание варианта", "Вариант создан", [tab("Варианты"), select("select[name='productId']", "Monstera Deliciosa"), fill("input[name='sku']", "SC-V2-{uid}"), fill("input[name='name']", "12 cm pot"), fill("input[name='price']", "151"), click("button:has-text('Создать вариант')"), wait_text("Вариант создан")], auth="admin")
sc("SC-206", "admin", "Редактирование варианта", "Вариант обновлён", [*variant_fixture(15000), {"action": "reload"}, tab("Варианты"), select("select[name='productId']", "{product.name}"), click("tr:has-text('{variant.sku}') button:has-text('Редактировать')"), fill("input[name='price']", "176"), click("button:has-text('Сохранить вариант')"), wait_text("Вариант обновлён")], auth="admin")
sc("SC-207", "admin", "Архивация варианта", "Вариант архивирован", [*variant_fixture(), {"action": "reload"}, tab("Варианты"), select("select[name='productId']", "{product.name}"), click("tr:has-text('{variant.sku}') button:has-text('Архивировать')"), wait_text("Вариант архивирован")], auth="admin")
sc("SC-208", "admin", "Создание партии", "Партия создана", [tab("Партии"), select("select", "Monstera Deliciosa"), select("select[name='productVariantId']", "MON-001-12 · 12 cm pot"), fill("input[name='supplier']", "SC Supplier 2"), fill("input[name='quantity']", "4"), fill("input[name='unitCost']", "41"), click("button:has-text('Создать партию')"), wait_text("Партия создана (QUARANTINE)")], auth="admin")
sc("SC-209", "admin", "Выпуск партии", "Партия опубликована", [tab("Партии"), select("select", "Monstera Deliciosa"), select("select[name='productVariantId']", "MON-001-12 · 12 cm pot"), fill("input[name='supplier']", "SC Supplier 3"), fill("input[name='quantity']", "3"), fill("input[name='unitCost']", "42"), click("button:has-text('Создать партию')"), wait_text("Партия создана (QUARANTINE)"), click("tr:has-text('SC Supplier 3') button:has-text('Выпустить')"), wait_text("Партия опубликована")], auth="admin")
sc("SC-210", "admin", "Все вкладки открываются", "6 вкладок", [count(".tab", 6), tab("Категории"), wait_text("Категории"), tab("Экземпляры"), wait_text("Экземпляры"), tab("Варианты"), wait_text("Варианты продукта"), tab("Партии"), wait_text("Новая партия"), tab("Претензии"), wait_text("Претензии")], auth="admin")
sc("SC-211", "admin", "Обновление админки", "Продукты", [{"action": "reload"}, wait_text("Продукты")], auth="admin")
sc("SC-212", "admin", "Бейдж статуса продукта", "Бейдж", [wait_sel(".badge"), visible(".badge")], auth="admin")
sc("SC-213", "admin", "Hover строки таблицы", "Тёмный фон", [wait_sel("tbody tr"), hover("tbody tr"), {"action": "expectCss", "selector": "tbody tr", "property": "background-color", "value": "rgb(23, 40, 31)"}], auth="admin")
sc("SC-214", "admin", "Галерея сохраняет URL", "Галерея сохранена", [plant_fixture(), {"action": "reload"}, tab("Экземпляры"), click("tr:has-text('{plant.code}') button:has-text('Редактировать')"), wait_text("Галерея WYSIWYG"), fill("input[placeholder='https://…']", "https://example.test/sc-general2.jpg"), click("button:has-text('Сохранить галерею')"), wait_text("Галерея сохранена")], auth="admin")
sc("SC-215", "admin", "Галерея загружает файл", "Фото загружено", [plant_fixture(), {"action": "reload"}, tab("Экземпляры"), click("tr:has-text('{plant.code}') button:has-text('Редактировать')"), wait_text("Галерея WYSIWYG"), upload("input[type='file']", "../../clients/storefront/public/images/plants/leaf.jpg"), wait_text("Фото загружено — не забудьте сохранить галерею")], auth="admin")
sc("SC-216", "admin", "Пустая галерея не сохраняется", "Ошибка", [plant_fixture(), {"action": "reload"}, tab("Экземпляры"), click("tr:has-text('{plant.code}') button:has-text('Редактировать')"), wait_text("Галерея WYSIWYG"), click("button:has-text('Сохранить галерею')"), wait_sel(".form-message")], auth="admin")
sc("SC-217", "admin", "Оператор подтверждает замену", "REPLACEMENT", [*paid_order(), api("createClaim", "claim", orderId="{order.id}", reason="SC replacement 2"), tab("Претензии"), wait_sel("tr:has-text('{claim.short}')"), click("tr:has-text('{claim.short}') button:has-text('Решить')"), wait_text("REPLACEMENT")], auth="admin")
sc("SC-218", "admin", "Оператор выбирает возврат", "REFUND", [*paid_order(), api("createClaim", "claim", orderId="{order.id}", reason="SC refund 2"), tab("Претензии"), wait_sel("tr:has-text('{claim.short}')"), select("tr:has-text('{claim.short}') select", "Возврат"), click("tr:has-text('{claim.short}') button:has-text('Решить')"), wait_text("REFUND")], auth="admin")
sc("SC-219", "admin", "Оператор отклоняет претензию", "REJECT", [*paid_order(), api("createClaim", "claim", orderId="{order.id}", reason="SC reject 2"), tab("Претензии"), wait_sel("tr:has-text('{claim.short}')"), select("tr:has-text('{claim.short}') select", "Отказ"), click("tr:has-text('{claim.short}') button:has-text('Решить')"), wait_text("REJECT")], auth="admin")
sc("SC-220", "admin", "Таблица претензий показывает фото", "фото 1", [*paid_order(), api("createClaim", "claim", orderId="{order.id}", reason="SC photo 2"), tab("Претензии"), wait_sel("tr:has-text('{claim.short}')"), visible("tr:has-text('{claim.short}') a:has-text('фото 1')")], auth="admin")
sc("SC-221", "admin", "Дубликат SKU показывает ошибку", "Ошибка", [tab("Продукты"), fill("input[name='sku']", "MON-001"), fill("input[name='name']", "SC Dup"), fill("input[name='slug']", "sc-dup-{uid}"), select("select[name='categoryId']", "Монстеры"), fill("input[name='price']", "10"), click("button:has-text('Создать продукт')"), wait_sel(".error")], auth="admin")
sc("SC-222", "admin", "Форма продукта содержит все поля", "Поля", [text("SKU", "Название", "Slug", "Категория", "Цена, BYN", "Размер", "Статус", "Свет", "Уход", "Описание")], auth="admin")
sc("SC-223", "admin", "Форма экземпляра содержит поля", "Поля", [tab("Экземпляры"), text("Продукт", "Код", "Цена, BYN", "Свет", "Уход", "Описание", "Фото URL")], auth="admin")
sc("SC-224", "admin", "Форма варианта содержит поля", "Поля", [tab("Варианты"), text("Продукт", "SKU", "Название", "Размер", "Горшок", "Форма", "Цена, BYN", "Статус")], auth="admin")
sc("SC-225", "admin", "Форма партии содержит поля", "Поля", [tab("Партии"), text("Продукт", "Вариант", "Поставщик", "Количество", "Себестоимость, BYN")], auth="admin")

# --- Consent, SEO, responsive, a11y (25) ------------------------------------
sc("SC-226", "consent", "Баннер cookie показывается", "Баннер", [goto("/catalog"), wait_sel(".cookie-banner"), visible(".cookie-banner")])
sc("SC-227", "consent", "Отказ скрывает баннер", "Баннер скрыт", [goto("/catalog"), wait_sel(".cookie-banner"), click(".cookie-banner button:has-text('Только необходимые')"), hidden(".cookie-banner")])
sc("SC-228", "consent", "Аналитика сохраняется", "pm.consent.v1=all", [goto("/catalog"), wait_sel(".cookie-banner"), click(".cookie-banner button:has-text('Принять аналитику')"), storage("pm.consent.v1", "all")])
sc("SC-229", "consent", "Выбор consent переживает reload", "Баннер скрыт после reload", [goto("/catalog"), wait_sel(".cookie-banner"), click(".cookie-banner button:has-text('Только необходимые')"), {"action": "reload"}, hidden(".cookie-banner")])
sc("SC-230", "seo", "robots.txt", "Sitemap", [goto("/robots.txt"), text("Sitemap")])
sc("SC-231", "seo", "sitemap.xml", "/catalog", [goto("/sitemap.xml"), text("/catalog")])
sc("SC-232", "seo", "Canonical каталога", "/catalog", [goto("/catalog"), attr("link[rel='canonical']", "href", "/catalog", "contains")])
sc("SC-233", "seo", "Meta description каталога", "nonEmpty", [goto("/catalog"), attr("meta[name='description']", "content", "", "nonEmpty")])
sc("SC-234", "seo", "JSON-LD экземпляра", "1 JSON-LD", [plant_fixture(), goto("/plants/{plant.id}"), count("script[type='application/ld+json']", 1)])
sc("SC-235", "seo", "OG title каталога", "nonEmpty", [goto("/catalog"), attr("meta[property='og:title']", "content", "", "nonEmpty")])
sc("SC-236", "responsive", "Каталог mobile", "Нет overflow", [goto("/catalog"), overflow()], viewport=[390, 844])
sc("SC-237", "responsive", "Экземпляр mobile", "Нет overflow", [plant_fixture(), goto("/plants/{plant.id}"), overflow()], viewport=[390, 844])
sc("SC-238", "responsive", "Корзина mobile", "Нет overflow", [goto("/cart"), overflow()], viewport=[390, 844])
sc("SC-239", "responsive", "Checkout mobile", "Нет overflow", [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel(".checkout-form"), overflow()], auth="customer", viewport=[390, 844])
sc("SC-240", "responsive", "Login mobile", "Нет overflow", [goto("/api/auth/login?returnTo=/catalog"), overflow()], viewport=[390, 844])
sc("SC-241", "responsive", "Register mobile", "Нет overflow", [goto("/register"), overflow()], viewport=[390, 844])
sc("SC-242", "responsive", "Admin login mobile", "Нет overflow", [goto_admin("/"), overflow()], viewport=[390, 844])
sc("SC-243", "responsive", "Legal mobile", "Нет overflow", [goto("/legal/cookies"), overflow()], viewport=[390, 844])
sc("SC-244", "a11y", "A11y каталога", "OK", [goto("/catalog"), a11y()])
sc("SC-245", "a11y", "A11y login", "OK", [goto("/api/auth/login?returnTo=/catalog"), a11y()])
sc("SC-246", "a11y", "A11y register", "OK", [goto("/register"), a11y()])
sc("SC-247", "a11y", "A11y checkout", "OK", [plant_fixture(), cart_plant(), goto("/checkout"), wait_sel(".checkout-form"), a11y()], auth="customer")
sc("SC-248", "a11y", "A11y admin login", "OK", [goto_admin("/"), a11y()])
sc("SC-249", "a11y", "Keyboard focus каталога", "A", [goto("/catalog"), press("body", "Tab"), {"action": "expectActiveTag", "value": "A"}])
sc("SC-250", "a11y", "Keyboard focus login", "INPUT", [goto("/api/auth/login?returnTo=/catalog"), press("body", "Tab"), {"action": "expectActiveTag", "value": "INPUT"}])

# =============================================================================
# I. 100 combined journeys — SC-251 .. SC-350
# =============================================================================

# --- 20 customer purchase journeys ------------------------------------------
for i in range(20):
    idx = 251 + i
    auth = "guest" if i % 2 == 0 else "customer"
    item = "plant" if i % 4 < 2 else "variant"
    zone = "GOMEL" if i % 5 == 0 else "MINSK"
    thermal = zone == "GOMEL"
    two = i % 6 == 0
    price = 12000 if i % 3 else 15000
    steps: list = []
    if item == "plant" and two:
        steps += [
            plant_fixture(price, store="plantA"),
            plant_fixture(price + 3000, store="plantB"),
            {
                "action": "setCart",
                "items": [
                    {"type": "INDIVIDUAL_PLANT", "id": "{plantA.id}", "name": "{plantA.productName} ({plantA.code})", "priceMinor": price, "currency": "BYN", "quantity": 1},
                    {"type": "INDIVIDUAL_PLANT", "id": "{plantB.id}", "name": "{plantB.productName} ({plantB.code})", "priceMinor": price + 3000, "currency": "BYN", "quantity": 1},
                ],
            },
        ]
    elif item == "plant":
        steps += [
            plant_fixture(price),
            goto("/plants/{plant.id}"),
            click("button:has-text('В корзину')"),
            wait_text("В корзине"),
            click("header a[href='/cart']"),
            wait_url("/cart"),
        ]
    else:
        steps += variant_fixture(price)
        steps += [
            goto("/catalog/{product.id}"),
            click(".variant-card button"),
            wait_text("Корзина (1)"),
        ]
    steps += [goto("/checkout"), wait_text("Оформление заказа"), {"action": "reload"}]
    if auth == "guest":
        steps.append(fill("input[name='guestEmail']", f"journey-{idx}@example.test"))
    steps += [
        fill(".checkout-form textarea", "Минск, Независимости 1"),
        select(".checkout-form select", "Гомель (зима) · 2–4 дн." if thermal else "Минск · 1–2 дн."),
    ]
    if thermal:
        steps.append(check(".checkout-thermal input"))
    steps += [click("button:has-text('Оплатить')"), wait_text("Заказ оплачен")]
    if auth == "customer":
        steps += [goto("/account/orders"), wait_text("PAID")]
    sc(
        f"SC-{idx}",
        "journey",
        f"Покупка: {auth}, {item}, {zone}, {'2 товара' if two else '1 товар'}",
        "Заказ оплачен" + (" и виден в истории" if auth == "customer" else ""),
        steps,
        auth=auth,
    )

# --- 20 admin lifecycle journeys --------------------------------------------
for i in range(20):
    idx = 271 + i
    cat = f"Journey Cat {idx}-{{uid}}"
    prod = f"Journey Product {idx}-{{uid}}"
    code = f"J-IP-{idx}-{{uid}}"
    sku = f"J-V-{idx}-{{uid}}"
    supplier = f"J Supplier {idx}-{{uid}}"
    steps = [
        tab("Категории"),
        fill("input[name='name']", cat),
        fill("input[name='slug']", f"journey-cat-{idx}-{{uid}}"),
        click("button:has-text('Создать')"),
        wait_text("Категория создана"),
        {"action": "reload"},
        tab("Продукты"),
        fill("input[name='sku']", f"J-P-{idx}-{{uid}}"),
        fill("input[name='name']", prod),
        fill("input[name='slug']", f"journey-p-{idx}-{{uid}}"),
        select("select[name='categoryId']", cat),
        fill("input[name='price']", "100"),
        click("button:has-text('Создать продукт')"),
        wait_text("Продукт создан"),
        {"action": "reload"},
        tab("Экземпляры"),
        select("select[name='productId']", prod),
        fill("input[name='code']", code),
        fill("input[name='price']", "120"),
        click("button:has-text('Создать экземпляр')"),
        wait_text("Экземпляр создан (ACQUIRED)"),
        {"action": "reload"},
        tab("Экземпляры"),
        click(f"tr:has-text('{code}') button:has-text('В карантин')"),
        wait_text("Переведён в карантин"),
        click(f"tr:has-text('{code}') button:has-text('Опубликовать')"),
        wait_text("Опубликован"),
        tab("Варианты"),
        select("select[name='productId']", prod),
        fill("input[name='sku']", sku),
        fill("input[name='name']", "12 cm pot"),
        fill("input[name='price']", "150"),
        click("button:has-text('Создать вариант')"),
        wait_text("Вариант создан"),
        {"action": "reload"},
        tab("Партии"),
        select("select", prod),
        select("select[name='productVariantId']", f"{sku} · 12 cm pot"),
        fill("input[name='supplier']", supplier),
        fill("input[name='quantity']", "5"),
        fill("input[name='unitCost']", "40"),
        click("button:has-text('Создать партию')"),
        wait_text("Партия создана (QUARANTINE)"),
    ]
    if i % 4 == 0:
        steps += [click(f"tr:has-text('{supplier}') button:has-text('Выпустить')"), wait_text("Партия опубликована")]
    elif i % 4 == 1:
        steps += [
            tab("Экземпляры"),
            click(f"tr:has-text('{code}') button:has-text('Редактировать')"),
            fill("input[name='price']", "121"),
            click("button:has-text('Сохранить экземпляр')"),
            wait_text("Экземпляр обновлён"),
        ]
    elif i % 4 == 2:
        steps += [
            tab("Продукты"),
            click(f"tr:has-text('J-P-{idx}-{{uid}}') button:has-text('Редактировать')"),
            fill("input[name='price']", "101"),
            click("button:has-text('Сохранить продукт')"),
            wait_text("Продукт обновлён"),
        ]
    else:
        steps += [
            tab("Экземпляры"),
            click(f"tr:has-text('{code}') button:has-text('Редактировать')"),
            wait_text("Галерея WYSIWYG"),
            fill("input[placeholder='https://…']", f"https://example.test/j-{idx}.jpg"),
            click("button:has-text('Сохранить галерею')"),
            wait_text("Галерея сохранена"),
        ]
    steps += [tab("Продукты"), wait_text("Продукты")]
    sc(
        f"SC-{idx}",
        "journey",
        f"Админ-цикл: категория → продукт → экземпляр → вариант → партия ({idx})",
        "Все сущности созданы и видны в таблицах",
        steps,
        auth="admin",
    )

# --- 20 auth/account journeys -----------------------------------------------
for i in range(20):
    idx = 291 + i
    if i < 5:
        email = f"journey-reg-{idx}-{{uid}}@example.test"
        steps = [
            goto("/api/auth/login?returnTo=/catalog"),
            click("a[href='/register']"),
            wait_text("Регистрация"),
            fill("input[name='fullName']", f"Journey User {idx}"),
            fill("input[name='email']", email),
            fill("input[name='password']", "Qa1234"),
            fill("input[name='confirmPassword']", "Qa1234"),
            click("button[type='submit']"),
            wait_text("Аккаунт создан. Войдите."),
        ]
        steps += [
            fill("input[name='password']", "Qa1234"),
            click("button[type='submit']"),
            wait_text("Выйти"),
            click("button.nav-button"),
            wait_url("/catalog"),
            wait_text("Войти"),
        ]
        name = f"Регистрация → вход → выход ({idx})"
    elif i < 10:
        email = f"journey-val-{idx}-{{uid}}@example.test"
        steps = [
            goto("/api/auth/login?returnTo=/catalog"),
            click("a[href='/register']"),
            wait_text("Регистрация"),
            fill("input[name='fullName']", f"Journey Val {idx}"),
            fill("input[name='email']", email),
            fill("input[name='password']", "Qa1234"),
            fill("input[name='confirmPassword']", "Qa9999" if i % 2 == 0 else "Qa1234"),
            click("button[type='submit']"),
        ]
        if i % 2 == 0:
            steps.append(wait_text("Пароли не совпадают."))
        else:
            steps.append(wait_text("Аккаунт создан. Войдите."))
        name = f"Валидация регистрации ({idx})"
    elif i < 15:
        steps = [
            goto("/api/auth/login?returnTo=/catalog"),
            fill("input[name='username']", "qa.customer@plantmarket.local"),
            fill("input[name='password']", "WrongPassword123!" if i % 2 == 0 else "QaCustomer123!"),
            click("button[type='submit']"),
            wait_text("Неверный e-mail или пароль" if i % 2 == 0 else "Выйти"),
        ]
        if i % 2 == 1:
            steps += [{"action": "reload"}, wait_text("Выйти")]
        name = f"Вход и сессия ({idx})"
    else:
        operator = i % 2 == 0
        steps = [
            goto_admin("/"),
            fill("input[type='email']", "qa.operator@plantmarket.local" if operator else "qa.manager@plantmarket.local"),
            fill("input[type='password']", "QaOperator123!" if operator else "QaManager123!"),
            click("button[type='submit']"),
            wait_text("Продукты"),
            click("button:has-text('Претензии')"),
            wait_text("Претензии"),
            click("button:has-text('Выйти')"),
            wait_text("Админка"),
        ]
        name = f"Админ-роль {('operator' if operator else 'manager')} ({idx})"
    sc(f"SC-{idx}", "journey", name, "Сценарий входа/аккаунта завершён", steps)

# --- 20 restock/claim/gallery/stock journeys --------------------------------
for i in range(20):
    idx = 311 + i
    if i < 8:
        steps = [
            plant_fixture(sold_out=True),
            goto("/plants/{plant.id}"),
            fill("#restock-email", f"journey-restock-{idx}-{{uid}}@example.test"),
            click("button:has-text('Сообщить о поступлении')"),
            wait_text(f"Готово: напишем на journey-restock-{idx}-{{uid}}@example.test"),
        ]
        name = f"Restock подписка ({idx})"
    elif i < 12:
        steps = [
            plant_fixture(sold_out=True),
            goto("/plants/{plant.id}"),
            fill("#restock-email", "bad"),
            click("button:has-text('Сообщить о поступлении')"),
            visible("#restock-email:invalid"),
        ]
        name = f"Restock невалидный e-mail ({idx})"
    elif i < 16:
        decision = ["REPLACEMENT", "REFUND", "REJECT", "REPLACEMENT"][i % 4]
        label = {"REPLACEMENT": "Замена", "REFUND": "Возврат", "REJECT": "Отказ"}[decision]
        steps = [
            *paid_order(),
            api("createClaim", "claim", orderId="{order.id}", reason=f"Journey claim {idx}"),
            tab("Претензии"),
            wait_sel("tr:has-text('{claim.short}')"),
        ]
        if decision != "REPLACEMENT":
            steps.append(select("tr:has-text('{claim.short}') select", label))
        steps += [
            click("tr:has-text('{claim.short}') button:has-text('Решить')"),
            wait_text(decision),
        ]
        name = f"Претензия: {decision} ({idx})"
    else:
        steps = [
            plant_fixture(),
            {"action": "reload"},
            tab("Экземпляры"),
            click("tr:has-text('{plant.code}') button:has-text('Редактировать')"),
            wait_text("Галерея WYSIWYG"),
            upload("input[type='file']", "../../clients/storefront/public/images/plants/leaf.jpg"),
            wait_text("Фото загружено — не забудьте сохранить галерею"),
            click("button:has-text('Сохранить галерею')"),
            wait_text("Галерея сохранена"),
        ]
        name = f"Галерея: загрузка файла ({idx})"
    sc(
        f"SC-{idx}",
        "journey",
        name,
        "Сценарий restock/claim/gallery завершён",
        steps,
        auth="admin" if i >= 12 else "none",
    )

# --- 20 consent/SEO/mobile/a11y journeys ------------------------------------
for i in range(20):
    idx = 331 + i
    if i < 5:
        steps = [
            goto("/catalog"),
            wait_sel(".cookie-banner"),
            click(".cookie-banner button:has-text('Принять аналитику')" if i % 2 == 0 else ".cookie-banner button:has-text('Только необходимые')"),
            storage("pm.consent.v1", "all" if i % 2 == 0 else "essential"),
            {"action": "reload"},
            hidden(".cookie-banner"),
            goto("/catalog?variegated=true"),
            wait_text("Найдено"),
        ]
        name = f"Consent + фильтр ({idx})"
    elif i < 10:
        paths = ["/robots.txt", "/sitemap.xml", "/catalog", "/legal/cookies", "/catalog"]
        checks = [
            [text("Sitemap")],
            [text("/catalog")],
            [attr("link[rel='canonical']", "href", "/catalog", "contains")],
            [text("Cookie и аналитика")],
            [attr("meta[property='og:title']", "content", "", "nonEmpty")],
        ]
        steps = [goto(paths[i % 5])] + checks[i % 5]
        name = f"SEO/legal страница ({idx})"
    elif i < 15:
        pages = [
            ("/catalog", None),
            ("/plants/{plant.id}", "plant"),
            ("/checkout", "checkout"),
            ("/register", None),
            ("/", "admin"),
        ]
        path, kind = pages[i % 5]
        steps = []
        auth = "none"
        if kind == "plant":
            steps.append(plant_fixture())
        if kind == "checkout":
            steps += [plant_fixture(), cart_plant()]
            auth = "customer"
        steps.append(goto_admin("/") if kind == "admin" else goto(path))
        if kind == "checkout":
            steps.append(wait_sel(".checkout-form"))
        steps.append(overflow())
        name = f"Mobile overflow: {path} ({idx})"
    else:
        pages = ["/catalog", "/api/auth/login?returnTo=/catalog", "/register", "/checkout", "/"]
        path = pages[i % 5]
        steps = []
        auth = "none"
        if path == "/checkout":
            steps += [plant_fixture(), cart_plant()]
            auth = "customer"
        steps.append(goto_admin("/") if path == "/" else goto(path))
        if path == "/checkout":
            steps.append(wait_sel(".checkout-form"))
        steps.append(a11y())
        name = f"A11y: {path} ({idx})"
    sc(
        f"SC-{idx}",
        "journey",
        name,
        "Сценарий consent/SEO/mobile/a11y завершён",
        steps,
        auth=auth if i >= 10 else "none",
        viewport=[390, 844] if 10 <= i < 15 else None,
    )

def main() -> None:
    if len(SCENARIOS) != 350:
        raise SystemExit(f"Expected 350 scenarios, got {len(SCENARIOS)}")
    ids = [entry["id"] for entry in SCENARIOS]
    if len(set(ids)) != len(ids):
        raise SystemExit("Duplicate scenario ids")

    root = Path(__file__).resolve().parent.parent
    json_path = root / "tests/e2e/src/test/resources/user-scenarios.json"
    json_path.parent.mkdir(parents=True, exist_ok=True)
    json_path.write_text(
        json.dumps({"scenarios": SCENARIOS}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    lines = [
        "# 350 пользовательских E2E-сценариев",
        "",
        "Single source of truth: `scripts/generate-user-scenarios.py`.",
        "Данные: `tests/e2e/src/test/resources/user-scenarios.json`.",
        "Раннер: `tests/e2e/src/test/java/com/plantmarket/e2e/UserScenarioE2ETest.java`.",
        "",
        "| ID | Область | Сценарий | Ожидаемый результат | Auth |",
        "| --- | --- | --- | --- | --- |",
    ]
    for entry in SCENARIOS:
        lines.append(
            f"| {entry['id']} | {entry['area']} | {entry['name']} | "
            f"{entry['expected']} | {entry['auth']} |"
        )
    lines.append("")
    (root / "docs/e2e-scenarios.md").write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {len(SCENARIOS)} scenarios to {json_path} and docs/e2e-scenarios.md")


if __name__ == "__main__":
    main()
