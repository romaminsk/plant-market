package com.plantmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Playwright;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

@Feature("A11y")
@Story("Lighthouse accessibility gate >= 0.95")
@EnabledIfEnvironmentVariable(named = "E2E_BASE_URL", matches = ".+")
class LighthouseA11yTest {

  @Test
  void catalogMeetsAccessibilityGate() throws Exception {
    String baseUrl = System.getenv("E2E_BASE_URL");
    Path output = Path.of("build", "e2e-artifacts", "lighthouse-a11y.json");
    Files.createDirectories(output.getParent());

    String chromePath;
    try (Playwright playwright = Playwright.create()) {
      chromePath = playwright.chromium().executablePath();
    }

    ProcessBuilder processBuilder =
        new ProcessBuilder(
            "pnpm",
            "dlx",
            "lighthouse@13.6.0",
            baseUrl + "/catalog",
            "--only-categories=accessibility",
            "--output=json",
            "--output-path=" + output.toAbsolutePath(),
            "--chrome-flags=--headless=new --no-sandbox",
            "--quiet");
    processBuilder.environment().put("CHROME_PATH", chromePath);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();
    String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();

    assertThat(exitCode).as(log).isZero();
    double score =
        new ObjectMapper()
            .readTree(output.toFile())
            .path("categories")
            .path("accessibility")
            .path("score")
            .asDouble();
    assertThat(score).isGreaterThanOrEqualTo(0.95);
  }
}
