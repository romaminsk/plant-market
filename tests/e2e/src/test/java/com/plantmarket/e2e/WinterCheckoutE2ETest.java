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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Feature("J3 winter delivery")
@Story("Gomel checkout requires thermal packaging and charges for it")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class WinterCheckoutE2ETest extends PlaywrightTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void gomelCheckoutRequiresThermalPackaging() throws Exception {
    String plantId = createAvailablePlant();
    page.navigate(baseUrl + "/plants/" + plantId);
    acceptEssentialCookies();
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("В корзину"))
        .click();
    page.getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Корзина (1)"))
        .click();
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

    page.getByLabel("Адрес доставки").fill("Гомель, Советская 1");
    page.locator("select").selectOption(new SelectOption().setLabel("Гомель (зима) · 2–4 дн."));
    assertThat(
            page.getByRole(
                    AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Оплатить"))
                .isDisabled())
        .isTrue();
    page.getByText("Для этой зоны обязательна термоупаковка.").waitFor();

    page.locator("input[type='checkbox']").check();
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Оплатить"))
        .click();
    page.getByText("Заказ оплачен").waitFor();
    page.getByRole(AriaRole.STATUS)
        .getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Мои заказы"))
        .click();
    page.locator(".order-card h2 a").first().click();
    page.getByText("Термоупаковка включена.").waitFor();
  }

  private String createAvailablePlant() throws Exception {
    String token = ApiTokens.admin(apiUrl);
    String productId = SeedData.productId(apiUrl, "PLANT-001");
    String code = "WIN-" + UUID.randomUUID().toString().substring(0, 8);
    JsonNode plant =
        post(
            "/api/v1/products/" + productId + "/individual-plants",
            token,
            Map.of(
                "code", code,
                "description", "Winter E2E plant",
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
    post(
        "/api/v1/individual-plants/" + plantId + "/transitions",
        token,
        Map.of("status", "AVAILABLE"));
    return plantId;
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
