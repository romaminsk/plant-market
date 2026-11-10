package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.AriaRole;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Feature("A12 consent and analytics")
@Story("Analytics events are emitted only after explicit cookie consent")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class ConsentAnalyticsE2ETest extends PlaywrightTestBase {

  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");

  @Test
  void analyticsRequiresConsent() throws Exception {
    page.navigate(baseUrl + "/catalog");
    page.getByRole(
            AriaRole.DIALOG,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Cookie и аналитика"))
        .waitFor();
    String consentVersion = page.getAttribute("aside.cookie-banner", "data-consent-version");
    assertThat(consentVersion).isNotBlank();
    Object beforeConsent = page.evaluate("() => (window.dataLayer ?? []).length");
    assertThat(((Number) beforeConsent).intValue()).isZero();

    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Принять аналитику"))
        .click();

    Object meta = page.evaluate("() => JSON.parse(localStorage.getItem('pm.consent.meta.v1'))");
    assertThat(meta).isInstanceOf(java.util.Map.class);
    java.util.Map<?, ?> record = (java.util.Map<?, ?>) meta;
    assertThat(record.get("version")).isEqualTo(consentVersion);
    assertThat(record.get("acceptedAt")).isNotNull();

    String productId = SeedData.productId(apiUrl, "MON-001");
    page.navigate(baseUrl + "/catalog/" + productId);
    page.getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("В корзину"))
        .first()
        .click();

    Object tracked =
        page.evaluate(
            "() => (window.dataLayer ?? []).some(entry => entry.event === 'add_to_cart')");
    assertThat((Boolean) tracked).isTrue();
  }

  @Test
  void staleConsentVersionAsksAgain() {
    page.navigate(baseUrl + "/catalog");
    page.evaluate(
        "() => { localStorage.setItem('pm.consent.v1', 'all');"
            + " localStorage.setItem('pm.consent.meta.v1',"
            + " JSON.stringify({version: 'old', acceptedAt: '2025-01-01T00:00:00.000Z'})); }");
    page.reload();
    page.getByRole(
            AriaRole.DIALOG,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Cookie и аналитика"))
        .waitFor();
  }
}
