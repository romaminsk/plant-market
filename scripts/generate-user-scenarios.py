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
        code="SC-{uid}",
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
    "Комбинация фильтров находит IP-001",
    "Карточка IP-001 и цена 120,00",
    [
        goto("/catalog?variegated=true&light=MEDIUM&maxPrice=150"),
        text("IP-001", "120,00"),
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
    "Страница /catalog/ и секция «Варианты»",
    [
        goto("/catalog"),
        click(".product-row h2 a"),
        wait_url("/catalog/"),
        text("Варианты"),
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
    "Неавторизованный checkout просит войти",
    "Экран «Нужно войти»",
    [plant_fixture(), cart_plant(), goto("/checkout"), wait_text("Нужно войти")],
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
        click(".checkout-result a:has-text('Войти')"),
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
    "Checkout с товаром требует авторизации",
    "«Нужно войти»",
    [plant_fixture(), cart_plant(), goto("/checkout"), wait_text("Нужно войти")],
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


def main() -> None:
    if len(SCENARIOS) != 100:
        raise SystemExit(f"Expected 100 scenarios, got {len(SCENARIOS)}")
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
        "# 100 пользовательских E2E-сценариев",
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
