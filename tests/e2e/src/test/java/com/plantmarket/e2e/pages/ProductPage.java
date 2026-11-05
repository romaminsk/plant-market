package com.plantmarket.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class ProductPage {

  private final Page page;

  public ProductPage(Page page) {
    this.page = page;
  }

  public void waitForLoaded() {
    priceLocator().waitFor();
  }

  public Locator headingLocator() {
    return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
  }

  public String heading() {
    return headingLocator().innerText();
  }

  public Locator priceLocator() {
    return page.getByTestId("product-price");
  }

  public String price() {
    return priceLocator().innerText();
  }
}
