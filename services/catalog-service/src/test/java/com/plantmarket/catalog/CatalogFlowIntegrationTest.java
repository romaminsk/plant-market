package com.plantmarket.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.plantmarket.catalog.domain.CareLevel;
import com.plantmarket.catalog.domain.Currency;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.domain.PlantPhotoKind;
import com.plantmarket.catalog.domain.ProductStatus;
import com.plantmarket.catalog.domain.ReservationTargetType;
import com.plantmarket.catalog.domain.RestockSubscriptionStatus;
import com.plantmarket.catalog.domain.RestockTargetType;
import com.plantmarket.catalog.domain.VariantStatus;
import com.plantmarket.catalog.notification.RestockNotifier;
import com.plantmarket.catalog.web.dto.CategoryRequest;
import com.plantmarket.catalog.web.dto.CategoryResponse;
import com.plantmarket.catalog.web.dto.IndividualPlantRequest;
import com.plantmarket.catalog.web.dto.IndividualPlantResponse;
import com.plantmarket.catalog.web.dto.MediaResponse;
import com.plantmarket.catalog.web.dto.PlantPhotoRequest;
import com.plantmarket.catalog.web.dto.ProductRequest;
import com.plantmarket.catalog.web.dto.ProductResponse;
import com.plantmarket.catalog.web.dto.ReservationItemRequest;
import com.plantmarket.catalog.web.dto.ReservationRequest;
import com.plantmarket.catalog.web.dto.ReservationResponse;
import com.plantmarket.catalog.web.dto.RestockSubscriptionRequest;
import com.plantmarket.catalog.web.dto.RestockSubscriptionResponse;
import com.plantmarket.catalog.web.dto.StockBatchRequest;
import com.plantmarket.catalog.web.dto.StockBatchResponse;
import com.plantmarket.catalog.web.dto.UpdatePlantPhotosRequest;
import com.plantmarket.catalog.web.dto.VariantRequest;
import com.plantmarket.catalog.web.dto.VariantResponse;
import com.plantmarket.common.api.PageResponse;
import java.nio.file.Path;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.health.mail.enabled=false")
@AutoConfigureTestRestTemplate
class CatalogFlowIntegrationTest {

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
  private static final Path MEDIA_ROOT = Path.of("build", "test-media");

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
    registry.add("app.media.root", () -> MEDIA_ROOT.toAbsolutePath().toString());
    registry.add("app.media.public-base-url", () -> "http://test.local/media");
  }

  @Autowired private TestRestTemplate restTemplate;
  @MockitoBean private RestockNotifier restockNotifier;

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
            null,
            "https://cdn.example.test/monstera.jpg");

    ResponseEntity<ProductResponse> created =
        restTemplate.postForEntity("/api/v1/products", productRequest, ProductResponse.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isNotNull();
    UUID productId = created.getBody().id();
    assertThat(created.getBody().priceMinor()).isEqualTo(129900L);
    assertThat(created.getBody().imageUrl()).isEqualTo("https://cdn.example.test/monstera.jpg");

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
  void uploadsImageAndServesItFromMediaPath() {
    byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(MediaType.IMAGE_PNG);
    ByteArrayResource file =
        new ByteArrayResource(png) {
          @Override
          public String getFilename() {
            return "plant.png";
          }
        };
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new HttpEntity<>(file, partHeaders));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<MediaResponse> uploaded =
        restTemplate.postForEntity(
            "/api/v1/media", new HttpEntity<>(body, headers), MediaResponse.class);
    assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(uploaded.getBody()).isNotNull();
    assertThat(uploaded.getBody().url()).startsWith("http://test.local/media/").endsWith(".png");

    String path = uploaded.getBody().url().substring("http://test.local".length());
    ResponseEntity<byte[]> fetched = restTemplate.getForEntity(path, byte[].class);
    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody()).isEqualTo(png);

    HttpHeaders textPart = new HttpHeaders();
    textPart.setContentType(MediaType.TEXT_PLAIN);
    MultiValueMap<String, Object> invalid = new LinkedMultiValueMap<>();
    invalid.add("file", new HttpEntity<>(file, textPart));
    ResponseEntity<String> rejected =
        restTemplate.postForEntity(
            "/api/v1/media", new HttpEntity<>(invalid, headers), String.class);
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ByteArrayResource fakePng =
        new ByteArrayResource("not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
          @Override
          public String getFilename() {
            return "fake.png";
          }
        };
    MultiValueMap<String, Object> spoofed = new LinkedMultiValueMap<>();
    spoofed.add("file", new HttpEntity<>(fakePng, partHeaders));
    ResponseEntity<String> spoofedResponse =
        restTemplate.postForEntity(
            "/api/v1/media", new HttpEntity<>(spoofed, headers), String.class);
    assertThat(spoofedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void individualPlantGalleryRequiresGeneralPhoto() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    CategoryResponse category =
        restTemplate
            .postForEntity(
                "/api/v1/categories",
                new CategoryRequest("Gallery " + suffix, "gallery-" + suffix, null),
                CategoryResponse.class)
            .getBody();
    assertThat(category).isNotNull();

    ProductResponse product =
        restTemplate
            .postForEntity(
                "/api/v1/products",
                new ProductRequest(
                    "GAL-" + suffix,
                    "Gallery Plant",
                    "gallery-plant-" + suffix,
                    null,
                    category.id(),
                    7000L,
                    Currency.BYN,
                    ProductStatus.PUBLISHED,
                    CareLevel.EASY,
                    LightLevel.MEDIUM,
                    true,
                    false,
                    null,
                    null,
                    null),
                ProductResponse.class)
            .getBody();
    assertThat(product).isNotNull();

    IndividualPlantResponse plant =
        restTemplate
            .postForEntity(
                "/api/v1/products/" + product.id() + "/individual-plants",
                new IndividualPlantRequest(
                    "GAL-IP-" + suffix,
                    "Gallery plant",
                    null,
                    7000L,
                    Currency.BYN,
                    CareLevel.EASY,
                    LightLevel.MEDIUM,
                    true),
                IndividualPlantResponse.class)
            .getBody();
    assertThat(plant).isNotNull();

    ResponseEntity<IndividualPlantResponse> updated =
        restTemplate.exchange(
            "/api/v1/individual-plants/" + plant.id() + "/photos",
            HttpMethod.PUT,
            new HttpEntity<UpdatePlantPhotosRequest>(
                new UpdatePlantPhotosRequest(
                    List.of(
                        new PlantPhotoRequest(
                            "https://example.test/general.jpg", PlantPhotoKind.GENERAL),
                        new PlantPhotoRequest("https://example.test/leaf.jpg", PlantPhotoKind.LEAF),
                        new PlantPhotoRequest(
                            "https://example.test/roots.jpg", PlantPhotoKind.ROOTS),
                        new PlantPhotoRequest(
                            "https://example.test/defects.jpg", PlantPhotoKind.DEFECTS)))),
            IndividualPlantResponse.class);
    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody()).isNotNull();
    assertThat(updated.getBody().photos()).hasSize(4);
    assertThat(updated.getBody().photos().getFirst().kind()).isEqualTo(PlantPhotoKind.GENERAL);
    assertThat(updated.getBody().photoUrl()).isEqualTo("https://example.test/general.jpg");

    ResponseEntity<String> withoutGeneral =
        restTemplate.exchange(
            "/api/v1/individual-plants/" + plant.id() + "/photos",
            HttpMethod.PUT,
            new HttpEntity<UpdatePlantPhotosRequest>(
                new UpdatePlantPhotosRequest(
                    List.of(
                        new PlantPhotoRequest(
                            "https://example.test/leaf-only.jpg", PlantPhotoKind.LEAF)))),
            String.class);
    assertThat(withoutGeneral.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void restockSubscriptionNotifiesWhenBatchIsReleased() {
    when(restockNotifier.notifyAvailable(anyString(), anyString())).thenReturn(true);
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    CategoryResponse category =
        restTemplate
            .postForEntity(
                "/api/v1/categories",
                new CategoryRequest("Restock " + suffix, "restock-" + suffix, null),
                CategoryResponse.class)
            .getBody();
    assertThat(category).isNotNull();

    ProductResponse product =
        restTemplate
            .postForEntity(
                "/api/v1/products",
                new ProductRequest(
                    "RS-" + suffix,
                    "Restock Plant",
                    "restock-plant-" + suffix,
                    null,
                    category.id(),
                    5000L,
                    Currency.BYN,
                    ProductStatus.PUBLISHED,
                    CareLevel.EASY,
                    LightLevel.MEDIUM,
                    true,
                    false,
                    null,
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
                    "RS-SKU-" + suffix,
                    "Restock variant",
                    null,
                    null,
                    null,
                    5000L,
                    Currency.BYN,
                    VariantStatus.ACTIVE),
                VariantResponse.class)
            .getBody();
    assertThat(variant).isNotNull();

    String email = "restock-" + suffix + "@example.test";
    RestockSubscriptionResponse subscription =
        restTemplate
            .postForEntity(
                "/api/v1/restock-subscriptions",
                new RestockSubscriptionRequest(RestockTargetType.VARIANT, variant.id(), email),
                RestockSubscriptionResponse.class)
            .getBody();
    assertThat(subscription).isNotNull();
    assertThat(subscription.status()).isEqualTo(RestockSubscriptionStatus.PENDING);

    ResponseEntity<String> duplicate =
        restTemplate.postForEntity(
            "/api/v1/restock-subscriptions",
            new RestockSubscriptionRequest(RestockTargetType.VARIANT, variant.id(), email),
            String.class);
    assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);

    StockBatchResponse batch =
        restTemplate
            .postForEntity(
                "/api/v1/stock-batches",
                new StockBatchRequest(
                    variant.id(),
                    "Restock Supplier",
                    3,
                    1000L,
                    Currency.BYN,
                    null,
                    Instant.now().plus(1, ChronoUnit.DAYS)),
                StockBatchResponse.class)
            .getBody();
    assertThat(batch).isNotNull();

    ResponseEntity<String> released =
        restTemplate.postForEntity(
            "/api/v1/stock-batches/" + batch.id() + "/release", null, String.class);
    assertThat(released.getStatusCode()).isEqualTo(HttpStatus.OK);

    RestockSubscriptionResponse notified =
        restTemplate
            .getForEntity(
                "/api/v1/restock-subscriptions/" + subscription.id(),
                RestockSubscriptionResponse.class)
            .getBody();
    assertThat(notified).isNotNull();
    assertThat(notified.status()).isEqualTo(RestockSubscriptionStatus.NOTIFIED);
    assertThat(notified.notifiedAt()).isNotNull();

    ResponseEntity<String> subscribeWhenAvailable =
        restTemplate.postForEntity(
            "/api/v1/restock-subscriptions",
            new RestockSubscriptionRequest(RestockTargetType.VARIANT, variant.id(), email),
            String.class);
    assertThat(subscribeWhenAvailable.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    ReservationResponse reservation =
        restTemplate
            .postForEntity(
                "/api/v1/reservations",
                new ReservationRequest(
                    UUID.randomUUID(),
                    List.of(
                        new ReservationItemRequest(
                            ReservationTargetType.VARIANT, variant.id(), 1))),
                ReservationResponse.class)
            .getBody();
    assertThat(reservation).isNotNull();
    assertThat(reservation.reservations()).hasSize(1);
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
