package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.plantmarket.e2e.pages.CatalogPage;
import com.plantmarket.e2e.pages.ProductPage;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Feature("J1 catalog")
@Story("Smoke: catalog -> product card -> price")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class J1SmokeTest extends PlaywrightTestBase {

  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");

  @Test
  void catalogShowsSeededProductAndPrice() {
    CatalogPage catalog = new CatalogPage(page, baseUrl).open();
    acceptEssentialCookies();

    assertThat(catalog.productCards().count()).isGreaterThan(0);
    assertThat(catalog.productLink("Monstera Deliciosa").isVisible()).isTrue();

    catalog.productLink("Monstera Deliciosa").click();

    ProductPage product = new ProductPage(page);
    product.waitForLoaded();

    assertThat(page.url()).endsWith("/catalog/monstera-deliciosa");
    assertThat(product.heading()).isEqualTo("Monstera Deliciosa");
    assertThat(product.price().replaceAll("[^0-9]", "")).contains("1299");
  }
}
