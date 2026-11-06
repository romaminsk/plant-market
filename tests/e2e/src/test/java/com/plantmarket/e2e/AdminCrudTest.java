package com.plantmarket.e2e;

import com.plantmarket.e2e.pages.AdminPage;
import com.plantmarket.e2e.support.PlaywrightTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Feature("Admin")
@Story("J5 admin: category -> product -> plant quarantine -> variant -> batch release")
@EnabledIfEnvironmentVariable(named = "E2E_ADMIN_URL", matches = ".+")
class AdminCrudTest extends PlaywrightTestBase {

  private final String adminUrl = env("E2E_ADMIN_URL", "http://localhost:5173");
  private final String email = env("E2E_ADMIN_EMAIL", "admin@plantmarket.local");
  private final String password = env("E2E_ADMIN_PASSWORD", "AdminPassword123!");

  @Test
  void adminCanCreateCatalogEntitiesAndReleaseStock() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String categoryName = "E2E Category " + suffix;
    String productName = "E2E Product " + suffix;
    String plantCode = "E2E-IP-" + suffix;
    String variantSku = "E2E-VAR-" + suffix;

    AdminPage admin = new AdminPage(page).open(adminUrl);
    admin.login(email, password);
    admin.waitForProducts();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Категории"))
        .click();
    page.getByLabel("Название").fill(categoryName);
    page.getByLabel("Slug").fill("e2e-category-" + suffix);
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Создать"))
        .click();
    page.getByText("Категория создана").waitFor();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Продукты"))
        .click();
    page.getByLabel("SKU").fill("E2E-PROD-" + suffix);
    page.getByLabel("Название").fill(productName);
    page.getByLabel("Slug").fill("e2e-product-" + suffix);
    page.getByLabel("Категория")
        .selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(categoryName));
    page.getByLabel("Цена, BYN").fill("100");
    page.getByLabel("Вариегатный").check();
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Создать продукт"))
        .click();
    page.getByText("Продукт создан").waitFor();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Экземпляры"))
        .click();
    page.getByLabel("Продукт")
        .selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(productName));
    page.getByLabel("Код").fill(plantCode);
    page.getByLabel("Цена, BYN").fill("120");
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Создать экземпляр"))
        .click();
    page.getByText("Экземпляр создан (ACQUIRED)").waitFor();
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.ROW,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName(plantCode))
        .getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("В карантин"))
        .click();
    page.getByText("Переведён в карантин").waitFor();
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.ROW,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName(plantCode))
        .getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Опубликовать"))
        .click();
    page.getByText("Опубликован").waitFor();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Варианты"))
        .click();
    page.getByLabel("Продукт")
        .selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(productName));
    page.getByLabel("SKU").fill(variantSku);
    page.getByLabel("Название").fill("12 cm pot");
    page.getByLabel("Цена, BYN").fill("150");
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Создать вариант"))
        .click();
    page.getByText("Вариант создан").waitFor();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Партии"))
        .click();
    page.getByLabel("Продукт")
        .selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(productName));
    page.getByLabel("Вариант")
        .selectOption(
            new com.microsoft.playwright.options.SelectOption()
                .setLabel(variantSku + " · 12 cm pot"));
    page.getByLabel("Поставщик").fill("E2E Supplier");
    page.getByLabel("Количество").fill("5");
    page.getByLabel("Себестоимость, BYN").fill("40");
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Создать партию"))
        .click();
    page.getByText("Партия создана (QUARANTINE)").waitFor();
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Выпустить"))
        .click();
    page.getByText("Партия опубликована").waitFor();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Экземпляры"))
        .click();
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.ROW,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName(plantCode))
        .getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Списать"))
        .click();
    page.getByText("Экземпляр списан").waitFor();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Продукты"))
        .click();
    String updatedProductName = productName + " Updated";
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.ROW,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName(productName))
        .getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Редактировать"))
        .click();
    page.getByLabel("Название").fill(updatedProductName);
    page.getByLabel("Цена, BYN").fill("110");
    page.getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("Сохранить продукт"))
        .click();
    page.getByText("Продукт обновлён").waitFor();

    page.getByRole(
            com.microsoft.playwright.options.AriaRole.ROW,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName(updatedProductName))
        .getByRole(
            com.microsoft.playwright.options.AriaRole.BUTTON,
            new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Архивировать"))
        .click();
    page.getByText("Продукт архивирован").waitFor();
  }
}
