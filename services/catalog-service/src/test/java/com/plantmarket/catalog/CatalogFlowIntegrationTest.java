package com.plantmarket.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.domain.ProductStatus;
import com.plantmarket.catalog.web.dto.CategoryRequest;
import com.plantmarket.catalog.web.dto.CategoryResponse;
import com.plantmarket.catalog.web.dto.ProductRequest;
import com.plantmarket.catalog.web.dto.ProductResponse;
import com.plantmarket.common.api.PageResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CatalogFlowIntegrationTest {

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
  void createListUpdateAndArchiveProduct() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    CategoryResponse category =
        restTemplate
            .postForEntity(
                "/api/v1/categories",
                new CategoryRequest("Monstera " + suffix, "monstera-" + suffix, "Large leaves"),
                CategoryResponse.class)
            .getBody();
    assertThat(category).isNotNull();

    ProductRequest productRequest =
        new ProductRequest(
            "SKU-" + suffix,
            "Monstera Deliciosa",
            "monstera-deliciosa-" + suffix,
            "A mature plant",
            category.id(),
            129900L,
            Currency.BYN,
            ProductStatus.PUBLISHED,
            CareLevel.EASY,
            LightLevel.BRIGHT,
            false,
            false,
            null,
            null);

    ResponseEntity<ProductResponse> created =
        restTemplate.postForEntity("/api/v1/products", productRequest, ProductResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    UUID productId = created.getBody().id();
    assertThat(created.getBody().priceMinor()).isEqualTo(129900L);

    ResponseEntity<PageResponse<ProductResponse>> listed =
        restTemplate.exchange(
            "/api/v1/products?page=0&size=10&search=monstera&status=PUBLISHED",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() {});
    assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listed.getBody()).isNotNull();
    assertThat(listed.getBody().items()).extracting(ProductResponse::id).contains(productId);
    assertThat(listed.getBody().totalElements()).isEqualTo(1);

    ResponseEntity<ProductResponse> fetched =
        restTemplate.getForEntity("/api/v1/products/" + productId, ProductResponse.class);
    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().categoryId()).isEqualTo(category.id());

    ProductRequest updated =
        new ProductRequest(
            productRequest.sku(),
            "Monstera Deliciosa XL",
            productRequest.slug(),
            productRequest.description(),
            category.id(),
            149900L,
            Currency.BYN,
            ProductStatus.PUBLISHED,
            CareLevel.MEDIUM,
            LightLevel.BRIGHT,
            true,
            false,
            null,
            null);
    ResponseEntity<ProductResponse> updatedResponse =
        restTemplate.exchange(
            "/api/v1/products/" + productId,
            HttpMethod.PUT,
            new HttpEntity<>(updated),
            ProductResponse.class);
    assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updatedResponse.getBody()).isNotNull();
    assertThat(updatedResponse.getBody().priceMinor()).isEqualTo(149900L);

    ResponseEntity<Void> archived =
        restTemplate.exchange("/api/v1/products/" + productId, HttpMethod.DELETE, null, Void.class);
    assertThat(archived.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(
            restTemplate
                .getForEntity("/api/v1/products/" + productId, ProductResponse.class)
                .getBody()
                .status())
        .isEqualTo(ProductStatus.ARCHIVED);

    ResponseEntity<String> deleteCategory =
        restTemplate.exchange(
            "/api/v1/categories/" + category.id(), HttpMethod.DELETE, null, String.class);
    assertThat(deleteCategory.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void rejectsDuplicateSkuAndInvalidPagination() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    CategoryResponse category =
        restTemplate
            .postForEntity(
                "/api/v1/categories",
                new CategoryRequest("Ferns " + suffix, "ferns-" + suffix, null),
                CategoryResponse.class)
            .getBody();
    assertThat(category).isNotNull();

    ProductRequest product =
        new ProductRequest(
            "SKU-" + suffix,
            "Boston Fern",
            "boston-fern-" + suffix,
            null,
            category.id(),
            4500L,
            Currency.BYN,
            ProductStatus.DRAFT,
            CareLevel.MEDIUM,
            LightLevel.MEDIUM,
            true,
            false,
            null,
            null);
    assertThat(
            restTemplate
                .postForEntity("/api/v1/products", product, ProductResponse.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(
            restTemplate.postForEntity("/api/v1/products", product, String.class).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<String> invalidPage =
        restTemplate.getForEntity("/api/v1/products?size=500", String.class);
    assertThat(invalidPage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(invalidPage.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
  }
}
