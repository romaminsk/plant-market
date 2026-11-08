package com.plantmarket.common.security;

/** Startup policy for shared secrets: no blanks, no short values, no dev defaults in prod. */
public final class SecretPolicy {

  private static final int MIN_JWT_LENGTH = 32;
  private static final String DEV_JWT_SECRET =
      "dev-only-plant-market-jwt-secret-change-me-0123456789";
  private static final String DEV_WEBHOOK_SECRET = "dev-payment-webhook-secret";

  private SecretPolicy() {}

  public static void validateJwtSecret(String secret, String environment) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET must be set");
    }
    if (secret.length() < MIN_JWT_LENGTH) {
      throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
    }
    if (!isDev(environment) && DEV_JWT_SECRET.equals(secret)) {
      throw new IllegalStateException(
          "JWT_SECRET uses the development default; set a production secret");
    }
  }

  public static void validateWebhookSecret(String secret, String environment) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("PAYMENT_WEBHOOK_SECRET must be set");
    }
    if (!isDev(environment) && DEV_WEBHOOK_SECRET.equals(secret)) {
      throw new IllegalStateException(
          "PAYMENT_WEBHOOK_SECRET uses the development default; set a production secret");
    }
  }

  private static boolean isDev(String environment) {
    return "dev".equalsIgnoreCase(environment) || "test".equalsIgnoreCase(environment);
  }
}
