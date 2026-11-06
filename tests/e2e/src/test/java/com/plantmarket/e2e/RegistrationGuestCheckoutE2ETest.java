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

@Feature("Registration and guest checkout")
@Story("A new customer registers and a guest pays without an account")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class RegistrationGuestCheckoutE2ETest extends PlaywrightTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void newCustomerRegistersAndLogsIn() {
    String email = "qa-reg-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    page.navigate(baseUrl + "/api/auth/login?returnTo=/catalog");
    acceptEssentialCookies();
    page.getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Зарегистрироваться"))
        .click();
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Регистрация"))
        .waitFor();
    page.locator("input[name='fullName']").fill("QA Registered Customer");
    page.locator("input[name='email']").fill(email);
    page.locator("input[name='password']").fill("QaRegister123!");
    page.locator("button[type='submit']").click();
    page.getByText("Аккаунт создан. Войдите.").waitFor();
    assertThat(page.locator("input[name='username']").inputValue()).isEqualTo(email);
    page.locator("input[name='password']").fill("QaRegister123!");
    page.locator("button[type='submit']").click();
    page.getByRole(
            AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("Выйти"))
        .waitFor();
    assertThat(page.url()).contains("/catalog");
  }

  @Test
  void guestPaysWithoutRegistration() throws Exception {
    String plantId = createAvailablePlant();
    page.navigate(baseUrl + "/catalog");
    acceptEssentialCookies();
    setCart(plantId);
    page.navigate(baseUrl + "/checkout");
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Оформление заказа"))
        .waitFor();
    page.locator("input[name='guestEmail']").fill("guest-checkout@example.test");
    page.locator(".checkout-form textarea").fill("Минск, Независимости 1");
    page.locator(".checkout-form select")
        .selectOption(new SelectOption().setLabel("Минск · 1–2 дн."));
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Оплатить"))
        .click();
    page.getByText("Заказ оплачен").waitFor();
    assertThat(page.locator(".mono").innerText()).isNotBlank();
    assertThat(page.locator("body").innerText())
        .contains("Подтверждение отправлено на guest-checkout@example.test");
  }

  @Test
  void guestEmailIsRequiredBeforePayment() throws Exception {
    String plantId = createAvailablePlant();
    page.navigate(baseUrl + "/catalog");
    acceptEssentialCookies();
    setCart(plantId);
    page.navigate(baseUrl + "/checkout");
    page.locator("input[name='guestEmail']").waitFor();
    page.locator(".checkout-form textarea").fill("Минск, Независимости 1");
    page.locator(".checkout-form select")
        .selectOption(new SelectOption().setLabel("Минск · 1–2 дн."));
    assertThat(page.locator("button:has-text('Оплатить')").isDisabled()).isTrue();
    page.locator("input[name='guestEmail']").fill("not-an-email");
    assertThat(page.locator("button:has-text('Оплатить')").isDisabled()).isTrue();
    page.locator("input[name='guestEmail']").fill("guest-valid@example.test");
    assertThat(page.locator("button:has-text('Оплатить')").isDisabled()).isFalse();
  }

  private void setCart(String plantId) {
    page.evaluate(
        "item => localStorage.setItem('pm.cart.v1', JSON.stringify([item]))",
        Map.of(
            "type",
            "INDIVIDUAL_PLANT",
            "id",
            plantId,
            "name",
            "Guest checkout plant",
            "priceMinor",
            12000,
            "currency",
            "BYN",
            "quantity",
            1));
  }

  private String createAvailablePlant() throws Exception {
    String token = ApiTokens.admin(apiUrl);
    String productId = SeedData.productId(apiUrl, "PLANT-001");
    String code = "GUEST-" + UUID.randomUUID().toString().substring(0, 8);
    JsonNode plant =
        post(
            "/api/v1/products/" + productId + "/individual-plants",
            token,
            Map.of(
                "code", code,
                "description", "Guest checkout plant",
                "priceMinor", 12000,
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
