package com.plantmarket.auth.config;

import com.plantmarket.common.security.SecretPolicy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String issuer, String secret, Duration accessTtl, Duration refreshTtl, String environment) {

  public JwtProperties {
    SecretPolicy.validateJwtSecret(secret, environment);
  }
}
