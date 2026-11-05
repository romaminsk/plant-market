package com.plantmarket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.auth.web.dto.LoginRequest;
import com.plantmarket.auth.web.dto.RefreshRequest;
import com.plantmarket.auth.web.dto.RegisterRequest;
import com.plantmarket.auth.web.dto.TokenResponse;
import com.plantmarket.auth.web.dto.UserResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthFlowIntegrationTest {

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
  void registerLoginReadCurrentUserAndRotateRefreshToken() {
    String email = "alice-" + UUID.randomUUID() + "@example.com";
    String password = "correct-horse-battery";

    ResponseEntity<UserResponse> registered =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest(email, password, "Alice"),
            UserResponse.class);
    assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(registered.getBody()).isNotNull();
    assertThat(registered.getBody().email()).isEqualTo(email);
    assertThat(registered.getBody().roles()).containsExactly("CUSTOMER");

    ResponseEntity<TokenResponse> loggedIn =
        restTemplate.postForEntity(
            "/api/v1/auth/login", new LoginRequest(email, password), TokenResponse.class);
    assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loggedIn.getBody()).isNotNull();
    String accessToken = loggedIn.getBody().accessToken();
    String refreshToken = loggedIn.getBody().refreshToken();
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<UserResponse> me =
        restTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);
    assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(me.getBody()).isNotNull();
    assertThat(me.getBody().email()).isEqualTo(email);

    ResponseEntity<TokenResponse> refreshed =
        restTemplate.postForEntity(
            "/api/v1/auth/refresh", new RefreshRequest(refreshToken), TokenResponse.class);
    assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshed.getBody()).isNotNull();
    assertThat(refreshed.getBody().refreshToken()).isNotEqualTo(refreshToken);

    ResponseEntity<String> reused =
        restTemplate.postForEntity(
            "/api/v1/auth/refresh", new RefreshRequest(refreshToken), String.class);
    assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(reused.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void rejectsDuplicateEmailAndShortPassword() {
    String email = "bob-" + UUID.randomUUID() + "@example.com";
    RegisterRequest valid = new RegisterRequest(email, "correct-horse-battery", "Bob");

    assertThat(
            restTemplate
                .postForEntity("/api/v1/auth/register", valid, UserResponse.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(
            restTemplate
                .postForEntity("/api/v1/auth/register", valid, String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    ResponseEntity<String> tooShort =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            new RegisterRequest("carl-" + UUID.randomUUID() + "@example.com", "short", "Carl"),
            String.class);
    assertThat(tooShort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(tooShort.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
  }
}
