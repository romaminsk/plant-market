package com.plantmarket.payment.config;

import com.plantmarket.common.security.SecretPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record PaymentJwtProperties(String secret, String environment) {

  public PaymentJwtProperties {
    SecretPolicy.validateJwtSecret(secret, environment);
  }
}
