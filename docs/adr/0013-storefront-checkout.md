# ADR-0013: Storefront checkout через BFF и OAuth2 PKCE

- **Статус:** Accepted (MVP buy-now реализован)
- **Дата:** 2026-11-06
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** ADR-0006 (OAuth2 PKCE), ADR-0008 (доставка), Phase 2/3

## Контекст

Storefront умел каталог, карточку WYSIWYG, фильтры и waitlist, но не умел вход покупателя и
оплату. J1 E2E проверял только каталог и карточку. Прятать access token в `localStorage` нельзя
(A11), а OAuth2 Authorization Code + PKCE уже реализован в auth-service (ADR-0006).

## Решение (MVP)

- **BFF в Next.js** (route handlers, server-side):
  - `GET /api/auth/login` генерирует `code_verifier`/`code_challenge`, кладёт verifier в
    httpOnly cookie и редиректит на auth-service `/oauth2/authorize` с `client_id=storefront`.
  - `GET /auth/callback` меняет `code` на токены server-to-server (`/oauth2/token`), кладёт
    access/refresh в httpOnly `__Host-` cookies (Secure в prod) и редиректит на `/checkout`.
  - `POST /api/auth/logout` очищает cookies.
  - `POST /api/checkout` читает access token из cookie, вызывает order-service:
    create → checkout (зона + термоупаковка) → pay; возвращает статус заказа.
  - При 401 BFF обновляет токен через refresh rotation и повторяет запрос; при повторном 401 —
    редирект на логин.
- **Корзина (MVP)**: сценарий «Купить сейчас» — карточка растения ведёт на `/checkout?plantId=…`.
  Мульти-корзина с `localStorage` остаётся следующей итерацией; цены и остатки в любом случае
  валидирует order-service при создании заказа.
- **Страницы**: `/checkout` (зоны, термоупаковка, итог) и `/plants/[id]` с кнопкой «Купить сейчас».
  История заказов и `/order/[id]` — следующая итерация; письмо уже отправляет order-service.
- **CSRF**: double-submit `XSRF-TOKEN` для BFF POST; SameSite=Lax cookies.
- **Redirect URI**: auth-service регистрирует `http://localhost:3000/auth/callback` (dev) и
  `http://localhost:3100/auth/callback` (E2E); prod — `https://plant-market.by/auth/callback`.

## Реализовано

- `GET /api/auth/login` (PKCE verifier/state в httpOnly cookie) и `GET /auth/callback`
  (обмен code → tokens, access/refresh в httpOnly cookie, redirect на `returnTo`).
- `POST /api/checkout`: create → checkout (зона/термоупаковка) → pay через order-service.
- `POST /api/auth/logout`, кнопка «Купить сейчас», `/checkout` с зонами и итогом.
- BFF POST защищён double-submit: `pm.csrf` cookie + `X-CSRF-Token`; в шапке «Войти»/«Выйти».
- E2E `CheckoutFlowE2ETest`: гость → карточка → логин через форму auth-service → checkout → pay →
  «Заказ оплачен».

## Альтернативы

- Токен в `localStorage` + прямые вызовы API из браузера — проще, но нарушает A11 (XSS-риск).
- OAuth2 password grant — запрещён спецификацией и Конституцией.
- Отдельный BFF-сервис — оверинжиниринг для одного фронтенда; route handlers достаточно.

## Последствия

- Access token живёт 10 минут и хранится в httpOnly cookie; refresh пока не используется BFF для
  автоматического обновления — при истечении токена пользователь входит снова.
- CSRF для BFF POST — double-submit `pm.csrf` + `X-CSRF-Token`; cookie `Secure` включается
  переменной `COOKIE_SECURE=true` на HTTPS.
- Next.js route handlers становятся точкой отказа checkout; health/readiness и логи обязательны.
- Мульти-корзина, адреса, слоты доставки и история заказов — после MVP; сейчас один товар на заказ.
- E2E J1 расширен до полного сценария: логин PKCE → checkout → pay → подтверждение.

## Критерии готовности

- ✅ J1 E2E зелёный через UI, включая OAuth2 PKCE и sandbox-оплату.
- ✅ Access/refresh токены в httpOnly cookie, не доступны JavaScript.
- ⏳ Повторный refresh rotation инвалидирует старую цепочку (тест auth-service уже есть);
  BFF-обновление токена — следующая итерация.
- ⏳ p95 checkout через gateway в бюджете A15 на staging.
