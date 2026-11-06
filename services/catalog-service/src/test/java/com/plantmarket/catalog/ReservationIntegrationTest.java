package com.plantmarket.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.IndividualPlantStatus;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.domain.ProductStatus;
import com.plantmarket.catalog.domain.ReservationStatus;
import com.plantmarket.catalog.domain.ReservationTargetType;
import com.plantmarket.catalog.domain.VariantStatus;
import com.plantmarket.catalog.web.dto.CategoryRequest;
import com.plantmarket.catalog.web.dto.CategoryResponse;
import com.plantmarket.catalog.web.dto.IndividualPlantRequest;
import com.plantmarket.catalog.web.dto.IndividualPlantResponse;
import com.plantmarket.catalog.web.dto.PlantTransitionRequest;
import com.plantmarket.catalog.web.dto.ProductRequest;
import com.plantmarket.catalog.web.dto.ProductResponse;
import com.plantmarket.catalog.web.dto.ReservationItemRequest;
import com.plantmarket.catalog.web.dto.ReservationRequest;
import com.plantmarket.catalog.web.dto.ReservationResponse;
import com.plantmarket.catalog.web.dto.StockBatchRequest;
import com.plantmarket.catalog.web.dto.StockBatchResponse;
import com.plantmarket.catalog.web.dto.VariantRequest;
import com.plantmarket.catalog.web.dto.VariantResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ReservationIntegrationTest {

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

  @Test
  void variantReservationRespectsStockAndReleases() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    VariantFixture fixture = createVariantFixture(suffix, 10);
    UUID orderId = UUID.randomUUID();

    ReservationResponse reserved =
        reserve(orderId, ReservationTargetType.VARIANT, fixture.variant().id(), 3);
    assertThat(reserved.reservations()).hasSize(1);
    assertThat(reserved.reservations().getFirst().status()).isEqualTo(ReservationStatus.ACTIVE);

    ResponseEntity<String> tooMany =
        restTemplate.postForEntity(
            "/api/v1/reservations",
            new ReservationRequest(
                UUID.randomUUID(),
                List.of(
                    new ReservationItemRequest(
                        ReservationTargetType.VARIANT, fixture.variant().id(), 8))),
            String.class);
    assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ReservationResponse released =
        restTemplate.postForObject(
            "/api/v1/reservations/" + orderId + "/release", null, ReservationResponse.class);
    assertThat(released.reservations().getFirst().status()).isEqualTo(ReservationStatus.RELEASED);

    UUID secondOrderId = UUID.randomUUID();
    ReservationResponse afterRelease =
        reserve(secondOrderId, ReservationTargetType.VARIANT, fixture.variant().id(), 8);
    assertThat(afterRelease.reservations()).hasSize(1);

    ReservationResponse confirmed =
        restTemplate.postForObject(
            "/api/v1/reservations/" + secondOrderId + "/confirm", null, ReservationResponse.class);
    assertThat(confirmed.reservations().getFirst().status()).isEqualTo(ReservationStatus.CONFIRMED);
    ResponseEntity<List<StockBatchResponse>> batches =
        restTemplate.exchange(
            "/api/v1/stock-batches?variantId=" + fixture.variant().id(),
            org.springframework.http.HttpMethod.GET,
            null,
            new org.springframework.core.ParameterizedTypeReference<>() {});
    assertThat(batches.getBody()).isNotNull();
    assertThat(batches.getBody().getFirst().remainingQuantity()).isEqualTo(2);
  }

  @Test
  void individualPlantReservationMovesPlantToReservedAndBack() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    VariantFixture fixture = createVariantFixture(suffix, 1);
    IndividualPlantResponse plant = createAvailablePlant(fixture.product().id(), suffix);
    UUID orderId = UUID.randomUUID();

    ReservationResponse reserved =
        reserve(orderId, ReservationTargetType.INDIVIDUAL_PLANT, plant.id(), 1);
    assertThat(reserved.reservations().getFirst().status()).isEqualTo(ReservationStatus.ACTIVE);
    assertThat(
            restTemplate
                .getForEntity(
                    "/api/v1/individual-plants/" + plant.id(), IndividualPlantResponse.class)
                .getBody()
                .status())
        .isEqualTo(IndividualPlantStatus.RESERVED);

    restTemplate.postForObject(
        "/api/v1/reservations/" + orderId + "/release", null, ReservationResponse.class);
    assertThat(
            restTemplate
                .getForEntity(
                    "/api/v1/individual-plants/" + plant.id(), IndividualPlantResponse.class)
                .getBody()
                .status())
        .isEqualTo(IndividualPlantStatus.AVAILABLE);
  }

  private ReservationResponse reserve(
      UUID orderId, ReservationTargetType targetType, UUID targetId, int quantity) {
    ResponseEntity<ReservationResponse> response =
        restTemplate.postForEntity(
            "/api/v1/reservations",
            new ReservationRequest(
                orderId, List.of(new ReservationItemRequest(targetType, targetId, quantity))),
            ReservationResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    return response.getBody();
  }

  private VariantFixture createVariantFixture(String suffix, int batchQuantity) {
    CategoryResponse category =
        restTemplate
            .postForEntity(
                "/api/v1/categories",
                new CategoryRequest("Res " + suffix, "res-" + suffix, null),
                CategoryResponse.class)
            .getBody();
    assertThat(category).isNotNull();
    ProductResponse product =
        restTemplate
            .postForEntity(
                "/api/v1/products",
                new ProductRequest(
                    "RES-" + suffix,
                    "Reservation Plant " + suffix,
                    "reservation-plant-" + suffix,
                    null,
                    category.id(),
                    10000L,
                    Currency.BYN,
                    ProductStatus.PUBLISHED,
                    CareLevel.EASY,
                    LightLevel.MEDIUM,
                    true,
                    false,
                    null,
                    null),
                ProductResponse.class)
            .getBody();
    assertThat(product).isNotNull();
    VariantResponse variant =
        restTemplate
            .postForEntity(
                "/api/v1/products/" + product.id() + "/variants",
                new VariantRequest(
                    "RES-VAR-" + suffix,
                    "Default",
                    null,
                    null,
                    null,
                    10000L,
                    Currency.BYN,
                    VariantStatus.ACTIVE),
                VariantResponse.class)
            .getBody();
    assertThat(variant).isNotNull();
    StockBatchResponse batch =
        restTemplate
            .postForEntity(
                "/api/v1/stock-batches",
                new StockBatchRequest(
                    variant.id(),
                    "Reservation Supplier",
                    batchQuantity,
                    5000L,
                    Currency.BYN,
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    Instant.now().plus(1, ChronoUnit.DAYS)),
                StockBatchResponse.class)
            .getBody();
    assertThat(batch).isNotNull();
    restTemplate.postForEntity(
        "/api/v1/stock-batches/" + batch.id() + "/release", null, StockBatchResponse.class);
    return new VariantFixture(product, variant);
  }

  private IndividualPlantResponse createAvailablePlant(UUID productId, String suffix) {
    IndividualPlantResponse plant =
        restTemplate
            .postForEntity(
                "/api/v1/products/" + productId + "/individual-plants",
                new IndividualPlantRequest(
                    "RES-PLANT-" + suffix,
                    "Reservation individual plant",
                    null,
                    12000L,
                    Currency.BYN,
                    CareLevel.EASY,
                    LightLevel.MEDIUM,
                    true),
                IndividualPlantResponse.class)
            .getBody();
    assertThat(plant).isNotNull();
    restTemplate.postForEntity(
        "/api/v1/individual-plants/" + plant.id() + "/transitions",
        new PlantTransitionRequest(IndividualPlantStatus.QUARANTINE),
        IndividualPlantResponse.class);
    return restTemplate
        .postForEntity(
            "/api/v1/individual-plants/" + plant.id() + "/transitions",
            new PlantTransitionRequest(IndividualPlantStatus.AVAILABLE),
            IndividualPlantResponse.class)
        .getBody();
  }

  private record VariantFixture(ProductResponse product, VariantResponse variant) {}
}
