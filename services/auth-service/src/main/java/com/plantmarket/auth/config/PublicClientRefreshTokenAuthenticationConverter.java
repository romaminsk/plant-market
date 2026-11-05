package com.plantmarket.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

/**
 * Spring Authorization Server's public-client converter only matches the PKCE authorization-code
 * exchange. Public clients also need refresh-token rotation, so this converter authenticates
 * refresh_token requests by client_id when no client secret is present.
 */
public final class PublicClientRefreshTokenAuthenticationConverter
    implements AuthenticationConverter {

  @Override
  public Authentication convert(HttpServletRequest request) {
    if (!"POST".equals(request.getMethod())
        || !request.getRequestURI().endsWith("/oauth2/token")
        || !"refresh_token".equals(request.getParameter("grant_type"))
        || StringUtils.hasText(request.getParameter("client_secret"))) {
      return null;
    }
    String clientId = request.getParameter("client_id");
    if (!StringUtils.hasText(clientId)) {
      return null;
    }
    return new OAuth2ClientAuthenticationToken(
        clientId,
        ClientAuthenticationMethod.NONE,
        null,
        java.util.Map.of("grant_type", "refresh_token"));
  }
}
