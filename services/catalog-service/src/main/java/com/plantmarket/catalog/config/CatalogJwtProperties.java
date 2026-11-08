package com.plantmarket.catalog.config;

import com.plantmarket.common.security.SecretPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record CatalogJwtProperties(String secret, String environment) {

  public CatalogJwtProperties {
    SecretPolicy.validateJwtSecret(secret, environment);
  }
}
