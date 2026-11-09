package com.plantmarket.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.payment.client.PaymentProviderClient;
import com.plantmarket.payment.client.ProviderPayment;
import com.plantmarket.payment.domain.PaymentEntity;
import com.plantmarket.payment.domain.PaymentStatus;
import com.plantmarket.payment.repository.PaymentRepository;
import com.plantmarket.payment.service.WebhookSignature;
import com.plantmarket.payment.web.dto.CreatePaymentRequest;
import com.plantmarket.payment.web.dto.PaymentResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class PaymentFlowIntegrationTest {

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");

  @BeforeAll
  static void startDatabase() {
    POSTGRES.start();
  }

  @AfterAll
  static void stopDatabase() {
    POSTGRES.stop();
  }

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private PaymentRepository paymentRepository;

  @Test
  void refundsConfirmedPaymentIdempotently() {
    UUID orderId = UUID.randomUUID();
    PaymentResponse created =
        restTemplate
            .postForEntity(
                "/api/v1/payments",
                new CreatePaymentRequest(orderId, 5000L, "BYN"),
                PaymentResponse.class)
            .getBody();
    assertThat(created).isNotNull();

    ResponseEntity<PaymentResponse> first =
        restTemplate.postForEntity(
            "/api/v1/payments/" + created.id() + "/refund", null, PaymentResponse.class);
    ResponseEntity<PaymentResponse> second =
        restTemplate.postForEntity(
            "/api/v1/payments/" + created.id() + "/refund", null, PaymentResponse.class);

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(first.getBody()).isNotNull();
    assertThat(first.getBody().status()).isEqualTo(PaymentStatus.REFUNDED);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getBody()).isNotNull();
    assertThat(second.getBody().status()).isEqualTo(PaymentStatus.REFUNDED);
  }

  @Test
  void rejectsRefundForNonConfirmedPayment() {
    PaymentEntity failed =
        paymentRepository.saveAndFlush(
            new PaymentEntity(
                UUID.randomUUID(),
                1000L,
                "BYN",
                PaymentStatus.FAILED,
                "failed-" + UUID.randomUUID(),
                null));

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/payments/" + failed.getId() + "/refund", null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void creatingPaymentTwiceForSameOrderReturnsSamePayment() {
    UUID orderId = UUID.randomUUID();
    ResponseEntity<PaymentResponse> first =
        restTemplate.postForEntity(
            "/api/v1/payments",
            new CreatePaymentRequest(orderId, 129900L, "BYN"),
            PaymentResponse.class);
    ResponseEntity<PaymentResponse> second =
        restTemplate.postForEntity(
            "/api/v1/payments",
            new CreatePaymentRequest(orderId, 129900L, "BYN"),
            PaymentResponse.class);

    assertThat(first.getBody()).isNotNull();
    assertThat(second.getBody()).isNotNull();
    assertThat(second.getBody().id()).isEqualTo(first.getBody().id());
  }

  @Test
  void createsPaymentAndAcceptsSignedWebhook() {
    UUID orderId = UUID.randomUUID();
    ResponseEntity<PaymentResponse> created =
        restTemplate.postForEntity(
            "/api/v1/payments",
            new CreatePaymentRequest(orderId, 129900L, "BYN"),
            PaymentResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    assertThat(created.getBody().status().name()).isEqualTo("CONFIRMED");
    String providerRef = created.getBody().providerRef();
    assertThat(providerRef).isNotBlank();

    String body = webhookBody(providerRef, "REFUNDED");
    ResponseEntity<PaymentResponse> webhook =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook",
            new HttpEntity<>(body, webhookHeaders(body, "evt-" + UUID.randomUUID(), nowSeconds())),
            PaymentResponse.class);
    assertThat(webhook.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(webhook.getBody()).isNotNull();
    assertThat(webhook.getBody().status().name()).isEqualTo("REFUNDED");

    String nextBody = webhookBody(providerRef, "FAILED");
    ResponseEntity<String> regression =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook",
            new HttpEntity<>(
                nextBody, webhookHeaders(nextBody, "evt-" + UUID.randomUUID(), nowSeconds())),
            String.class);
    assertThat(regression.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void rejectsWebhookWithInvalidSignature() {
    String providerRef = createPayment();
    String body = webhookBody(providerRef, "REFUNDED");
    HttpHeaders headers = webhookHeaders(body, "evt-" + UUID.randomUUID(), nowSeconds());
    headers.set(
        "X-Provider-Signature",
        WebhookSignature.compute("wrong-secret", "1700000000", "evt-x", body));

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook", new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsWebhookWithStaleTimestamp() {
    String providerRef = createPayment();
    String body = webhookBody(providerRef, "REFUNDED");
    long stale = nowSeconds() - 3600;

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook",
            new HttpEntity<>(body, webhookHeaders(body, "evt-" + UUID.randomUUID(), stale)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void duplicateWebhookEventIsIdempotent() {
    String providerRef = createPayment();
    String body = webhookBody(providerRef, "REFUNDED");
    String eventId = "evt-" + UUID.randomUUID();
    long timestamp = nowSeconds();
    HttpHeaders headers = webhookHeaders(body, eventId, timestamp);

    ResponseEntity<PaymentResponse> first =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook", new HttpEntity<>(body, headers), PaymentResponse.class);
    ResponseEntity<PaymentResponse> second =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook", new HttpEntity<>(body, headers), PaymentResponse.class);

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getBody()).isNotNull();
    assertThat(second.getBody().status()).isEqualTo(PaymentStatus.REFUNDED);
  }

  private String createPayment() {
    ResponseEntity<PaymentResponse> created =
        restTemplate.postForEntity(
            "/api/v1/payments",
            new CreatePaymentRequest(UUID.randomUUID(), 129900L, "BYN"),
            PaymentResponse.class);
    assertThat(created.getBody()).isNotNull();
    return created.getBody().providerRef();
  }

  private static String webhookBody(String providerRef, String status) {
    return "{\"providerRef\":\"" + providerRef + "\",\"status\":\"" + status + "\"}";
  }

  private static long nowSeconds() {
    return Instant.now().getEpochSecond();
  }

  private static HttpHeaders webhookHeaders(String body, String eventId, long timestamp) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    headers.set("X-Provider-Timestamp", String.valueOf(timestamp));
    headers.set("X-Provider-Event-Id", eventId);
    headers.set(
        "X-Provider-Signature",
        WebhookSignature.compute(
            "dev-payment-webhook-secret", String.valueOf(timestamp), eventId, body));
    return headers;
  }

  @TestConfiguration
  static class ProviderTestConfig {

    @Bean
    @Primary
    PaymentProviderClient paymentProviderClient() {
      return (orderId, amountMinor, currency) ->
          new ProviderPayment(
              "provider-" + UUID.randomUUID(), "AUTHORIZED", "http://localhost:8089/pay/mock");
    }
  }
}
