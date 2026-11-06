#!/usr/bin/env python3
"""Single source of truth for the 100-case UI matrix.

Generates:
  tests/e2e/src/test/resources/ui-cases.json  (executed by UiMatrixE2ETest)
  docs/test-cases.md                          (human-readable catalog)

Run from the repo root: python3 scripts/generate-ui-cases.py
"""

from __future__ import annotations

import json
from pathlib import Path

CASES: list[dict] = []


def case(
    case_id: str,
    area: str,
    name: str,
    start: str,
    steps: list[dict],
    expected: str,
    *,
    admin: bool = False,
    viewport: list[int] | None = None,
    storage: dict[str, str] | None = None,
) -> None:
    entry: dict = {
        "id": case_id,
        "area": area,
        "name": name,
        "start": start,
        "steps": steps,
        "expected": expected,
    }
    if admin:
        entry["admin"] = True
    if viewport:
        entry["viewport"] = viewport
    if storage:
        entry["storage"] = storage
    CASES.append(entry)


def text(*values: str) -> list[dict]:
    return [{"action": "expectText", "value": value} for value in values]


def count(selector: str, value: int) -> list[dict]:
    return [{"action": "expectCount", "selector": selector, "value": value}]


def min_count(selector: str, value: int) -> list[dict]:
    return [{"action": "expectMinCount", "selector": selector, "value": value}]


def visible(*selectors: str) -> list[dict]:
    return [{"action": "expectVisible", "selector": selector} for selector in selectors]


def attr(selector: str, name: str, value: str, match: str = "equals") -> list[dict]:
    return [
        {
            "action": "expectAttribute",
            "selector": selector,
            "name": name,
            "value": value,
            "match": match,
        }
    ]


def css(selector: str, prop: str, value: str) -> list[dict]:
    return [{"action": "expectCss", "selector": selector, "property": prop, "value": value}]


def url(value: str) -> list[dict]:
    return [{"action": "expectUrl", "value": value}]


def title(value: str) -> list[dict]:
    return [{"action": "expectTitle", "value": value}]


def status(value: int) -> list[dict]:
    return [{"action": "expectStatus", "value": value}]


def gone(value: str) -> list[dict]:
    return [{"action": "expectTextGone", "value": value}]


def overflow() -> list[dict]:
    return [{"action": "expectNoOverflow"}]


def a11y() -> list[dict]:
    return [{"action": "expectA11y"}]


def storage(key: str, value: str) -> list[dict]:
    return [{"action": "expectStorage", "key": key, "value": value}]


# --- Catalog (TC-001 .. TC-018) -------------------------------------------------
case("TC-001", "catalog", "Каталог отвечает 200", "/catalog", status(200), "HTTP 200")
case(
    "TC-002",
    "catalog",
    "Hero каталога показывает заголовок",
    "/catalog",
    text("Растения, которые не повторить."),
    "Заголовок hero виден",
)
case(
    "TC-003",
    "catalog",
    "Каталог показывает не меньше 11 карточек растений",
    "/catalog",
    min_count(".plant-card", 11),
    ">= 11 карточек",
)
case(
    "TC-004",
    "catalog",
    "Индекс позиций показывает не меньше 11 строк",
    "/catalog",
    min_count(".product-row", 11),
    ">= 11 позиций",
)
case(
    "TC-005",
    "catalog",
    "Фильтр «Только вариегатные» сужает каталог",
    "/catalog?variegated=true",
    url("variegated=true") + min_count(".plant-card", 6),
    "Параметр в URL, >= 6 карточек",
)
case(
    "TC-006",
    "catalog",
    "Фильтр по среднему свету сужает каталог",
    "/catalog?variegated=true&light=MEDIUM",
    url("light=MEDIUM") + min_count(".plant-card", 3),
    "Параметр в URL, >= 3 карточки",
)
case(
    "TC-007",
    "catalog",
    "Фильтр по цене до 150 BYN сужает каталог",
    "/catalog?variegated=true&light=MEDIUM&maxPrice=150",
    url("maxPrice=150") + min_count(".plant-card", 1),
    "Параметр в URL, >= 1 карточка",
)
case(
    "TC-008",
    "catalog",
    "Сброс фильтров возвращает полный каталог",
    "/catalog?variegated=true&light=MEDIUM&maxPrice=150",
    [
        {"action": "click", "selector": "button:has-text('Сбросить фильтры')"},
        {"action": "waitForFunction", "value": "() => location.search === ''"},
    ]
    + attr("button:has-text('Любой')", "aria-pressed", "true")
    + min_count(".plant-card", 11),
    "Фильтры сброшены, >= 11 карточек",
)
case(
    "TC-009",
    "catalog",
    "Пилюля «Любой» активна по умолчанию",
    "/catalog",
    attr("button:has-text('Любой')", "aria-pressed", "true"),
    "aria-pressed=true",
)
case(
    "TC-010",
    "catalog",
    "Тумблер вариегатности выключен по умолчанию",
    "/catalog",
    attr(".filter-toggle", "aria-pressed", "false"),
    "aria-pressed=false",
)
case(
    "TC-011",
    "catalog",
    "Пустой результат фильтра показывает подсказку",
    "/catalog?maxPrice=1",
    text("Под эти фильтры растений нет"),
    "Текст пустого состояния",
)
case(
    "TC-012",
    "catalog",
    "Каждая карточка растения имеет бейдж WYSIWYG",
    "/catalog",
    min_count(".photo-badge", 11),
    ">= 11 бейджей",
)
case("TC-013", "catalog", "Карточка IP-001 видна в каталоге", "/catalog", text("IP-001"), "Код IP-001")
case(
    "TC-014",
    "catalog",
    "Карточка IP-001 показывает цену",
    "/catalog",
    text("120,00"),
    "Цена 120,00 BYN",
)
case(
    "TC-015",
    "catalog",
    "Карточка растения ведёт на страницу экземпляра",
    "/catalog",
    visible("a[href^='/plants/']"),
    "Ссылка /plants/...",
)
case(
    "TC-016",
    "catalog",
    "Строка индекса ведёт на страницу продукта",
    "/catalog",
    visible(".product-row h2 a[href^='/catalog/']"),
    "Ссылка /catalog/...",
)
case(
    "TC-017",
    "catalog",
    "Hero показывает общий остаток, а не ноль под фильтром",
    "/catalog?variegated=true&light=MEDIUM&maxPrice=150",
    text("РАСТЕНИЙ В НАЛИЧИИ") + gone("0 РАСТЕНИЙ В НАЛИЧИИ"),
    "Нет «0 растений в наличии»",
)
case(
    "TC-018",
    "catalog",
    "Meta title каталога задан",
    "/catalog",
    title("Каталог растений"),
    "Title содержит «Каталог растений»",
)

# --- Plant detail (TC-019 .. TC-030) --------------------------------------------
case("TC-019", "plant", "Страница экземпляра отвечает 200", "/plants/{plantId}", status(200), "HTTP 200")
case(
    "TC-020",
    "plant",
    "Страница экземпляра показывает название",
    "/plants/{plantId}",
    text("{plantName}"),
    "Название растения",
)
case(
    "TC-021",
    "plant",
    "Страница экземпляра показывает цену",
    "/plants/{plantId}",
    text("{plantPrice}"),
    "Цена экземпляра",
)
case(
    "TC-022",
    "plant",
    "Бейдж WYSIWYG на странице экземпляра",
    "/plants/{plantId}",
    visible(".wysiwyg-badge"),
    "Бейдж виден",
)
case(
    "TC-023",
    "plant",
    "Главное фото галереи отображается",
    "/plants/{plantId}",
    visible(".gallery-main img"),
    "Фото видно",
)
case(
    "TC-024",
    "plant",
    "Галерея содержит минимум один ракурс",
    "/plants/{plantId}",
    min_count(".gallery-thumb", 1),
    ">= 1 миниатюры",
)
case(
    "TC-025",
    "plant",
    "Блок доверия содержит три пункта",
    "/plants/{plantId}",
    count(".trust-list li", 3),
    "3 пункта",
)
case(
    "TC-026",
    "plant",
    "Кнопка «В корзину» доступна",
    "/plants/{plantId}",
    visible("button:has-text('В корзину')"),
    "Кнопка видна",
)
case(
    "TC-027",
    "plant",
    "Кнопка «Купить сейчас» доступна",
    "/plants/{plantId}",
    visible("button:has-text('Купить сейчас')"),
    "Кнопка видна",
)
case(
    "TC-028",
    "plant",
    "Ссылка «Назад в каталог» доступна",
    "/plants/{plantId}",
    visible(".back-link"),
    "Ссылка видна",
)
case(
    "TC-029",
    "plant",
    "Страница экземпляра содержит JSON-LD",
    "/plants/{plantId}",
    count("script[type='application/ld+json']", 1),
    "1 JSON-LD блок",
)
case(
    "TC-030",
    "plant",
    "Meta title содержит название растения",
    "/plants/{plantId}",
    title("{plantName}"),
    "Title с названием",
)

# --- Product detail (TC-031 .. TC-036) -----------------------------------------
case(
    "TC-031",
    "product",
    "Страница продукта отвечает 200",
    "/catalog/{productId}",
    status(200),
    "HTTP 200",
)
case(
    "TC-032",
    "product",
    "Страница продукта показывает название",
    "/catalog/{productId}",
    text("Monstera Deliciosa"),
    "Название продукта",
)
case(
    "TC-033",
    "product",
    "Страница продукта показывает цену",
    "/catalog/{productId}",
    text("299,00"),
    "Цена продукта",
)
case(
    "TC-034",
    "product",
    "Секция вариантов отображается",
    "/catalog/{productId}",
    text("Варианты"),
    "Заголовок «Варианты»",
)
case(
    "TC-035",
    "product",
    "У продукта есть минимум один вариант",
    "/catalog/{productId}",
    min_count(".variant-card", 1),
    ">= 1 вариант",
)
case(
    "TC-036",
    "product",
    "Главное фото продукта отображается",
    "/catalog/{productId}",
    visible(".product-hero img"),
    "Фото видно",
)

# --- Cart (TC-037 .. TC-041) ---------------------------------------------------
case("TC-037", "cart", "Пустая корзина показывает заголовок", "/cart", text("Корзина"), "Заголовок")
case(
    "TC-038",
    "cart",
    "Пустая корзина предлагает перейти в каталог",
    "/cart",
    visible(".empty a[href='/catalog']"),
    "Ссылка в каталог",
)
case(
    "TC-039",
    "cart",
    "Ссылка на корзину есть в шапке",
    "/cart",
    visible("header a[href='/cart']"),
    "Ссылка /cart",
)
case("TC-040", "cart", "Meta title корзины задан", "/cart", title("Корзина"), "Title содержит «Корзина»")
case(
    "TC-041",
    "cart",
    "Корзина не имеет горизонтального скролла на десктопе",
    "/cart",
    overflow(),
    "scrollWidth <= clientWidth",
)

# --- Checkout (TC-042 .. TC-045) ------------------------------------------------
case(
    "TC-042",
    "checkout",
    "Пустой checkout показывает заголовок",
    "/checkout",
    text("Оформление заказа"),
    "Заголовок",
)
case(
    "TC-043",
    "checkout",
    "Пустой checkout предлагает каталог",
    "/checkout",
    visible(".empty a[href='/catalog']"),
    "Ссылка в каталог",
)
case(
    "TC-044",
    "checkout",
    "Checkout с товаром требует вход",
    "/checkout",
    text("Нужно войти"),
    "Экран «Нужно войти»",
    storage={
        "pm.cart.v1": (
            '[{"type":"INDIVIDUAL_PLANT","id":"{plantId}","name":"{plantName}",'
            '"priceMinor":12000,"currency":"BYN","quantity":1}]'
        )
    },
)
case(
    "TC-045",
    "checkout",
    "Meta title checkout задан",
    "/checkout",
    title("Оформление заказа"),
    "Title содержит «Оформление заказа»",
)

# --- Auth (TC-046 .. TC-055) ----------------------------------------------------
case(
    "TC-046",
    "auth",
    "Storefront login редиректит на auth-service",
    "/api/auth/login?returnTo=/account/orders",
    url(":8090/login"),
    "URL /login на auth-service",
)
case(
    "TC-047",
    "auth",
    "Брендированный login имеет title",
    "/api/auth/login?returnTo=/account/orders",
    title("Вход — Plant Market"),
    "Title «Вход — Plant Market»",
)
case(
    "TC-048",
    "auth",
    "Брендированный login показывает заголовок «Вход»",
    "/api/auth/login?returnTo=/account/orders",
    text("Вход", "ЛИЧНЫЙ КАБИНЕТ"),
    "Заголовок и eyebrow",
)
case(
    "TC-049",
    "auth",
    "Login показывает бренд Plant Market",
    "/api/auth/login?returnTo=/account/orders",
    text("Plant Market"),
    "Бренд",
)
case(
    "TC-050",
    "auth",
    "Login содержит поле E-mail",
    "/api/auth/login?returnTo=/account/orders",
    text("E-MAIL") + visible("input[name='username']"),
    "Поле username",
)
case(
    "TC-051",
    "auth",
    "Login содержит поле Пароль",
    "/api/auth/login?returnTo=/account/orders",
    text("ПАРОЛЬ") + visible("input[name='password']"),
    "Поле password",
)
case(
    "TC-052",
    "auth",
    "Login содержит CSRF-токен",
    "/api/auth/login?returnTo=/account/orders",
    count("input[name='_csrf']", 1),
    "1 hidden _csrf",
)
case(
    "TC-053",
    "auth",
    "Неверный пароль показывает ошибку",
    "/api/auth/login?returnTo=/account/orders",
    [
        {"action": "fill", "selector": "input[name='username']", "value": "qa.customer@plantmarket.local"},
        {"action": "fill", "selector": "input[name='password']", "value": "WrongPassword123!"},
        {"action": "click", "selector": "button[type='submit']"},
        {"action": "waitText", "value": "Неверный e-mail или пароль"},
    ],
    "Сообщение об ошибке",
)
case(
    "TC-054",
    "auth",
    "Верный пароль возвращает в личный кабинет",
    "/api/auth/login?returnTo=/account/orders",
    [
        {"action": "fill", "selector": "input[name='username']", "value": "qa.customer@plantmarket.local"},
        {"action": "fill", "selector": "input[name='password']", "value": "QaCustomer123!"},
        {"action": "click", "selector": "button[type='submit']"},
        {"action": "waitText", "value": "Мои заказы"},
    ],
    "Страница «Мои заказы»",
)
case(
    "TC-055",
    "auth",
    "Выход возвращает в каталог",
    "/api/auth/login?returnTo=/catalog",
    [
        {"action": "fill", "selector": "input[name='username']", "value": "qa.customer@plantmarket.local"},
        {"action": "fill", "selector": "input[name='password']", "value": "QaCustomer123!"},
        {"action": "click", "selector": "button[type='submit']"},
        {"action": "waitText", "value": "Выйти"},
        {"action": "click", "selector": "button.nav-button"},
        {"action": "waitUrl", "value": "/catalog"},
        {"action": "waitText", "value": "Войти"},
    ],
    "URL /catalog и ссылка «Войти»",
)

# --- Legal, SEO, consent (TC-056 .. TC-065) -------------------------------------
case(
    "TC-056",
    "legal",
    "Страница cookie открывается",
    "/legal/cookies",
    text("Cookie и аналитика"),
    "Заголовок",
)
case(
    "TC-057",
    "legal",
    "Страница cookie показывает статус черновика",
    "/legal/cookies",
    text("Черновик"),
    "Плашка «Черновик»",
)
case(
    "TC-058",
    "consent",
    "Cookie-баннер показывается при первом визите",
    "/catalog",
    visible(".cookie-banner") + text("Используем необходимые cookie"),
    "Баннер виден",
)
case(
    "TC-059",
    "consent",
    "Отказ от аналитики скрывает баннер",
    "/catalog",
    [
        {"action": "click", "selector": ".cookie-banner button:has-text('Только необходимые')"},
        {"action": "expectHidden", "selector": ".cookie-banner"},
    ],
    "Баннер скрыт",
)
case(
    "TC-060",
    "consent",
    "Принятие аналитики сохраняет выбор",
    "/catalog",
    [
        {"action": "click", "selector": ".cookie-banner button:has-text('Принять аналитику')"},
        {"action": "expectStorage", "key": "pm.consent.v1", "value": "all"},
    ],
    "localStorage pm.consent.v1=all",
)
case(
    "TC-061",
    "seo",
    "robots.txt содержит Sitemap",
    "/robots.txt",
    status(200) + text("Sitemap"),
    "HTTP 200 + Sitemap",
)
case(
    "TC-062",
    "seo",
    "sitemap.xml содержит /catalog",
    "/sitemap.xml",
    status(200) + text("/catalog"),
    "HTTP 200 + URL",
)
case(
    "TC-063",
    "seo",
    "Каталог содержит canonical",
    "/catalog",
    attr("link[rel='canonical']", "href", "/catalog", "contains"),
    "canonical на /catalog",
)
case(
    "TC-064",
    "seo",
    "Каталог содержит meta description",
    "/catalog",
    attr("meta[name='description']", "content", "", "nonEmpty"),
    "meta description присутствует",
)
case(
    "TC-065",
    "seo",
    "Футер содержит копирайт",
    "/catalog",
    text("© 2026 Plant Market"),
    "Копирайт",
)

# --- Admin (TC-066 .. TC-093) ---------------------------------------------------
case("TC-066", "admin", "Экран входа админки показывает заголовок", "/", text("Админка"), "Заголовок")
case("TC-067", "admin", "Экран входа админки содержит Email", "/", text("Email"), "Label Email")
case("TC-068", "admin", "Экран входа админки содержит Пароль", "/", text("Пароль"), "Label Пароль")
case(
    "TC-069",
    "admin",
    "Экран входа админки содержит кнопку Войти",
    "/",
    visible("button[type='submit']"),
    "Кнопка Войти",
)
case(
    "TC-070",
    "admin",
    "Неверный пароль админки показывает ошибку",
    "/",
    [
        {"action": "fill", "selector": "input[type='email']", "value": "admin@plantmarket.local"},
        {"action": "fill", "selector": "input[type='password']", "value": "WrongPassword123!"},
        {"action": "click", "selector": "button[type='submit']"},
        {"action": "waitText", "value": "Invalid email or password"},
    ],
    "Текст ошибки auth-service",
)
case(
    "TC-071",
    "admin",
    "Вход администратора открывает продукты",
    "/",
    [
        {"action": "fill", "selector": "input[type='email']", "value": "admin@plantmarket.local"},
        {"action": "fill", "selector": "input[type='password']", "value": "AdminPassword123!"},
        {"action": "click", "selector": "button[type='submit']"},
        {"action": "waitText", "value": "Продукты"},
    ],
    "Раздел «Продукты»",
)
case(
    "TC-072",
    "admin",
    "Таблица продуктов заполнена",
    "/",
    [{"action": "waitSelector", "selector": "tbody tr"}] + min_count("tbody tr", 1),
    ">= 1 строка",
    admin=True,
)
case(
    "TC-073",
    "admin",
    "Вкладка категорий открывается",
    "/",
    [{"action": "adminTab", "value": "Категории"}, {"action": "waitText", "value": "Категории"}, {"action": "waitSelector", "selector": "tbody tr"}, {"action": "expectMinCount", "selector": "tbody tr", "value": 1}],
    "Таблица категорий",
    admin=True,
)
case(
    "TC-074",
    "admin",
    "Вкладка экземпляров открывается",
    "/",
    [{"action": "adminTab", "value": "Экземпляры"}, {"action": "waitText", "value": "Экземпляры"}, {"action": "waitSelector", "selector": "tbody tr"}, {"action": "expectMinCount", "selector": "tbody tr", "value": 1}],
    "Таблица экземпляров",
    admin=True,
)
case(
    "TC-075",
    "admin",
    "Вкладка вариантов открывается",
    "/",
    [
        {"action": "adminTab", "value": "Варианты"},
        {"action": "waitText", "value": "Варианты продукта"},
        {"action": "select", "selector": "select[name='productId']", "value": "Monstera Deliciosa"},
        {"action": "waitSelector", "selector": "tbody tr"},
        {"action": "expectMinCount", "selector": "tbody tr", "value": 1},
    ],
    "Таблица вариантов",
    admin=True,
)
case(
    "TC-076",
    "admin",
    "Вкладка партий открывается",
    "/",
    [{"action": "adminTab", "value": "Партии"}, {"action": "waitText", "value": "Новая партия"}, {"action": "waitSelector", "selector": "tbody tr"}, {"action": "expectMinCount", "selector": "tbody tr", "value": 1}],
    "Таблица партий",
    admin=True,
)
case(
    "TC-077",
    "admin",
    "Вкладка претензий открывается",
    "/",
    [
        {"action": "adminTab", "value": "Претензии"},
        {"action": "waitText", "value": "Претензии"},
        {"action": "expectText", "value": "Решение оператора"},
    ],
    "Таблица претензий",
    admin=True,
)
case("TC-078", "admin", "В админке шесть вкладок", "/", count(".tab", 6), "6 вкладок", admin=True)
case(
    "TC-079",
    "admin",
    "Форма нового продукта отображается",
    "/",
    text("Новый продукт"),
    "Заголовок формы",
    admin=True,
)
case(
    "TC-080",
    "admin",
    "Форма продукта содержит SKU",
    "/",
    text("SKU") + visible("input[name='sku']"),
    "Поле SKU",
    admin=True,
)
case(
    "TC-081",
    "admin",
    "Форма продукта содержит название",
    "/",
    text("Название") + visible("input[name='name']"),
    "Поле name",
    admin=True,
)
case(
    "TC-082",
    "admin",
    "Форма продукта содержит slug",
    "/",
    text("Slug") + visible("input[name='slug']"),
    "Поле slug",
    admin=True,
)
case(
    "TC-083",
    "admin",
    "Форма продукта содержит категорию",
    "/",
    text("Категория") + visible("select[name='categoryId']"),
    "Селект категории",
    admin=True,
)
case(
    "TC-084",
    "admin",
    "Форма продукта содержит цену",
    "/",
    text("Цена, BYN") + visible("input[name='price']"),
    "Поле цены",
    admin=True,
)
case(
    "TC-085",
    "admin",
    "Форма продукта содержит размер",
    "/",
    text("Размер") + visible("input[name='size']"),
    "Поле размера",
    admin=True,
)
case(
    "TC-086",
    "admin",
    "Форма продукта содержит статус",
    "/",
    text("Статус") + visible("select[name='status']"),
    "Селект статуса",
    admin=True,
)
case(
    "TC-087",
    "admin",
    "Форма продукта содержит свет",
    "/",
    text("Свет") + visible("select[name='light']"),
    "Селект света",
    admin=True,
)
case(
    "TC-088",
    "admin",
    "Форма продукта содержит уход",
    "/",
    text("Уход") + visible("select[name='careLevel']"),
    "Селект ухода",
    admin=True,
)
case(
    "TC-089",
    "admin",
    "Форма продукта содержит описание",
    "/",
    text("Описание") + visible("textarea[name='description']"),
    "Поле описания",
    admin=True,
)
case(
    "TC-090",
    "admin",
    "В таблице продуктов есть бейдж статуса",
    "/",
    [{"action": "waitSelector", "selector": ".badge"}] + visible(".badge"),
    "Бейдж статуса",
    admin=True,
)
case(
    "TC-091",
    "admin",
    "Верхняя панель содержит Обновить и Выйти",
    "/",
    visible("button:has-text('Обновить')", "button:has-text('Выйти')"),
    "Две кнопки",
    admin=True,
)
case(
    "TC-092",
    "admin",
    "Строка таблицы при hover остаётся тёмной",
    "/",
    [{"action": "waitSelector", "selector": "tbody tr"}, {"action": "hover", "selector": "tbody tr"}, {"action": "expectCss", "selector": "tbody tr", "property": "background-color", "value": "rgb(23, 40, 31)"}],
    "background-color surface-2",
    admin=True,
)
case(
    "TC-093",
    "admin",
    "Выход из админки возвращает на экран входа",
    "/",
    [{"action": "click", "selector": "button:has-text('Выйти')"}, {"action": "waitText", "value": "Админка"}],
    "Экран входа",
    admin=True,
)

# --- Responsive and a11y (TC-094 .. TC-100) -------------------------------------
mobile = [390, 844]
case("TC-094", "responsive", "Каталог без горизонтального скролла на мобильном", "/catalog", overflow(), "Нет overflow", viewport=mobile)
case("TC-095", "responsive", "Карточка растения без горизонтального скролла на мобильном", "/plants/{plantId}", overflow(), "Нет overflow", viewport=mobile)
case("TC-096", "responsive", "Корзина без горизонтального скролла на мобильном", "/cart", overflow(), "Нет overflow", viewport=mobile)
case("TC-097", "responsive", "Юридическая страница без горизонтального скролла на мобильном", "/legal/cookies", overflow(), "Нет overflow", viewport=mobile)
case("TC-098", "responsive", "Экран входа без горизонтального скролла на мобильном", "/api/auth/login?returnTo=/catalog", overflow(), "Нет overflow", viewport=mobile)
case("TC-099", "responsive", "Экран входа админки без горизонтального скролла на мобильном", "/", overflow(), "Нет overflow", viewport=mobile)
case(
    "TC-100",
    "a11y",
    "A11y-смоук каталога: alt, labels, h1, lang",
    "/catalog",
    a11y(),
    "Все проверки пройдены",
)


def main() -> None:
    if len(CASES) != 100:
        raise SystemExit(f"Expected 100 cases, got {len(CASES)}")
    ids = [entry["id"] for entry in CASES]
    if len(set(ids)) != len(ids):
        raise SystemExit("Duplicate case ids")

    root = Path(__file__).resolve().parent.parent
    json_path = root / "tests/e2e/src/test/resources/ui-cases.json"
    json_path.parent.mkdir(parents=True, exist_ok=True)
    json_path.write_text(
        json.dumps({"cases": CASES}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    lines = [
        "# UI test cases — 100 automated checks",
        "",
        "Single source of truth: `scripts/generate-ui-cases.py`.",
        "Generated data: `tests/e2e/src/test/resources/ui-cases.json`.",
        "Runner: `tests/e2e/src/test/java/com/plantmarket/e2e/UiMatrixE2ETest.java`.",
        "",
        "| ID | Area | Test case | Expected |",
        "| --- | --- | --- | --- |",
    ]
    for entry in CASES:
        lines.append(
            f"| {entry['id']} | {entry['area']} | {entry['name']} | {entry['expected']} |"
        )
    lines.append("")
    (root / "docs/test-cases.md").write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {len(CASES)} cases to {json_path} and docs/test-cases.md")


if __name__ == "__main__":
    main()
