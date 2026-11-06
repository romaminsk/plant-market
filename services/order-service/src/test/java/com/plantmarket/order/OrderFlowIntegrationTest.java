package com.plantmarket.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.plantmarket.order.client.CatalogClient;
import com.plantmarket.order.client.CatalogPlant;
import com.plantmarket.order.client.CatalogVariant;
import com.plantmarket.order.client.PaymentClient;
import com.plantmarket.order.client.PaymentResult;
import com.plantmarket.order.client.ReservationRequest;
import com.plantmarket.order.client.ReservationResult;
import com.plantmarket.order.web.dto.CreateOrderItemRequest;
import com.plantmarket.order.web.dto.CreateOrderRequest;
import com.plantmarket.order.web.dto.OrderResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderFlowIntegrationTest {

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
  private static final UUID VARIANT_ID = UUID.randomUUID();
  private static final UUID PLANT_ID = UUID.randomUUID();
  private static final FakeCatalogClient CATALOG = new FakeCatalogClient();
  private static final FakePaymentClient PAYMENT = new FakePaymentClient();

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
  void createCheckoutPayAndCancelOrder() {
    UUID customerId = UUID.randomUUID();
    HttpHeaders headers = headers(customerId);

    ResponseEntity<OrderResponse> created =
        restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(
                new CreateOrderRequest(
                    List.of(
                        new CreateOrderItemRequest(VARIANT_ID, null, 2),
                        new CreateOrderItemRequest(null, PLANT_ID, 1))),
                headers),
            OrderResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    assertThat(created.getBody().status().name()).isEqualTo("DRAFT");
    assertThat(created.getBody().totalMinor()).isEqualTo(32000L);
    UUID orderId = created.getBody().id();

    ResponseEntity<OrderResponse> checkout =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/checkout",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            OrderResponse.class);
    assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(checkout.getBody()).isNotNull();
    assertThat(checkout.getBody().status().name()).isEqualTo("PENDING_PAYMENT");
    assertThat(checkout.getBody().reservationExpiresAt()).isNotNull();
    assertThat(CATALOG.reserveCalls.get()).isEqualTo(1);

    ResponseEntity<OrderResponse> paid =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/pay",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            OrderResponse.class);
    assertThat(paid.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(paid.getBody()).isNotNull();
    assertThat(paid.getBody().status().name()).isEqualTo("PAID");
    assertThat(CATALOG.confirmCalls.get()).isEqualTo(1);
    assertThat(PAYMENT.createCalls.get()).isEqualTo(1);

    ResponseEntity<OrderResponse> second =
        restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(
                new CreateOrderRequest(List.of(new CreateOrderItemRequest(VARIANT_ID, null, 1))),
                headers),
            OrderResponse.class);
    UUID secondId = second.getBody().id();
    restTemplate.exchange(
        "/api/v1/orders/" + secondId + "/checkout",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        OrderResponse.class);
    ResponseEntity<OrderResponse> cancelled =
        restTemplate.exchange(
            "/api/v1/orders/" + secondId + "/cancel",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            OrderResponse.class);
    assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelled.getBody()).isNotNull();
    assertThat(cancelled.getBody().status().name()).isEqualTo("CANCELLED");
    assertThat(CATALOG.releaseCalls.get()).isEqualTo(1);

    ResponseEntity<OrderResponse[]> list =
        restTemplate.exchange(
            "/api/v1/orders", HttpMethod.GET, new HttpEntity<>(headers), OrderResponse[].class);
    assertThat(list.getBody()).hasSize(2);
  }

  private HttpHeaders headers(UUID customerId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token(customerId));
    return headers;
  }

  private String token(UUID customerId) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(customerId.toString())
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
  static class CatalogTestConfig {

    @Bean
    @Primary
    CatalogClient catalogClient() {
      return CATALOG;
    }

    @Bean
    @Primary
    PaymentClient paymentClient() {
      return PAYMENT;
    }
  }

  static final class FakePaymentClient implements PaymentClient {

    final AtomicInteger createCalls = new AtomicInteger();

    @Override
    public PaymentResult createPayment(UUID orderId, long amountMinor, String currency) {
      createCalls.incrementAndGet();
      return new PaymentResult(UUID.randomUUID(), "CONFIRMED");
    }
  }

  static final class FakeCatalogClient implements CatalogClient {

    final AtomicInteger reserveCalls = new AtomicInteger();
    final AtomicInteger releaseCalls = new AtomicInteger();
    final AtomicInteger confirmCalls = new AtomicInteger();

    @Override
    public CatalogVariant getVariant(UUID id) {
      return new CatalogVariant(VARIANT_ID, "VAR-1", "Variant", 10000L, "BYN", "ACTIVE");
    }

    @Override
    public CatalogPlant getPlant(UUID id) {
      return new CatalogPlant(PLANT_ID, "Individual plant", "IP-1", 12000L, "BYN", "AVAILABLE");
    }

    @Override
    public ReservationResult reserve(UUID orderId, List<ReservationRequest.Item> items) {
      reserveCalls.incrementAndGet();
      return new ReservationResult(orderId, List.of(), Instant.now().plusSeconds(900));
    }

    @Override
    public void release(UUID orderId) {
      releaseCalls.incrementAndGet();
    }

    @Override
    public void confirm(UUID orderId) {
      confirmCalls.incrementAndGet();
    }
  }
}
