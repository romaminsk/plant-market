package com.plantmarket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OAuth2PkceIntegrationTest {

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
  private static final Pattern CSRF_PATTERN =
      Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

  @LocalServerPort private int port;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void authorizationCodeWithPkceAndRefreshRotation() throws Exception {
    CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    String email = "oauth-" + UUID.randomUUID() + "@example.com";
    String password = "correct-horse-battery";
    HttpResponse<String> registered =
        postJson(
            client,
            "/api/v1/auth/register",
            OBJECT_MAPPER.writeValueAsString(
                java.util.Map.of("email", email, "password", password, "fullName", "OAuth User")));
    assertThat(registered.statusCode()).isEqualTo(201);

    String verifier = randomVerifier();
    String challenge = challenge(verifier);
    String redirectUri = "http://localhost:3000/auth/callback";
    String authorizePath =
        "/oauth2/authorize?response_type=code&client_id=storefront&redirect_uri="
            + encode(redirectUri)
            + "&scope=catalog.read&code_challenge="
            + challenge
            + "&code_challenge_method=S256&state=test-state";

    HttpResponse<String> unauthenticated = get(client, authorizePath);
    assertThat(unauthenticated.statusCode()).isEqualTo(302);
    assertThat(unauthenticated.headers().firstValue("Location").orElseThrow()).endsWith("/login");

    HttpResponse<String> loginPage = get(client, "/login");
    assertThat(loginPage.statusCode()).isEqualTo(200);
    assertThat(loginPage.body()).contains("Личный кабинет", "name=\"username\"");
    assertThat(get(client, "/fonts/manrope-400-latin.woff2").statusCode()).isEqualTo(200);
    Matcher csrf = CSRF_PATTERN.matcher(loginPage.body());
    assertThat(csrf.find()).isTrue();

    String loginBody =
        "username="
            + encode(email)
            + "&password="
            + encode(password)
            + "&_csrf="
            + encode(csrf.group(1));
    HttpResponse<String> loggedIn = postForm(client, "/login", loginBody);
    assertThat(loggedIn.statusCode()).isEqualTo(302);

    HttpResponse<String> authorized = get(client, authorizePath);
    assertThat(authorized.statusCode()).isEqualTo(302);
    String location = authorized.headers().firstValue("Location").orElseThrow();
    assertThat(location).startsWith(redirectUri);
    String code = queryParameter(location, "code");
    assertThat(code).isNotBlank();

    String tokenBody =
        "grant_type=authorization_code&client_id=storefront&code="
            + encode(code)
            + "&redirect_uri="
            + encode(redirectUri)
            + "&code_verifier="
            + encode(verifier);
    HttpResponse<String> tokens = postForm(client, "/oauth2/token", tokenBody);
    assertThat(tokens.statusCode()).isEqualTo(200);
    JsonNode tokenJson = OBJECT_MAPPER.readTree(tokens.body());
    String accessToken = tokenJson.path("access_token").asString();
    String refreshToken = tokenJson.path("refresh_token").asString();
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from oauth2_authorization where principal_name = ?",
                Integer.class,
                email))
        .isEqualTo(1);

    HttpResponse<String> me =
        client.send(
            HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/auth/me"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(me.statusCode()).isEqualTo(200);
    assertThat(me.body()).contains(email);

    HttpResponse<String> refreshed =
        postForm(
            client,
            "/oauth2/token",
            "grant_type=refresh_token&client_id=storefront&refresh_token=" + encode(refreshToken));
    assertThat(refreshed.statusCode()).isEqualTo(200);
    JsonNode refreshedJson = OBJECT_MAPPER.readTree(refreshed.body());
    assertThat(refreshedJson.path("access_token").asString()).isNotBlank();
    assertThat(refreshedJson.path("refresh_token").asString())
        .isNotBlank()
        .isNotEqualTo(refreshToken);

    HttpResponse<String> reused =
        postForm(
            client,
            "/oauth2/token",
            "grant_type=refresh_token&client_id=storefront&refresh_token=" + encode(refreshToken));
    assertThat(reused.statusCode()).isEqualTo(400);
  }

  private HttpResponse<String> get(HttpClient client, String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> postForm(HttpClient client, String path, String body)
      throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> postJson(HttpClient client, String path, String body)
      throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }

  private static String randomVerifier() {
    byte[] bytes = new byte[48];
    new java.security.SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String challenge(String verifier) throws Exception {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }

  private static String queryParameter(String location, String name) {
    Matcher matcher = Pattern.compile("[?&]" + name + "=([^&]*)").matcher(location);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
