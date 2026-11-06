package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Feature("J1 catalog filters")
@Story("Filter variegated medium-light plants under 150 BYN")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class CatalogFilterE2ETest extends PlaywrightTestBase {

  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");

  @Test
  void filtersNarrowTheCatalogAndReset() {
    page.navigate(baseUrl + "/catalog");
    acceptEssentialCookies();
    page.locator(".plant-card").first().waitFor();
    assertThat(page.locator(".plant-card").count()).isEqualTo(11);
    assertThat(
            page.getByRole(
                    AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions()
                        .setName("Только вариегатные"))
                .getAttribute("aria-pressed"))
        .isEqualTo("false");
    assertThat(
            page.getByRole(
                    AriaRole.BUTTON,
                    new com.microsoft.playwright.Page.GetByRoleOptions().setName("Любой"))
                .getAttribute("aria-pressed"))
        .isEqualTo("true");
    page.screenshot(
        new com.microsoft.playwright.Page.ScreenshotOptions()
            .setPath(java.nio.file.Path.of("build", "e2e-artifacts", "catalog-all.png"))
            .setFullPage(true));

    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Только вариегатные"))
        .click();
    page.waitForFunction("() => new URL(location.href).searchParams.get('variegated') === 'true'");
    assertThat(page.locator(".plant-card").count()).isEqualTo(6);

    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Средний"))
        .click();
    page.waitForFunction("() => new URL(location.href).searchParams.get('light') === 'MEDIUM'");
    assertThat(page.locator(".plant-card").count()).isEqualTo(3);

    page.getByLabel("Цена до, BYN").fill("150");
    page.getByRole(
            AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions().setName("ОК"))
        .click();
    page.waitForFunction("() => new URL(location.href).searchParams.get('maxPrice') === '150'");
    assertThat(page.locator(".plant-card").count()).isEqualTo(1);
    assertThat(page.locator(".plant-card").first().innerText()).contains("IP-001");

    page.screenshot(
        new com.microsoft.playwright.Page.ScreenshotOptions()
            .setPath(java.nio.file.Path.of("build", "e2e-artifacts", "catalog-filters.png"))
            .setFullPage(true));

    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Сбросить фильтры"))
        .click();
    page.waitForFunction("() => location.search === ''");
    Locator cards = page.locator(".plant-card");
    assertThat(cards.count()).isEqualTo(11);
  }
}
