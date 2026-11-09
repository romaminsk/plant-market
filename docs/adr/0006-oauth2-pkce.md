# ADR-0006: OAuth2 Authorization Code + PKCE в Phase 1

- **Статус:** Accepted
- **Дата:** 2026-11-05
- **Решающие:** AI-лид (автономный режим владельца)
- **Связанные документы:** `MASTER_PROMPT.md` A11, ADR-0001, ADR-0004

## Контекст

A11 требует OAuth2 Authorization Code + PKCE, access token на 10 минут и refresh с ротацией.
Storefront и admin — публичные SPA-клиенты: client secret хранить нельзя. Spring Authorization
Server 7.1.2 по умолчанию:

- не выдаёт refresh token публичным клиентам;
- конвертер публичного клиента распознаёт только обмен authorization code с `code_verifier`,
  поэтому refresh-запрос публичного клиента падает с `invalid_grant`.

## Решение

- В `auth-service` подключён `spring-boot-starter-security-oauth2-authorization-server` 7.1.2.
- Зарегистрированы публичные клиенты `storefront` (`http://localhost:3000/auth/callback`) и
  `admin` (`http://localhost:5173/auth/callback`) с `ClientAuthenticationMethod.NONE`,
  `requireProofKey(true)`, grant types `authorization_code` и `refresh_token`, TTL 10 минут / 7 дней.
- Access token — JWT HS256, подписанный тем же секретом, что и password-flow; gateway продолжает
  проверять подпись без изменений.
- `PublicClientRefreshTokenGenerator` выдаёт refresh token публичным PKCE-клиентам.
- `PublicClientRefreshTokenAuthenticationConverter` + `...Provider` аутентифицируют
  `grant_type=refresh_token` по `client_id` без секрета и без `code_verifier`.
- Браузерная сессия: cookie `PM-SESSION` в dev; в prod `__Host-PM-SESSION` + `Secure` +
  `HttpOnly` + `SameSite=Lax` через `SESSION_COOKIE_NAME`/`SESSION_COOKIE_SECURE`.
- CSRF на форме логина — double-submit cookie (`XSRF-TOKEN` + `_csrf`), не HttpOnly по
  определению double-submit; session cookie остаётся HttpOnly.
- Registered clients — in-memory (статическая конфигурация); authorizations и consents —
  `JdbcOAuth2AuthorizationService` в PostgreSQL (`oauth2_authorization`).

## Последствия

- Публичные клиенты получают refresh token и ротацию; повторное использование старого токена
  возвращает `invalid_grant` (проверено `OAuth2PkceIntegrationTest`).
- Authorizations и consents переживают рестарт auth-service и общие для нескольких экземпляров;
  registered clients остаются статической in-memory конфигурацией.
- SAS 7.1.2 на Jackson 3 отклоняет Java 9 immutable collections в JWT claims, поэтому
  `JdbcOAuth2AuthorizationService` использует JsonMapper с разрешённым
  `java.util.ImmutableCollections$`.
- HMAC-секрет общий для auth-service и gateway; секрет должен быть заменён в prod и храниться
  в Vault/SOPS.
- CSRF double-submit и cookie `__Host-` включаются в prod-конфиге; dev использует `PM-SESSION` и
  `XSRF-TOKEN`.

## Проверка

- `OAuth2PkceIntegrationTest`: authorize → login → code → token → refresh → reuse rejected;
  строка авторизации сохранена в `oauth2_authorization`.
- Ручной curl-flow: access token принят gateway на `/api/v1/auth/me` и защищённой мутации каталога.
