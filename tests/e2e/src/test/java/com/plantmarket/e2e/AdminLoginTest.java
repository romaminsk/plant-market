package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.plantmarket.e2e.pages.AdminPage;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Feature("Admin")
@Story("Login and reuse storage state")
@EnabledIfEnvironmentVariable(named = "E2E_ADMIN_URL", matches = ".+")
class AdminLoginTest extends PlaywrightTestBase {

  private final String adminUrl = env("E2E_ADMIN_URL", "http://localhost:5173");
  private final String email = env("E2E_ADMIN_EMAIL", "admin@plantmarket.local");
  private final String password = env("E2E_ADMIN_PASSWORD", "AdminPassword123!");

  @Test
  void loginAndReuseStorageState() throws Exception {
    AdminPage admin = new AdminPage(page).open(adminUrl);
    admin.login(email, password);
    admin.waitForProducts();
    assertThat(admin.productRows().count()).isGreaterThan(0);

    Path storage = Path.of("build", "e2e-artifacts", "admin-storage.json");
    Files.createDirectories(storage.getParent());
    context.storageState(new BrowserContext.StorageStateOptions().setPath(storage));

    BrowserContext reused =
        browser.newContext(
            new Browser.NewContextOptions()
                .setStorageStatePath(storage)
                .setViewportSize(1280, 800));
    try {
      Page reusedPage = reused.newPage();
      AdminPage reusedAdmin = new AdminPage(reusedPage).open(adminUrl);
      reusedAdmin.waitForProducts();
      assertThat(reusedAdmin.productRows().count()).isGreaterThan(0);
    } finally {
      reused.close();
    }
  }
}
