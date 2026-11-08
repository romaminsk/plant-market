package com.plantmarket.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecretPolicyTest {

  private static final String STRONG_SECRET = "a-very-long-random-production-secret-0123456789";
  private static final String DEV_SECRET = "dev-only-plant-market-jwt-secret-change-me-0123456789";

  @Test
  void rejectsMissingSecret() {
    assertThatThrownBy(() -> SecretPolicy.validateJwtSecret(null, "dev"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
    assertThatThrownBy(() -> SecretPolicy.validateJwtSecret("", "dev"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsShortSecret() {
    assertThatThrownBy(() -> SecretPolicy.validateJwtSecret("short", "prod"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("32");
  }

  @Test
  void rejectsKnownDevSecretOutsideDev() {
    assertThatThrownBy(() -> SecretPolicy.validateJwtSecret(DEV_SECRET, "prod"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("development");
  }

  @Test
  void acceptsKnownDevSecretInDev() {
    assertThatCode(() -> SecretPolicy.validateJwtSecret(DEV_SECRET, "dev"))
        .doesNotThrowAnyException();
  }

  @Test
  void acceptsStrongSecretInProd() {
    assertThatCode(() -> SecretPolicy.validateJwtSecret(STRONG_SECRET, "prod"))
        .doesNotThrowAnyException();
  }

  @Test
  void validatesWebhookSecret() {
    assertThatThrownBy(
            () -> SecretPolicy.validateWebhookSecret("dev-payment-webhook-secret", "prod"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PAYMENT_WEBHOOK_SECRET");
    assertThatCode(
            () -> SecretPolicy.validateWebhookSecret("a-strong-webhook-secret-0123456789", "prod"))
        .doesNotThrowAnyException();
  }
}
