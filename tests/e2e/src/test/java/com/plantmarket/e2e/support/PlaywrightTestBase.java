package com.plantmarket.e2e.support;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/** Programmatic Playwright lifecycle: no JUnit 5 extensions (ADR-0004). */
public abstract class PlaywrightTestBase {

  protected static Playwright playwright;
  protected static Browser browser;

  protected BrowserContext context;
  protected Page page;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
  }

  @AfterAll
  static void closeBrowser() {
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  @BeforeEach
  void openPage() {
    context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
    context
        .tracing()
        .start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
    page = context.newPage();
  }

  @AfterEach
  void captureArtifacts(TestInfo testInfo) throws Exception {
    if (context == null) {
      return;
    }
    String safeName = testInfo.getDisplayName().replaceAll("[^A-Za-z0-9.-]", "_");
    Path artifacts = Path.of("build", "e2e-artifacts", safeName);
    Files.createDirectories(artifacts);
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(artifacts.resolve("screenshot.png"))
            .setFullPage(true));
    context.tracing().stop(new Tracing.StopOptions().setPath(artifacts.resolve("trace.zip")));
    context.close();
  }

  protected String env(String key, String fallback) {
    String value = System.getenv(key);
    return value == null || value.isBlank() ? fallback : value;
  }
}
