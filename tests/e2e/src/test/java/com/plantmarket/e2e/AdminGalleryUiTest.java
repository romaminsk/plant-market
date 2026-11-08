package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.plantmarket.e2e.pages.AdminPage;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

@Feature("Admin media")
@Story("Operator edits the WYSIWYG gallery of an individual plant")
@EnabledIfEnvironmentVariable(named = "E2E_ADMIN_URL", matches = ".+")
class AdminGalleryUiTest extends PlaywrightTestBase {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String adminUrl = env("E2E_ADMIN_URL", "http://localhost:5173");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void operatorSavesPlantGallery() throws Exception {
    AdminPage admin = new AdminPage(page);
    admin.openWithToken(adminUrl, ApiTokens.admin(apiUrl));
    admin.waitForProducts();

    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Экземпляры"))
        .click();
    Locator row =
        page.locator("tr").filter(new Locator.FilterOptions().setHasText("IP-001")).first();
    row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Редактировать")).click();

    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Галерея WYSIWYG"))
        .waitFor();
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Сохранить галерею"))
        .click();
    page.getByText("Галерея сохранена").waitFor();
  }

  @Test
  void switchingPlantsDoesNotKeepPreviousGalleryUrls() throws Exception {
    AdminPage admin = new AdminPage(page);
    admin.openWithToken(adminUrl, ApiTokens.admin(apiUrl));
    admin.waitForProducts();

    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Экземпляры"))
        .click();

    editPlant("IP-012");
    String firstGeneral = generalPhotoInput().inputValue();
    assertThat(firstGeneral).contains("philodendron");

    editPlant("IP-011");
    String secondGeneral = generalPhotoInput().inputValue();
    assertThat(secondGeneral).contains("anthurium");
  }

  private void editPlant(String code) {
    page.locator("tr")
        .filter(new Locator.FilterOptions().setHasText(code))
        .first()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Редактировать"))
        .click();
    page.getByRole(
            AriaRole.HEADING,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Галерея WYSIWYG"))
        .waitFor();
  }

  private Locator generalPhotoInput() {
    return page.locator(".gallery-editor-row input").first();
  }
}
