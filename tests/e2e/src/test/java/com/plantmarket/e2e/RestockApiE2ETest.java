package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Feature("J2 restock")
@Story("Sold-out subscription, restock notification and purchase")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class RestockApiE2ETest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String apiUrl =
      System.getenv().getOrDefault("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();
  private static String cachedCustomerToken;
  private static String cachedAdminToken;

  @Test
  void soldOutSubscriptionNotifiesAndAllowsPurchase() throws Exception {
    String suffix = Long.toString(System.currentTimeMillis(), 36);
    String adminToken = adminLogin();

    JsonNode category =
        post(
            "/api/v1/categories",
            adminToken,
            Map.of(
                "name", "E2E restock " + suffix,
                "slug", "e2e-restock-" + suffix,
                "description", "Temporary category for J2"));
    String categoryId = category.path("id").asString();

    Map<String, Object> product = new LinkedHashMap<>();
    product.put("sku", "E2E-RS-" + suffix);
    product.put("name", "E2E restock plant");
    product.put("slug", "e2e-restock-plant-" + suffix);
    product.put("description", "Temporary product for J2");
    product.put("categoryId", categoryId);
    product.put("priceMinor", 2500);
    product.put("currency", "BYN");
    product.put("status", "PUBLISHED");
    product.put("careLevel", "EASY");
    product.put("light", "MEDIUM");
    product.put("petSafe", true);
    product.put("variegated", false);
    String productId = post("/api/v1/products", adminToken, product).path("id").asString();

    JsonNode variant =
        post(
            "/api/v1/products/" + productId + "/variants",
            adminToken,
            Map.of(
                "sku", "E2E-RS-SKU-" + suffix,
                "name", "E2E restock variant",
                "priceMinor", 2500,
                "currency", "BYN",
                "status", "ACTIVE"));
    String variantId = variant.path("id").asString();

    String email = "restock-e2e-" + suffix + "@example.test";
    JsonNode subscription =
        post(
            "/api/v1/restock-subscriptions",
            null,
            Map.of("targetType", "VARIANT", "targetId", variantId, "email", email));
    assertThat(subscription.path("status").asString()).isEqualTo("PENDING");
    String subscriptionId = subscription.path("id").asString();

    JsonNode batch =
        post(
            "/api/v1/stock-batches",
            adminToken,
            Map.of(
                "productVariantId",
                variantId,
                "supplier",
                "E2E Supplier",
                "quantity",
                2,
                "unitCostMinor",
                1000,
                "currency",
                "BYN",
                "quarantineUntil",
                Instant.now().plus(1, ChronoUnit.DAYS).toString()));
    post("/api/v1/stock-batches/" + batch.path("id").asString() + "/release", adminToken, Map.of());

    JsonNode notified = subscription(subscriptionId);
    for (int attempt = 0;
        attempt < 10 && !"NOTIFIED".equals(notified.path("status").asString());
        attempt++) {
      Thread.sleep(200);
      notified = subscription(subscriptionId);
    }
    assertThat(notified.path("status").asString()).isEqualTo("NOTIFIED");
    assertThat(notified.path("notifiedAt").isNull()).isFalse();

    HttpResponse<String> alreadyInStock =
        postRaw(
            "/api/v1/restock-subscriptions",
            null,
            Map.of("targetType", "VARIANT", "targetId", variantId, "email", email));
    assertThat(alreadyInStock.statusCode()).isEqualTo(409);

    String customerToken = customerLogin();
    JsonNode order =
        post(
            "/api/v1/orders",
            customerToken,
            Map.of("items", List.of(Map.of("variantId", variantId, "quantity", 1))));
    JsonNode checkedOut =
        post(
            "/api/v1/orders/" + order.path("id").asString() + "/checkout", customerToken, Map.of());
    assertThat(checkedOut.path("status").asString()).isEqualTo("PENDING_PAYMENT");
    post("/api/v1/orders/" + order.path("id").asString() + "/cancel", customerToken, Map.of());
  }

  private JsonNode subscription(String id) throws Exception {
    return get("/api/v1/restock-subscriptions/" + id, null);
  }

  private String customerLogin() throws Exception {
    if (cachedCustomerToken != null) {
      return cachedCustomerToken;
    }
    JsonNode response =
        post(
            "/api/v1/auth/login",
            null,
            Map.of("email", "customer@plantmarket.local", "password", "CustomerPassword123!"));
    cachedCustomerToken = response.path("accessToken").asString();
    return cachedCustomerToken;
  }

  private String adminLogin() throws Exception {
    if (cachedAdminToken != null) {
      return cachedAdminToken;
    }
    JsonNode response =
        post(
            "/api/v1/auth/login",
            null,
            Map.of("email", "admin@plantmarket.local", "password", "AdminPassword123!"));
    cachedAdminToken = response.path("accessToken").asString();
    return cachedAdminToken;
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
