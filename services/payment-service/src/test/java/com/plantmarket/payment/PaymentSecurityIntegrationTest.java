package com.plantmarket.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.plantmarket.payment.client.PaymentProviderClient;
import com.plantmarket.payment.client.ProviderPayment;
import com.plantmarket.payment.service.WebhookSignature;
import com.plantmarket.payment.web.dto.CreatePaymentRequest;
import com.plantmarket.payment.web.dto.PaymentResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.security.enabled=true")
@AutoConfigureTestRestTemplate
class PaymentSecurityIntegrationTest {

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

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Test
  void paymentRequiresSystemRole() {
    CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), 129900L, "BYN");

    ResponseEntity<String> anonymous =
        restTemplate.postForEntity("/api/v1/payments", request, String.class);
    assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    ResponseEntity<String> customer =
        restTemplate.postForEntity(
            "/api/v1/payments",
            new HttpEntity<>(request, bearer(List.of("CUSTOMER"))),
            String.class);
    assertThat(customer.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<PaymentResponse> system =
        restTemplate.postForEntity(
            "/api/v1/payments",
            new HttpEntity<>(request, bearer(List.of("SYSTEM"))),
            PaymentResponse.class);
    assertThat(system.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(system.getBody()).isNotNull();
  }

  @Test
  void webhookStaysPublicButVerifiesSignature() {
    UUID orderId = UUID.randomUUID();
    ResponseEntity<PaymentResponse> created =
        restTemplate.postForEntity(
            "/api/v1/payments",
            new HttpEntity<>(
                new CreatePaymentRequest(orderId, 1000L, "BYN"), bearer(List.of("SYSTEM"))),
            PaymentResponse.class);
    assertThat(created.getBody()).isNotNull();
    String providerRef = created.getBody().providerRef();
    String body = "{\"providerRef\":\"" + providerRef + "\",\"status\":\"REFUNDED\"}";
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String eventId = "evt-" + UUID.randomUUID();

    HttpHeaders wrong = new HttpHeaders();
    wrong.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    wrong.set("X-Provider-Timestamp", timestamp);
    wrong.set("X-Provider-Event-Id", eventId);
    wrong.set("X-Provider-Signature", "00".repeat(32));
    ResponseEntity<String> rejected =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook", new HttpEntity<>(body, wrong), String.class);
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    HttpHeaders correct = new HttpHeaders();
    correct.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    correct.set("X-Provider-Timestamp", timestamp);
    correct.set("X-Provider-Event-Id", eventId);
    correct.set(
        "X-Provider-Signature",
        WebhookSignature.compute("dev-payment-webhook-secret", timestamp, eventId, body));
    ResponseEntity<PaymentResponse> accepted =
        restTemplate.postForEntity(
            "/api/v1/payments/webhook", new HttpEntity<>(body, correct), PaymentResponse.class);
    assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(accepted.getBody()).isNotNull();
    assertThat(accepted.getBody().status().name()).isEqualTo("REFUNDED");
  }

  private HttpHeaders bearer(List<String> roles) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token(roles));
    return headers;
  }

  private String token(List<String> roles) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject("security-test-" + UUID.randomUUID())
              .claim("roles", roles)
              .issueTime(new Date())
              .expirationTime(Date.from(Instant.now().plusSeconds(300)))
              .build();
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(jwtSecret));
      return jwt.serialize();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create test token", exception);
    }
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
