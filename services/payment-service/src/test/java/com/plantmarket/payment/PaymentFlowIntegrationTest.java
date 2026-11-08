package com.plantmarket.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.payment.client.PaymentProviderClient;
import com.plantmarket.payment.client.ProviderPayment;
import com.plantmarket.payment.domain.PaymentEntity;
import com.plantmarket.payment.domain.PaymentStatus;
import com.plantmarket.payment.repository.PaymentRepository;
import com.plantmarket.payment.web.dto.CreatePaymentRequest;
import com.plantmarket.payment.web.dto.PaymentResponse;
import com.plantmarket.payment.web.dto.PaymentWebhookRequest;
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

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Provider-Secret", "dev-payment-webhook-secret");
    ResponseEntity<PaymentResponse> webhook =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook",
            new HttpEntity<>(new PaymentWebhookRequest(providerRef, "REFUNDED"), headers),
            PaymentResponse.class);
    assertThat(webhook.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(webhook.getBody()).isNotNull();
    assertThat(webhook.getBody().status().name()).isEqualTo("REFUNDED");

    HttpHeaders wrongHeaders = new HttpHeaders();
    wrongHeaders.set("X-Provider-Secret", "wrong-secret");
    ResponseEntity<String> badSecret =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook",
            new HttpEntity<>(new PaymentWebhookRequest(providerRef, "CONFIRMED"), wrongHeaders),
            String.class);
    assertThat(badSecret.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    HttpHeaders validHeaders = new HttpHeaders();
    validHeaders.set("X-Provider-Secret", "dev-payment-webhook-secret");
    ResponseEntity<String> regression =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook",
            new HttpEntity<>(new PaymentWebhookRequest(providerRef, "FAILED"), validHeaders),
            String.class);
    assertThat(regression.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
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
