package com.plantmarket.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.IndividualPlantStatus;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.domain.ProductStatus;
import com.plantmarket.catalog.domain.StockBatchStatus;
import com.plantmarket.catalog.domain.VariantStatus;
import com.plantmarket.catalog.web.dto.AdjustStockRequest;
import com.plantmarket.catalog.web.dto.CategoryRequest;
import com.plantmarket.catalog.web.dto.CategoryResponse;
import com.plantmarket.catalog.web.dto.IndividualPlantRequest;
import com.plantmarket.catalog.web.dto.IndividualPlantResponse;
import com.plantmarket.catalog.web.dto.PlantTransitionRequest;
import com.plantmarket.catalog.web.dto.ProductRequest;
import com.plantmarket.catalog.web.dto.ProductResponse;
import com.plantmarket.catalog.web.dto.StockBatchRequest;
import com.plantmarket.catalog.web.dto.StockBatchResponse;
import com.plantmarket.catalog.web.dto.VariantRequest;
import com.plantmarket.catalog.web.dto.VariantResponse;
import com.plantmarket.common.api.PageResponse;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CatalogPhase1IntegrationTest {

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
  void variantsPlantsAndStockBatchesFollowTheDomainRules() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    CategoryResponse category =
        restTemplate
            .postForEntity(
                "/api/v1/categories",
                new CategoryRequest("Variegated " + suffix, "variegated-" + suffix, null),
                CategoryResponse.class)
            .getBody();
    assertThat(category).isNotNull();

    ProductResponse product =
        restTemplate
            .postForEntity(
                "/api/v1/products",
                new ProductRequest(
                    "PROD-" + suffix,
                    "Monstera Albo",
                    "monstera-albo-" + suffix,
                    "Variegated plant",
                    category.id(),
                    129900L,
                    Currency.BYN,
                    ProductStatus.PUBLISHED,
                    CareLevel.MEDIUM,
                    LightLevel.MEDIUM,
                    false,
                    true,
                    "MEDIUM",
                    "Bright indirect light"),
                ProductResponse.class)
            .getBody();
    assertThat(product).isNotNull();
    assertThat(product.variegated()).isTrue();

    VariantRequest variantRequest =
        new VariantRequest(
            "VAR-" + suffix,
            "12 cm pot",
            "MEDIUM",
            "12cm",
            "POTTED",
            149900L,
            Currency.BYN,
            VariantStatus.ACTIVE);
    ResponseEntity<VariantResponse> variant =
        restTemplate.postForEntity(
            "/api/v1/products/" + product.id() + "/variants",
            variantRequest,
            VariantResponse.class);
    assertThat(variant.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(variant.getBody()).isNotNull();
    assertThat(variant.getBody().productId()).isEqualTo(product.id());

    ResponseEntity<List<VariantResponse>> variants =
        restTemplate.exchange(
            "/api/v1/products/" + product.id() + "/variants",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});
    assertThat(variants.getBody()).extracting(VariantResponse::sku).contains("VAR-" + suffix);

    assertThat(
            restTemplate
                .postForEntity(
                    "/api/v1/products/" + product.id() + "/variants", variantRequest, String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    IndividualPlantRequest plantRequest =
        new IndividualPlantRequest(
            "PLANT-" + suffix,
            "Individual variegated plant",
            "https://placehold.co/600x800?text=plant",
            12000L,
            Currency.BYN,
            CareLevel.MEDIUM,
            LightLevel.MEDIUM,
            false);
    IndividualPlantResponse plant =
        restTemplate
            .postForEntity(
                "/api/v1/products/" + product.id() + "/individual-plants",
                plantRequest,
                IndividualPlantResponse.class)
            .getBody();
    assertThat(plant).isNotNull();
    assertThat(plant.status()).isEqualTo(IndividualPlantStatus.ACQUIRED);

    assertThat(transition(plant.id(), IndividualPlantStatus.QUARANTINE).status())
        .isEqualTo(IndividualPlantStatus.QUARANTINE);
    assertThat(transition(plant.id(), IndividualPlantStatus.AVAILABLE).status())
        .isEqualTo(IndividualPlantStatus.AVAILABLE);
    assertThat(
            restTemplate
                .postForEntity(
                    "/api/v1/individual-plants/" + plant.id() + "/transitions",
                    new PlantTransitionRequest(IndividualPlantStatus.DELIVERED),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<PageResponse<IndividualPlantResponse>> filtered =
        restTemplate.exchange(
            "/api/v1/individual-plants?status=AVAILABLE&light=MEDIUM&variegated=true"
                + "&maxPriceMinor=15000",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});
    assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(filtered.getBody()).isNotNull();
    assertThat(filtered.getBody().items())
        .extracting(IndividualPlantResponse::id)
        .contains(plant.id());

    Instant receivedAt = Instant.now().minus(1, ChronoUnit.DAYS);
    Instant quarantineUntil = Instant.now().plus(1, ChronoUnit.DAYS);
    StockBatchRequest batchRequest =
        new StockBatchRequest(
            variant.getBody().id(),
            "Test Supplier",
            10,
            5000L,
            Currency.BYN,
            receivedAt,
            quarantineUntil);
    ResponseEntity<StockBatchResponse> batch =
        restTemplate.postForEntity("/api/v1/stock-batches", batchRequest, StockBatchResponse.class);
    assertThat(batch.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(batch.getBody()).isNotNull();
    assertThat(batch.getBody().status()).isEqualTo(StockBatchStatus.QUARANTINE);
    assertThat(batch.getBody().remainingQuantity()).isEqualTo(10);

    ResponseEntity<StockBatchResponse> released =
        restTemplate.postForEntity(
            "/api/v1/stock-batches/" + batch.getBody().id() + "/release",
            null,
            StockBatchResponse.class);
    assertThat(released.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(released.getBody()).isNotNull();
    assertThat(released.getBody().status()).isEqualTo(StockBatchStatus.AVAILABLE);

    ResponseEntity<StockBatchResponse> adjusted =
        restTemplate.postForEntity(
            "/api/v1/stock-batches/" + batch.getBody().id() + "/adjust",
            new AdjustStockRequest(4),
            StockBatchResponse.class);
    assertThat(adjusted.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(adjusted.getBody()).isNotNull();
    assertThat(adjusted.getBody().remainingQuantity()).isEqualTo(4);

    assertThat(
            restTemplate
                .postForEntity(
                    "/api/v1/stock-batches/" + batch.getBody().id() + "/adjust",
                    new AdjustStockRequest(99),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private IndividualPlantResponse transition(UUID id, IndividualPlantStatus status) {
    ResponseEntity<IndividualPlantResponse> response =
        restTemplate.postForEntity(
            "/api/v1/individual-plants/" + id + "/transitions",
            new PlantTransitionRequest(status),
            IndividualPlantResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    return response.getBody();
  }
}
