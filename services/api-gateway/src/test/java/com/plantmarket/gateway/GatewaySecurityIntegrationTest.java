package com.plantmarket.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityIntegrationTest {

  @LocalServerPort private int port;

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void healthIsPublicAndEchoesRequestId() {
    client
        .get()
        .uri("/actuator/health")
        .header("X-Request-Id", "test-request-123")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals("X-Request-Id", "test-request-123");
  }

  @Test
  void catalogMutationRequiresJwt() {
    client
        .post()
        .uri("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .expectHeader()
        .exists("X-Request-Id");
  }

  @Test
  void invalidJwtIsRejected() {
    client
        .post()
        .uri("/api/v1/products")
        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void validJwtPassesSecurityAndReachesRouting() {
    client
        .post()
        .uri("/api/v1/products")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Test
  void corsPreflightIsAllowedForStorefront() {
    client
        .options()
        .uri("/api/v1/products")
        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
  }

  private String accessToken() {
    try {
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .subject(UUID.randomUUID().toString())
              .issuer("gateway-test")
              .issueTime(new Date())
              .expirationTime(Date.from(Instant.now().plusSeconds(300)))
              .build();
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(jwtSecret));
      assertThat(jwt.serialize()).isNotBlank();
      return jwt.serialize();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create test token", exception);
    }
  }
}
