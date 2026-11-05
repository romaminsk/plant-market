package com.plantmarket.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public final class AdminPage {

  private final Page page;

  public AdminPage(Page page) {
    this.page = page;
  }

  public AdminPage open(String baseUrl) {
    page.navigate(baseUrl);
    return this;
  }

  public void login(String email, String password) {
    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Админка")).waitFor();
    page.getByLabel("Email").fill(email);
    page.getByLabel("Пароль").fill(password);
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Войти")).click();
  }

  public void waitForProducts() {
    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Продукты")).waitFor();
    productRows().first().waitFor();
  }

  public Locator productRows() {
    return page.locator("tbody tr");
  }
}
