package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
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

@Feature("J1 checkout")
@Story("Guest fills a cart, logs in with OAuth2 PKCE, pays and sees order history")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class CheckoutFlowE2ETest extends PlaywrightTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void guestCanLoginAndPayForCart() throws Exception {
    String firstPlant = createAvailablePlant();
    String secondPlant = createAvailablePlant();

    addToCart(firstPlant);
    addToCart(secondPlant);
    page.getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Корзина (2)"))
        .click();

    page.getByText("Checkout E2E plant").first().waitFor();
    page.getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Перейти к оформлению"))
        .click();

    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Нужно войти"))
        .waitFor();
    page.getByRole(AriaRole.STATUS)
        .getByRole(
            AriaRole.LINK, new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Войти"))
        .click();

    page.locator("input[name='username']").fill("customer@plantmarket.local");
    page.locator("input[name='password']").fill("CustomerPassword123!");
    page.locator("button[type='submit']").click();

    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Оплатить"))
        .click();
    page.locator("select").selectOption(new SelectOption().setLabel("Минск · 1–2 дн."));
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Оплатить"))
        .click();
    page.getByText("Заказ оплачен").waitFor();
    String orderId = page.locator(".mono").innerText().trim();
    assertThat(orderId).isNotBlank();

    page.getByRole(AriaRole.STATUS)
        .getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Мои заказы"))
        .click();
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Мои заказы"))
        .waitFor();
    page.getByText(orderId.substring(orderId.length() - 8)).first().waitFor();
    page.getByText("PAID").first().waitFor();
  }

  private void addToCart(String plantId) {
    page.navigate(baseUrl + "/plants/" + plantId);
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("В корзину"))
        .click();
  }

  private String createAvailablePlant() throws Exception {
    String token = ApiTokens.admin(apiUrl);
    String productId = findProductIdBySku("PLANT-001");
    String code = "CHK-" + Long.toString(System.currentTimeMillis(), 36);
    JsonNode plant =
        post(
            "/api/v1/products/" + productId + "/individual-plants",
            token,
            Map.of(
                "code", code,
                "description", "Checkout E2E plant",
                "priceMinor", 1000,
                "currency", "BYN",
                "careLevel", "EASY",
                "light", "MEDIUM",
                "petSafe", true));
    String plantId = plant.path("id").asString();
    post(
        "/api/v1/individual-plants/" + plantId + "/transitions",
        token,
        Map.of("status", "QUARANTINE"));
    JsonNode available =
        post(
            "/api/v1/individual-plants/" + plantId + "/transitions",
            token,
            Map.of("status", "AVAILABLE"));
    assertThat(available.path("status").asString()).isEqualTo("AVAILABLE");
    return plantId;
  }

  private String findProductIdBySku(String sku) throws Exception {
    String token = ApiTokens.admin(apiUrl);
    JsonNode products = get("/api/v1/products?size=100", token);
    for (JsonNode item : products.path("items")) {
      if (sku.equals(item.path("sku").asString())) {
        return item.path("id").asString();
      }
    }
    throw new IllegalStateException("Seed product " + sku + " is missing");
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
