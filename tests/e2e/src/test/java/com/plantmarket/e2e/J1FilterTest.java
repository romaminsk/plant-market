package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Feature("J1 catalog")
@Story("Filter variegated medium light under 150 BYN and open WYSIWYG card")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class J1FilterTest extends PlaywrightTestBase {

  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");

  @Test
  void filterFindsVariegatedPlantAndShowsWysiwygPrice() {
    page.navigate(baseUrl + "/catalog?variegated=true&light=MEDIUM&maxPrice=150");
    acceptEssentialCookies();

    Locator plantCard =
        page.locator(".plant-card").filter(new Locator.FilterOptions().setHasText("IP-001"));
    plantCard.waitFor();
    assertThat(plantCard.innerText()).contains("Philodendron Pink Princess");

    plantCard
        .getByRole(
            AriaRole.LINK,
            new Locator.GetByRoleOptions().setName("Philodendron Pink Princess").setExact(true))
        .click();

    Locator price = page.getByTestId("plant-price");
    price.waitFor();

    assertThat(page.url()).endsWith("/plants/IP-001");
    assertThat(page.locator("body").innerText()).contains("WYSIWYG");
    assertThat(price.innerText().replaceAll("[^0-9]", "")).contains("12000");
  }
}
