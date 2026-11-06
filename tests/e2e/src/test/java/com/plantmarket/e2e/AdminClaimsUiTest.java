package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.plantmarket.e2e.pages.AdminPage;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Feature("Admin claims")
@Story("Operator resolves a damaged plant claim")
@EnabledIfEnvironmentVariable(named = "E2E_ADMIN_URL", matches = ".+")
class AdminClaimsUiTest extends PlaywrightTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String adminUrl = env("E2E_ADMIN_URL", "http://localhost:5173");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void operatorResolvesClaimInAdminUi() throws Exception {
    String claimId = createClaim();
    String shortId = claimId.substring(claimId.length() - 8);

    AdminPage admin = new AdminPage(page);
    admin.openWithToken(adminUrl, ApiTokens.admin(apiUrl));
    admin.waitForProducts();
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Претензии"))
        .click();

    Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(shortId));
    row.waitFor();
    row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Решить")).click();
    row.getByText("REPLACEMENT").waitFor();
  }

  private String createClaim() throws Exception {
    String token = ApiTokens.customer(apiUrl);
    String variantId = SeedData.variantId(apiUrl);
    JsonNode order =
        post(
            "/api/v1/orders",
            token,
            Map.of("items", List.of(Map.of("variantId", variantId, "quantity", 1))));
    String orderId = order.path("id").asString();
    post("/api/v1/orders/" + orderId + "/checkout", token, Map.of());
    post("/api/v1/orders/" + orderId + "/pay", token, Map.of());
    JsonNode claim =
        post(
            "/api/v1/orders/" + orderId + "/claims",
            token,
            Map.of(
                "reason",
                "Damaged leaf on delivery",
                "photoUrls",
                List.of("https://example.test/claim-photo.jpg")));
    assertThat(claim.path("status").asString()).isEqualTo("OPEN");
    return claim.path("id").asString();
  }

  private String seedVariantId() throws Exception {
    JsonNode products = get("/api/v1/products?size=100");
    for (JsonNode item : products.path("items")) {
      if ("MON-001".equals(item.path("sku").asString())) {
        JsonNode variants = get("/api/v1/products/" + item.path("id").asString() + "/variants");
        return variants.get(0).path("id").asString();
      }
    }
    throw new IllegalStateException("Seed product MON-001 is missing");
  }

  private JsonNode get(String path) throws Exception {
    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(URI.create(apiUrl + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("response body: %s", response.body()).isBetween(200, 299);
    return OBJECT_MAPPER.readTree(response.body());
  }

  private JsonNode post(String path, String token, Map<String, Object> body) throws Exception {
    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(URI.create(apiUrl + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("response body: %s", response.body()).isBetween(200, 299);
    return OBJECT_MAPPER.readTree(response.body());
  }
}
