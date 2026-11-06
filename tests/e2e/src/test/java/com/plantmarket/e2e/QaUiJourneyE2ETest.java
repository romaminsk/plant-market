package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Feature("QA storefront and admin")
@Story("QA users complete the customer journey and the operator claim flow")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class QaUiJourneyE2ETest extends PlaywrightTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");
  private final String adminUrl = env("E2E_ADMIN_URL", "http://localhost:5173");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void qaCustomerCompletesStorefrontJourney() throws Exception {
    page.navigate(baseUrl + "/catalog");
    acceptEssentialCookies();
    page.locator(".plant-card").first().waitFor();
    assertThat(page.locator(".plant-card").count()).isGreaterThan(0);

    JsonNode qaPlant = createAvailablePlant();
    String plantId = qaPlant.path("id").asString();
    String code = qaPlant.path("code").asString();
    page.navigate(baseUrl + "/plants/" + plantId);
    page.getByTestId("plant-price").waitFor();
    assertThat(page.getByTestId("plant-price").innerText()).contains("10,00");
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("В корзину"))
        .click();
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("В корзине"))
        .waitFor();

    page.navigate(baseUrl + "/cart");
    page.getByText(code).waitFor();
    page.getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Перейти к оформлению"))
        .click();

    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Нужно войти"))
        .waitFor();
    page.getByRole(AriaRole.STATUS)
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Войти"))
        .click();

    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Вход").setExact(true))
        .waitFor();
    assertThat(page.locator("body").innerText()).contains("ЛИЧНЫЙ КАБИНЕТ");
    page.locator("input[name='username']").fill("qa.customer@plantmarket.local");
    page.locator("input[name='password']").fill("QaCustomer123!");
    page.locator("button[type='submit']").click();

    page.getByLabel("Адрес доставки").fill("Минск, Независимости 1");
    page.locator("select").selectOption(new SelectOption().setLabel("Минск · 1–2 дн."));
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Оплатить"))
        .click();
    page.getByText("Заказ оплачен").waitFor();
    String orderId = page.locator(".mono").innerText().trim();
    assertThat(orderId).isNotBlank();

    page.navigate(baseUrl + "/account/orders");
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Мои заказы"))
        .waitFor();
    String shortId = orderId.substring(orderId.length() - 8);
    assertThat(page.locator("body").innerText()).contains(shortId.toUpperCase());

    page.locator(".order-card")
        .filter(new Locator.FilterOptions().setHasText(shortId.toUpperCase()))
        .locator("h2 a")
        .click();
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Заказ " + shortId))
        .waitFor();
    assertThat(page.locator("body").innerText()).contains("Адрес доставки: Минск, Независимости 1");
  }

  @Test
  void brandedLoginPageRejectsWrongPasswordAndLogsOut() {
    page.navigate(baseUrl + "/api/auth/login?returnTo=/account/orders");
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Вход").setExact(true))
        .waitFor();

    page.locator("input[name='username']").fill("qa.customer@plantmarket.local");
    page.locator("input[name='password']").fill("WrongPassword123!");
    page.locator("button[type='submit']").click();
    page.getByText("Неверный e-mail или пароль").waitFor();

    page.locator("input[name='username']").fill("qa.customer@plantmarket.local");
    page.locator("input[name='password']").fill("QaCustomer123!");
    page.locator("button[type='submit']").click();
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Мои заказы"))
        .waitFor();

    page.getByRole(
            AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("Выйти"))
        .click();
    page.waitForURL("**/catalog");
    assertThat(
            page.getByRole(
                    AriaRole.LINK,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Войти"))
                .isVisible())
        .isTrue();
  }

  @Test
  void qaOperatorResolvesClaimInAdminUi() throws Exception {
    String claimId = createQaClaim();
    String shortId = claimId.substring(claimId.length() - 8);

    AdminPage admin = new AdminPage(page);
    admin.open(adminUrl);
    admin.login("qa.operator@plantmarket.local", "QaOperator123!");
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

  private JsonNode createAvailablePlant() throws Exception {
    String token = ApiTokens.admin(apiUrl);
    String productId = SeedData.productId(apiUrl, "PLANT-001");
    String code = "QA-" + UUID.randomUUID().toString().substring(0, 8);
    JsonNode plant =
        post(
            "/api/v1/products/" + productId + "/individual-plants",
            token,
            Map.of(
                "code", code,
                "description", "QA E2E plant",
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
    return post(
        "/api/v1/individual-plants/" + plantId + "/transitions",
        token,
        Map.of("status", "AVAILABLE"));
  }

  private String createQaClaim() throws Exception {
    String token = ApiTokens.qaCustomer(apiUrl);
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
                "QA damaged leaf on delivery",
                "photoUrls",
                List.of("https://example.test/qa-claim-photo.jpg")));
    assertThat(claim.path("status").asString()).isEqualTo("OPEN");
    return claim.path("id").asString();
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
