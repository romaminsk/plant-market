package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.plantmarket.e2e.pages.AdminPage;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Layout audit: forms, tables and page overflow at desktop and mobile widths. */
@Feature("UI layout")
@Story("Forms and tables do not overlap or overflow")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class LayoutAuditE2ETest extends PlaywrightTestBase {

  private static final String AUDIT_JS =
      """
      () => {
        const problems = [];
        const visible = (el) => {
          const rect = el.getBoundingClientRect();
          const style = getComputedStyle(el);
          return rect.width > 1 && rect.height > 1
            && style.visibility !== 'hidden' && style.display !== 'none';
        };
        const describe = (el) => (el.tagName + ':' + (el.className || '') + ':'
          + (el.innerText || '').trim().slice(0, 18)).replace(/\s+/g, ' ');
        const controls = [...document.querySelectorAll('input, select, textarea, button')]
          .filter(visible);
        const rects = controls.map((el) => ({ el, rect: el.getBoundingClientRect() }));
        for (let i = 0; i < rects.length; i++) {
          for (let j = i + 1; j < rects.length; j++) {
            const a = rects[i];
            const b = rects[j];
            if (a.el.contains(b.el) || b.el.contains(a.el)) continue;
            const x = Math.max(0, Math.min(a.rect.right, b.rect.right)
              - Math.max(a.rect.left, b.rect.left));
            const y = Math.max(0, Math.min(a.rect.bottom, b.rect.bottom)
              - Math.max(a.rect.top, b.rect.top));
            if (x * y > 4) {
              problems.push('overlap ' + describe(a.el) + ' / ' + describe(b.el)
                + ' ' + Math.round(x * y) + 'px2');
            }
          }
        }
        const viewport = document.documentElement.clientWidth;
        for (const { el, rect } of rects) {
          if (el.closest('.table-wrap') || el.closest('td')) continue;
          if (rect.right > viewport + 2 || rect.left < -2) {
            problems.push('overflow ' + el.tagName + ' ['
              + Math.round(rect.left) + ',' + Math.round(rect.right)
              + '] viewport=' + viewport);
          }
        }
        if (document.documentElement.scrollWidth > viewport + 1) {
          problems.push('page-overflow ' + document.documentElement.scrollWidth
            + '>' + viewport);
        }
        for (const wrap of document.querySelectorAll('.table-wrap')) {
          const table = wrap.querySelector('table');
          if (table && table.scrollWidth > wrap.clientWidth + 1
            && getComputedStyle(wrap).overflowX === 'visible') {
            problems.push('table-overflow-not-scrollable');
          }
        }
        return problems;
      }
      """;

  private final String baseUrl = env("E2E_BASE_URL", "http://localhost:3000");
  private final String adminUrl = env("E2E_ADMIN_URL", "http://localhost:5173");
  private final String authUrl = env("E2E_AUTH_URL", "http://localhost:8080");
  private final String apiUrl = env("E2E_API_URL", "http://localhost:8080");
  private final List<String> problems = new ArrayList<>();

  @Test
  void storefrontFormsAndTablesHaveNoOverlap() {
    auditPage("catalog", baseUrl + "/catalog");
    auditPage("cart-empty", baseUrl + "/cart");
    auditPage("legal", baseUrl + "/legal/cookies");
    auditPage("login", baseUrl + "/api/auth/login?returnTo=/catalog");
    auditPage("register", authUrl + "/register");

    page.navigate(baseUrl + "/catalog");
    acceptEssentialCookies();
    setCart();
    auditPage("cart", baseUrl + "/cart");
    page.navigate(baseUrl + "/checkout");
    acceptEssentialCookies();
    page.locator("input[name='guestEmail']").waitFor();
    audit("checkout");

    page.navigate(baseUrl + "/catalog");
    page.locator(".plant-card h2 a").first().click();
    page.getByTestId("plant-price").waitFor();
    audit("plant");

    page.navigate(baseUrl + "/catalog");
    page.locator(".product-row h2 a").first().click();
    page.locator(".detail-price").first().waitFor();
    audit("product");

    assertThat(problems).as("layout problems").isEmpty();
  }

  @Test
  void adminFormsAndTablesHaveNoOverlap() throws Exception {
    page.navigate(adminUrl);
    audit("admin-login");

    AdminPage admin = new AdminPage(page);
    admin.openWithToken(adminUrl, ApiTokens.admin(apiUrl));
    admin.waitForProducts();
    audit("admin-products");

    auditAdminTab("admin-categories", "Категории", "Категории");
    auditAdminTab("admin-plants", "Экземпляры", "Экземпляры");
    page.locator("tbody tr")
        .first()
        .getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Редактировать"))
        .click();
    page.getByText("Галерея WYSIWYG").waitFor();
    audit("admin-gallery");
    auditAdminTab("admin-variants", "Варианты", "Варианты продукта");
    auditAdminTab("admin-stock", "Партии", "Новая партия");
    auditAdminTab("admin-claims", "Претензии", "Претензии");

    assertThat(problems).as("layout problems").isEmpty();
  }

  private void auditAdminTab(String name, String tab, String heading) {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(tab)).click();
    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(heading)).waitFor();
    audit(name);
  }

  private void auditPage(String name, String url) {
    page.navigate(url);
    acceptEssentialCookies();
    audit(name);
  }

  private void audit(String name) {
    for (int width : new int[] {1280, 390}) {
      page.setViewportSize(width, width == 390 ? 844 : 800);
      page.evaluate("window.scrollTo(0, 0)");
      @SuppressWarnings("unchecked")
      List<String> pageProblems = (List<String>) page.evaluate(AUDIT_JS);
      for (String problem : pageProblems) {
        problems.add(name + " @" + width + ": " + problem);
      }
      try {
        Path directory = Path.of("build", "e2e-artifacts", "layout");
        Files.createDirectories(directory);
        page.screenshot(
            new Page.ScreenshotOptions()
                .setPath(directory.resolve(name + "-" + width + ".png"))
                .setFullPage(true));
      } catch (Exception ignored) {
        // Screenshots are best effort; the assertion is the gate.
      }
    }
  }

  private void setCart() {
    page.evaluate(
        "() => localStorage.setItem('pm.cart.v1', JSON.stringify([{"
            + "type:'INDIVIDUAL_PLANT', id:'layout-audit',"
            + " name:'Layout audit plant', priceMinor:12000, currency:'BYN', quantity:1}]))");
  }
}
