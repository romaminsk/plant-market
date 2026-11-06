package com.plantmarket.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

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
import com.plantmarket.order.domain.CareReminderEntity;
import com.plantmarket.order.domain.ClaimDecision;
import com.plantmarket.order.domain.ClaimStatus;
import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.domain.OutboxEventEntity;
import com.plantmarket.order.notification.OutboxPublisher;
import com.plantmarket.order.repository.CareReminderRepository;
import com.plantmarket.order.repository.OutboxEventRepository;
import com.plantmarket.order.service.CareReminderService;
import com.plantmarket.order.web.dto.CareReminderRequest;
import com.plantmarket.order.web.dto.CareReminderResponse;
import com.plantmarket.order.web.dto.CheckoutRequest;
import com.plantmarket.order.web.dto.ClaimDecisionRequest;
import com.plantmarket.order.web.dto.ClaimRequest;
import com.plantmarket.order.web.dto.ClaimResponse;
import com.plantmarket.order.web.dto.CreateOrderItemRequest;
import com.plantmarket.order.web.dto.CreateOrderRequest;
import com.plantmarket.order.web.dto.OrderResponse;
import com.plantmarket.order.web.dto.OutboxEventResponse;
import com.plantmarket.order.web.dto.ShippingZoneRequest;
import com.plantmarket.order.web.dto.ShippingZoneResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.health.mail.enabled=false", "spring.task.scheduling.enabled=false"})
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
  @Autowired private OutboxPublisher outboxPublisher;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private CareReminderRepository careReminderRepository;
  @Autowired private CareReminderService careReminderService;

  @MockitoBean private JavaMailSender mailSender;

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
    outboxPublisher.publishPending();
    assertThat(outboxEventRepository.findAll()).allMatch(event -> event.getPublishedAt() != null);

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

  @Test
  void adminCanRedriveDeadOutboxEvent() {
    OutboxEventEntity event =
        new OutboxEventEntity(
            UUID.randomUUID(), "order.paid", "{\"orderId\":\"x\",\"status\":\"PAID\"}");
    for (int attempt = 0; attempt < 5; attempt++) {
      event.markFailed("smtp down", Instant.now());
    }
    event.markDead();
    outboxEventRepository.save(event);

    ResponseEntity<OutboxEventResponse> redriven =
        restTemplate.exchange(
            "/api/v1/admin/outbox/" + event.getId() + "/redrive",
            HttpMethod.POST,
            new HttpEntity<>(adminHeaders()),
            OutboxEventResponse.class);
    assertThat(redriven.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(redriven.getBody()).isNotNull();
    assertThat(redriven.getBody().dead()).isFalse();
    assertThat(redriven.getBody().attempts()).isZero();
    assertThat(redriven.getBody().publishedAt()).isNull();

    ResponseEntity<List<OutboxEventResponse>> dead =
        restTemplate.exchange(
            "/api/v1/admin/outbox/dead",
            HttpMethod.GET,
            new HttpEntity<>(adminHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(dead.getBody()).noneMatch(item -> item.id().equals(event.getId()));

    ResponseEntity<String> forbidden =
        restTemplate.exchange(
            "/api/v1/admin/outbox/" + event.getId() + "/redrive",
            HttpMethod.POST,
            new HttpEntity<>(headers(UUID.randomUUID())),
            String.class);
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void outboxRetriesFailedNotificationAndPublishesAfterRecovery() {
    UUID customerId = UUID.randomUUID();
    HttpHeaders headers = headers(customerId);
    UUID orderId = createVariantOrder(headers);
    restTemplate.exchange(
        "/api/v1/orders/" + orderId + "/checkout",
        HttpMethod.POST,
        new HttpEntity<>(Map.of(), headers),
        OrderResponse.class);
    restTemplate.exchange(
        "/api/v1/orders/" + orderId + "/pay",
        HttpMethod.POST,
        new HttpEntity<>(Map.of(), headers),
        OrderResponse.class);

    doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
    outboxPublisher.publishPending();

    OutboxEventEntity paid =
        outboxEventRepository.findAll().stream()
            .filter(event ->
                "order.paid".equals(event.getEventType()) && event.getAggregateId().equals(orderId))
            .findFirst()
            .orElseThrow();
    assertThat(paid.getPublishedAt()).isNull();
    assertThat(paid.getAttempts()).isEqualTo(1);
    assertThat(paid.getNextAttemptAt()).isAfter(Instant.now());

    reset(mailSender);
    paid.scheduleNextAttempt(Instant.now().minusSeconds(1));
    outboxEventRepository.save(paid);
    outboxPublisher.publishPending();
    assertThat(outboxEventRepository.findById(paid.getId()).orElseThrow().getPublishedAt())
        .isNotNull();
  }

  @Test
  void careRemindersCanBeOptedInListedAndDisabled() {
    UUID customerId = UUID.randomUUID();
    HttpHeaders headers = headers(customerId);
    UUID orderId = createVariantOrder(headers);
    restTemplate.exchange(
        "/api/v1/orders/" + orderId + "/checkout",
        HttpMethod.POST,
        new HttpEntity<>(Map.of(), headers),
        OrderResponse.class);
    ResponseEntity<OrderResponse> paid =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/pay",
            HttpMethod.POST,
            new HttpEntity<>(Map.of(), headers),
            OrderResponse.class);
    assertThat(paid.getBody()).isNotNull();
    UUID orderItemId = paid.getBody().items().getFirst().id();

    ResponseEntity<List<CareReminderResponse>> created =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/care-reminders",
            HttpMethod.POST,
            new HttpEntity<>(new CareReminderRequest(7), headers),
            new ParameterizedTypeReference<>() {});
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).hasSize(1);
    CareReminderResponse reminder = created.getBody().getFirst();
    assertThat(reminder.active()).isTrue();
    assertThat(reminder.intervalDays()).isEqualTo(7);

    ResponseEntity<List<CareReminderResponse>> listed =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/care-reminders",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});
    assertThat(listed.getBody()).hasSize(1);

    CareReminderEntity due = careReminderRepository.findById(reminder.id()).orElseThrow();
    due.scheduleAt(Instant.now().minusSeconds(60));
    careReminderRepository.save(due);
    careReminderService.sendDue();
    assertThat(careReminderRepository.findById(reminder.id()).orElseThrow().getNextSendAt())
        .isAfter(Instant.now());

    ResponseEntity<String> deleted =
        restTemplate.exchange(
            "/api/v1/care-reminders/" + reminder.id(),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    ResponseEntity<List<CareReminderResponse>> afterDelete =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/care-reminders",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});
    assertThat(afterDelete.getBody()).isEmpty();

    UUID draftOrderId = createVariantOrder(headers);
    ResponseEntity<String> rejected =
        restTemplate.exchange(
            "/api/v1/orders/" + draftOrderId + "/care-reminders",
            HttpMethod.POST,
            new HttpEntity<>(new CareReminderRequest(7), headers),
            String.class);
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void checkoutAppliesShippingZoneAndThermalRule() {
    UUID customerId = UUID.randomUUID();
    HttpHeaders headers = headers(customerId);

    ShippingZoneResponse normalZone =
        restTemplate
            .exchange(
                "/api/v1/shipping/zones",
                HttpMethod.POST,
                new HttpEntity<>(
                    new ShippingZoneRequest("Minsk", "Minsk", 500L, 50000L, 1, 2, false, 0L, true),
                    headers),
                ShippingZoneResponse.class)
            .getBody();
    assertThat(normalZone).isNotNull();

    UUID firstOrder = createVariantOrder(headers);
    ResponseEntity<OrderResponse> firstCheckout =
        restTemplate.exchange(
            "/api/v1/orders/" + firstOrder + "/checkout",
            HttpMethod.POST,
            new HttpEntity<>(
                new CheckoutRequest(normalZone.id(), false, "Minsk, Nezavisimosti 1"), headers),
            OrderResponse.class);
    assertThat(firstCheckout.getBody()).isNotNull();
    assertThat(firstCheckout.getBody().shippingMinor()).isEqualTo(500L);
    assertThat(firstCheckout.getBody().totalMinor()).isEqualTo(10500L);

    ShippingZoneResponse winterZone =
        restTemplate
            .exchange(
                "/api/v1/shipping/zones",
                HttpMethod.POST,
                new HttpEntity<>(
                    new ShippingZoneRequest(
                        "Gomel winter", "Gomel", 500L, 50000L, 2, 4, true, 300L, true),
                    headers),
                ShippingZoneResponse.class)
            .getBody();
    assertThat(winterZone).isNotNull();

    UUID secondOrder = createVariantOrder(headers);
    ResponseEntity<String> rejected =
        restTemplate.exchange(
            "/api/v1/orders/" + secondOrder + "/checkout",
            HttpMethod.POST,
            new HttpEntity<>(
                new CheckoutRequest(winterZone.id(), false, "Gomel, Sovetskaya 1"), headers),
            String.class);
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<OrderResponse> accepted =
        restTemplate.exchange(
            "/api/v1/orders/" + secondOrder + "/checkout",
            HttpMethod.POST,
            new HttpEntity<>(
                new CheckoutRequest(winterZone.id(), true, "Gomel, Sovetskaya 1"), headers),
            OrderResponse.class);
    assertThat(accepted.getBody()).isNotNull();
    assertThat(accepted.getBody().shippingMinor()).isEqualTo(800L);
    assertThat(accepted.getBody().thermalPack()).isTrue();
    assertThat(accepted.getBody().deliveryAddress()).isEqualTo("Gomel, Sovetskaya 1");
  }

  @Test
  void customerOpensClaimAndAdminDecidesReplacement() {
    UUID customerId = UUID.randomUUID();
    HttpHeaders headers = headers(customerId);
    UUID orderId = createVariantOrder(headers);
    restTemplate.exchange(
        "/api/v1/orders/" + orderId + "/checkout",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        OrderResponse.class);
    ResponseEntity<OrderResponse> paid =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/pay",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            OrderResponse.class);
    assertThat(paid.getBody()).isNotNull();
    assertThat(paid.getBody().status()).isEqualTo(OrderStatus.PAID);

    ResponseEntity<ClaimResponse> opened =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/claims",
            HttpMethod.POST,
            new HttpEntity<>(
                new ClaimRequest(
                    "Damaged leaf on delivery", List.of("https://example.test/photo-1.jpg")),
                headers),
            ClaimResponse.class);
    assertThat(opened.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(opened.getBody()).isNotNull();
    assertThat(opened.getBody().status()).isEqualTo(ClaimStatus.OPEN);
    assertThat(opened.getBody().photoUrls()).containsExactly("https://example.test/photo-1.jpg");
    UUID claimId = opened.getBody().id();

    ResponseEntity<ClaimResponse[]> claims =
        restTemplate.exchange(
            "/api/v1/orders/" + orderId + "/claims",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ClaimResponse[].class);
    assertThat(claims.getBody()).extracting(ClaimResponse::id).contains(claimId);

    ResponseEntity<ClaimResponse> decided =
        restTemplate.exchange(
            "/api/v1/claims/" + claimId + "/decision",
            HttpMethod.POST,
            new HttpEntity<>(
                new ClaimDecisionRequest(ClaimDecision.REPLACEMENT, "Replacement approved"),
                adminHeaders()),
            ClaimResponse.class);
    assertThat(decided.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(decided.getBody()).isNotNull();
    assertThat(decided.getBody().status()).isEqualTo(ClaimStatus.REPLACEMENT);
    assertThat(decided.getBody().decidedAt()).isNotNull();

    ResponseEntity<String> secondDecision =
        restTemplate.exchange(
            "/api/v1/claims/" + claimId + "/decision",
            HttpMethod.POST,
            new HttpEntity<>(new ClaimDecisionRequest(ClaimDecision.REFUND, null), adminHeaders()),
            String.class);
    assertThat(secondDecision.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    UUID draftOrderId = createVariantOrder(headers);
    ResponseEntity<String> rejected =
        restTemplate.exchange(
            "/api/v1/orders/" + draftOrderId + "/claims",
            HttpMethod.POST,
            new HttpEntity<>(
                new ClaimRequest("Damaged leaf", List.of("https://example.test/photo-2.jpg")),
                headers),
            String.class);
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  private UUID createVariantOrder(HttpHeaders headers) {
    ResponseEntity<OrderResponse> created =
        restTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            new HttpEntity<>(
                new CreateOrderRequest(List.of(new CreateOrderItemRequest(VARIANT_ID, null, 1))),
                headers),
            OrderResponse.class);
    assertThat(created.getBody()).isNotNull();
    return created.getBody().id();
  }

  private HttpHeaders headers(UUID customerId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token(customerId));
    return headers;
  }

  private HttpHeaders adminHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token(UUID.randomUUID(), List.of("ADMIN")));
    return headers;
  }

  private String token(UUID customerId) {
    return token(customerId, List.of());
  }

  private String token(UUID customerId, List<String> roles) {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(customerId.toString())
              .claim("email", "customer-" + customerId + "@example.test")
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
