package com.plantmarket.gateway.config;

import com.plantmarket.common.security.SecretPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String environment) {

  public JwtProperties {
    SecretPolicy.validateJwtSecret(secret, environment);
  }
}
