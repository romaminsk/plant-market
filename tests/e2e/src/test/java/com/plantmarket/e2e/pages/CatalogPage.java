package com.plantmarket.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;

public final class CatalogPage {

  private final Page page;
  private final String baseUrl;

  public CatalogPage(Page page, String baseUrl) {
    this.page = page;
    this.baseUrl = baseUrl;
  }

  public CatalogPage open() {
    page.navigate(baseUrl + "/catalog");
    Locator heading =
        page.getByRole(
            AriaRole.HEADING, new Page.GetByRoleOptions().setName("Растения для вашего дома"));
    try {
      heading.waitFor(new Locator.WaitForOptions().setTimeout(60_000));
    } catch (PlaywrightException firstAttempt) {
      page.reload();
      heading.waitFor(new Locator.WaitForOptions().setTimeout(60_000));
    }
    return this;
  }

  public Locator productLink(String name) {
    return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  public Locator productCards() {
    return page.locator(".product-card");
  }
}
