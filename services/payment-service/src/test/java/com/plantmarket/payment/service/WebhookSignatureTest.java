package com.plantmarket.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebhookSignatureTest {

  private static final String SECRET = "dev-payment-webhook-secret";
  private static final String TIMESTAMP = "1700000000";
  private static final String EVENT_ID = "evt-1";
  private static final String BODY = "{\"providerRef\":\"p-1\",\"status\":\"CONFIRMED\"}";

  @Test
  void computesStableHmacOverTimestampEventIdAndBody() {
    assertThat(WebhookSignature.compute(SECRET, TIMESTAMP, EVENT_ID, BODY))
        .isEqualTo("22e5e8085bf2109189921753bd97b70ea0c1ad77ee7a8c3c19839fb29e3b58b1");
  }

  @Test
  void acceptsOnlyTheExactPayload() {
    String signature = WebhookSignature.compute(SECRET, TIMESTAMP, EVENT_ID, BODY);

    assertThat(WebhookSignature.matches(SECRET, TIMESTAMP, EVENT_ID, BODY, signature)).isTrue();
    assertThat(
            WebhookSignature.matches(
                SECRET, TIMESTAMP, EVENT_ID, BODY.replace("CONFIRMED", "FAILED"), signature))
        .isFalse();
    assertThat(WebhookSignature.matches(SECRET, TIMESTAMP, "evt-2", BODY, signature)).isFalse();
    assertThat(WebhookSignature.matches(SECRET, "1700000001", EVENT_ID, BODY, signature)).isFalse();
    assertThat(WebhookSignature.matches(SECRET, TIMESTAMP, EVENT_ID, BODY, "not-hex")).isFalse();
    assertThat(WebhookSignature.matches(SECRET, TIMESTAMP, EVENT_ID, BODY, null)).isFalse();
  }
}
