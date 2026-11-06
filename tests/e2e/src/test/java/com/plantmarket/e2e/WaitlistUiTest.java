package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.plantmarket.e2e.support.PlaywrightTestBase;
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

@Feature("J2 restock UI")
@Story("Sold-out plant shows a restock subscription form")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class WaitlistUiTest extends PlaywrightTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void soldOutPlantShowsRestockForm() throws Exception {
    String adminToken = ApiTokens.admin(apiUrl);
    String productId = findProductId(adminToken);
    String code = "WL-" + Long.toString(System.currentTimeMillis(), 36);
    JsonNode plant =
        post(
            "/api/v1/products/" + productId + "/individual-plants",
            adminToken,
            Map.of(
                "code", code,
                "description", "Sold-out plant for the waitlist UI",
                "priceMinor", 1500,
                "currency", "BYN",
                "careLevel", "EASY",
                "light", "MEDIUM",
                "petSafe", true));
    assertThat(plant.path("status").asString()).isEqualTo("ACQUIRED");

    page.navigate(baseUrl + "/plants/" + plant.path("id").asString());
    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Появится снова?"))
        .waitFor();
    page.getByLabel("E-mail").fill("waitlist-ui@example.test");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Сообщить о поступлении"))
        .click();
    page.getByText("Готово: напишем на waitlist-ui@example.test").waitFor();
  }

  private String findProductId(String token) throws Exception {
    JsonNode products = get("/api/v1/products?size=100", token);
    for (JsonNode item : products.path("items")) {
      if ("PLANT-001".equals(item.path("sku").asString())) {
        return item.path("id").asString();
      }
    }
    throw new IllegalStateException("Seed product PLANT-001 is missing");
  }

  private JsonNode get(String path, String token) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl + path)).GET();
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    HttpResponse<String> response =
        client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("response body: %s", response.body()).isBetween(200, 299);
    return OBJECT_MAPPER.readTree(response.body());
  }

  private JsonNode post(String path, String token, Map<String, Object> body) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(apiUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)));
    if (token != null) {
      builder.header("Authorization", "Bearer " + token);
    }
    HttpResponse<String> response =
        client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("response body: %s", response.body()).isBetween(200, 299);
    return OBJECT_MAPPER.readTree(response.body());
  }
}
