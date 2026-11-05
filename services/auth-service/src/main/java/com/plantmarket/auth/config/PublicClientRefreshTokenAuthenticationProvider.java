package com.plantmarket.auth.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Authenticates public-client refresh_token requests without a PKCE code_verifier. The converter
 * marks such requests; the default public-client provider assumes authorization_code and requires a
 * verifier, so this provider runs first.
 */
public final class PublicClientRefreshTokenAuthenticationProvider
    implements AuthenticationProvider {

  static final String GRANT_TYPE_PARAMETER = "grant_type";

  private final RegisteredClientRepository registeredClientRepository;

  public PublicClientRefreshTokenAuthenticationProvider(
      RegisteredClientRepository registeredClientRepository) {
    this.registeredClientRepository = registeredClientRepository;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!(authentication instanceof OAuth2ClientAuthenticationToken clientAuthentication)
        || !ClientAuthenticationMethod.NONE.equals(
            clientAuthentication.getClientAuthenticationMethod())
        || !"refresh_token"
            .equals(clientAuthentication.getAdditionalParameters().get(GRANT_TYPE_PARAMETER))) {
      return null;
    }

    String clientId = clientAuthentication.getPrincipal().toString();
    RegisteredClient client = registeredClientRepository.findByClientId(clientId);
    if (client == null
        || !client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
        || !client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_client"));
    }
    return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
