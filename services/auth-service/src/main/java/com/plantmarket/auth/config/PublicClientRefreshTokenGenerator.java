package com.plantmarket.auth.config;

import java.time.Instant;
import java.util.Base64;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * Spring Authorization Server does not issue refresh tokens to public clients by default. Phase 1
 * needs rotation for the PKCE storefront/admin clients, so this generator applies the registered
 * client's refresh TTL without the public-client restriction.
 */
public final class PublicClientRefreshTokenGenerator
    implements OAuth2TokenGenerator<OAuth2RefreshToken> {

  private final StringKeyGenerator tokenGenerator =
      new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

  @Override
  public OAuth2RefreshToken generate(OAuth2TokenContext context) {
    if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
      return null;
    }
    Instant issuedAt = Instant.now();
    Instant expiresAt =
        issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
    return new OAuth2RefreshToken(tokenGenerator.generateKey(), issuedAt, expiresAt);
  }
}
