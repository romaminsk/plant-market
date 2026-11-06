package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Feature("J5 orders")
@Story("Create order, reserve stock, cancel and release")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class OrderApiE2ETest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String apiUrl =
      System.getenv().getOrDefault("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void createCheckoutAndCancelOrderReleasesReservation() throws Exception {
    String token = login();
    String variantId = SeedData.variantId(apiUrl);
    int availableBefore =
        get("/api/v1/variants/" + variantId, null).path("availableQuantity").asInt();

    JsonNode created =
        post(
            "/api/v1/orders",
            token,
            Map.of("items", java.util.List.of(Map.of("variantId", variantId, "quantity", 1))));
    assertThat(created.path("status").asString()).isEqualTo("DRAFT");
    String orderId = created.path("id").asString();

    JsonNode checkedOut = post("/api/v1/orders/" + orderId + "/checkout", token, Map.of());
    assertThat(checkedOut.path("status").asString()).isEqualTo("PENDING_PAYMENT");
    assertThat(checkedOut.path("reservationExpiresAt").asString()).isNotBlank();

    JsonNode cancelled = post("/api/v1/orders/" + orderId + "/cancel", token, Map.of());
    assertThat(cancelled.path("status").asString()).isEqualTo("CANCELLED");
    int availableAfter =
        get("/api/v1/variants/" + variantId, null).path("availableQuantity").asInt();
    assertThat(availableAfter).isEqualTo(availableBefore);

    JsonNode orders = get("/api/v1/orders", token);
    boolean found = false;
    for (JsonNode order : orders) {
      if (orderId.equals(order.path("id").asString())) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();
  }

  @Test
  void payOrderThroughSandboxProvider() throws Exception {
    String token = login();
    String variantId = seedVariantId();
    JsonNode created =
        post(
            "/api/v1/orders",
            token,
            Map.of("items", java.util.List.of(Map.of("variantId", variantId, "quantity", 1))));
    String orderId = created.path("id").asString();

    JsonNode checkedOut = post("/api/v1/orders/" + orderId + "/checkout", token, Map.of());
    assertThat(checkedOut.path("status").asString()).isEqualTo("PENDING_PAYMENT");

    JsonNode paid = post("/api/v1/orders/" + orderId + "/pay", token, Map.of());
    assertThat(paid.path("status").asString()).isEqualTo("PAID");
  }

  @Test
  void openClaimAndOperatorDecision() throws Exception {
    String token = login();
    String variantId = seedVariantId();
    JsonNode created =
        post(
            "/api/v1/orders",
            token,
            Map.of("items", java.util.List.of(Map.of("variantId", variantId, "quantity", 1))));
    String orderId = created.path("id").asString();
    post("/api/v1/orders/" + orderId + "/checkout", token, Map.of());
    JsonNode paid = post("/api/v1/orders/" + orderId + "/pay", token, Map.of());
    assertThat(paid.path("status").asString()).isEqualTo("PAID");

    HttpResponse<String> claimResponse =
        postRaw(
            "/api/v1/orders/" + orderId + "/claims",
            token,
            Map.of(
                "reason",
                "Damaged leaf on delivery",
                "photoUrls",
                java.util.List.of("https://example.test/photo-1.jpg")));
    assertThat(claimResponse.statusCode()).isEqualTo(201);
    JsonNode claim = OBJECT_MAPPER.readTree(claimResponse.body());
    assertThat(claim.path("status").asString()).isEqualTo("OPEN");
    String claimId = claim.path("id").asString();

    String adminToken = adminLogin();
    JsonNode decided =
        post(
            "/api/v1/claims/" + claimId + "/decision",
            adminToken,
            Map.of("decision", "REPLACEMENT", "comment", "Replacement approved"));
    assertThat(decided.path("status").asString()).isEqualTo("REPLACEMENT");
    assertThat(decided.path("decidedAt").isNull()).isFalse();
  }

  @Test
  void careReminderOptInAndOptOut() throws Exception {
    String token = login();
    String variantId = seedVariantId();
    JsonNode created =
        post(
            "/api/v1/orders",
            token,
            Map.of("items", java.util.List.of(Map.of("variantId", variantId, "quantity", 1))));
    String orderId = created.path("id").asString();
    post("/api/v1/orders/" + orderId + "/checkout", token, Map.of());
    post("/api/v1/orders/" + orderId + "/pay", token, Map.of());

    JsonNode reminders =
        post("/api/v1/orders/" + orderId + "/care-reminders", token, Map.of("intervalDays", 14));
    assertThat(reminders.size()).isEqualTo(1);
    String reminderId = reminders.get(0).path("id").asString();
    assertThat(reminders.get(0).path("active").asBoolean()).isTrue();
    assertThat(reminders.get(0).path("intervalDays").asInt()).isEqualTo(14);

    JsonNode listed = get("/api/v1/orders/" + orderId + "/care-reminders", token);
    assertThat(listed.size()).isEqualTo(1);

    HttpResponse<String> deleted = delete("/api/v1/care-reminders/" + reminderId, token);
    assertThat(deleted.statusCode()).isEqualTo(204);

    JsonNode afterDelete = get("/api/v1/orders/" + orderId + "/care-reminders", token);
    assertThat(afterDelete.size()).isZero();
  }

  @Test
  void winterZoneRequiresThermalPackaging() throws Exception {
    String token = login();
    String variantId = seedVariantId();
    JsonNode zones = get("/api/v1/shipping/zones", null);
    String winterZoneId = "";
    for (JsonNode zone : zones) {
      if ("Гомель".equals(zone.path("city").asString())) {
        winterZoneId = zone.path("id").asString();
        break;
      }
    }
    assertThat(winterZoneId).isNotBlank();

    JsonNode created =
        post(
            "/api/v1/orders",
            token,
            Map.of("items", java.util.List.of(Map.of("variantId", variantId, "quantity", 1))));
    String orderId = created.path("id").asString();

    HttpResponse<String> rejected =
        postRaw(
            "/api/v1/orders/" + orderId + "/checkout",
            token,
            Map.of(
                "shippingZoneId",
                winterZoneId,
                "thermalPack",
                false,
                "deliveryAddress",
                "Gomel, Sovetskaya 1"));
    assertThat(rejected.statusCode()).isEqualTo(409);

    JsonNode accepted =
        post(
            "/api/v1/orders/" + orderId + "/checkout",
            token,
            Map.of(
                "shippingZoneId",
                winterZoneId,
                "thermalPack",
                true,
                "deliveryAddress",
                "Gomel, Sovetskaya 1"));
    assertThat(accepted.path("status").asString()).isEqualTo("PENDING_PAYMENT");
    assertThat(accepted.path("thermalPack").asBoolean()).isTrue();
    assertThat(accepted.path("shippingMinor").asLong()).isEqualTo(500L);

    post("/api/v1/orders/" + orderId + "/cancel", token, Map.of());
  }

  private String seedVariantId() throws Exception {
    return SeedData.variantId(apiUrl);
  }

  private String login() throws Exception {
    return ApiTokens.customer(apiUrl);
  }

  private String adminLogin() throws Exception {
    return ApiTokens.admin(apiUrl);
  }

  private HttpResponse<String> delete(String path, String token) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl + path)).DELETE();
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode get(String path, String token) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl + path)).GET();
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return send(builder.build());
  }

  private JsonNode post(String path, String token, Map<String, Object> body) throws Exception {
    HttpResponse<String> response = postRaw(path, token, body);
    assertThat(response.statusCode()).as("response body: %s", response.body()).isBetween(200, 299);
    return OBJECT_MAPPER.readTree(response.body());
  }

  private HttpResponse<String> postRaw(String path, String token, Map<String, Object> body)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(apiUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private JsonNode send(HttpRequest request) throws Exception {
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("response body: %s", response.body()).isBetween(200, 299);
    return OBJECT_MAPPER.readTree(response.body());
  }
}
