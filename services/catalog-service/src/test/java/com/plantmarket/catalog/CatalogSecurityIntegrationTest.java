package com.plantmarket.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.plantmarket.catalog.domain.ReservationTargetType;
import com.plantmarket.catalog.web.dto.CategoryRequest;
import com.plantmarket.catalog.web.dto.CategoryResponse;
import com.plantmarket.catalog.web.dto.ReservationItemRequest;
import com.plantmarket.catalog.web.dto.ReservationRequest;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.health.mail.enabled=false", "app.security.enabled=true"})
@AutoConfigureTestRestTemplate
class CatalogSecurityIntegrationTest {

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
  void publicCatalogReadDoesNotRequireToken() {
    assertThat(restTemplate.getForEntity("/api/v1/products", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void catalogWriteRequiresStaffRole() {
    CategoryRequest request =
        new CategoryRequest("Security " + UUID.randomUUID(), "security-" + UUID.randomUUID(), null);

    ResponseEntity<String> anonymous =
        restTemplate.postForEntity("/api/v1/categories", request, String.class);
    assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    ResponseEntity<String> customer =
        restTemplate.exchange(
            "/api/v1/categories",
            HttpMethod.POST,
            new HttpEntity<>(request, bearer(List.of("CUSTOMER"))),
            String.class);
    assertThat(customer.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<CategoryResponse> admin =
        restTemplate.exchange(
            "/api/v1/categories",
            HttpMethod.POST,
            new HttpEntity<>(request, bearer(List.of("ADMIN"))),
            CategoryResponse.class);
    assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(admin.getBody()).isNotNull();
  }

  @Test
  void reservationEndpointRequiresSystemRole() {
    ReservationRequest request =
        new ReservationRequest(
            UUID.randomUUID(),
            List.of(
                new ReservationItemRequest(ReservationTargetType.VARIANT, UUID.randomUUID(), 1)));

    ResponseEntity<String> admin =
        restTemplate.exchange(
            "/api/v1/reservations",
            HttpMethod.POST,
            new HttpEntity<>(request, bearer(List.of("ADMIN"))),
            String.class);
    assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<String> system =
        restTemplate.exchange(
            "/api/v1/reservations",
            HttpMethod.POST,
            new HttpEntity<>(request, bearer(List.of("SYSTEM"))),
            String.class);
    assertThat(system.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
}
