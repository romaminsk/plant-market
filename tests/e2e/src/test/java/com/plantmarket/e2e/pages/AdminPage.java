package com.plantmarket.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute;
import java.util.List;

public final class AdminPage {

  private final Page page;

  public AdminPage(Page page) {
    this.page = page;
  }

  public AdminPage open(String baseUrl) {
    page.navigate(baseUrl);
    return this;
  }

  public void openWithToken(String baseUrl, String accessToken) {
    page.navigate(baseUrl);
    page.context()
        .addCookies(
            List.of(
                new Cookie("pm.admin.access", accessToken)
                    .setDomain("localhost")
                    .setPath("/")
                    .setHttpOnly(true)
                    .setSameSite(SameSiteAttribute.STRICT)));
    page.reload();
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
