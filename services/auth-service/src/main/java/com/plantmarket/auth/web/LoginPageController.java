package com.plantmarket.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Renders the branded login page instead of Spring Security's generated form. */
@Controller
public class LoginPageController {

  private static final MediaType HTML_UTF8 =
      new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
  private static final String ERROR =
      "<p class=\"alert\" role=\"alert\">Неверный e-mail или пароль.</p>";
  private static final String LOGOUT =
      "<p class=\"notice\" role=\"status\">Вы вышли из аккаунта.</p>";

  private final String template;

  public LoginPageController() throws IOException {
    try (var input = new ClassPathResource("templates/login.html").getInputStream()) {
      this.template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @GetMapping("/login")
  public ResponseEntity<String> login(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    String html =
        template
            .replace("{{csrfToken}}", csrfToken == null ? "" : csrfToken.getToken())
            .replace("{{error}}", request.getParameter("error") == null ? "" : ERROR)
            .replace("{{logout}}", request.getParameter("logout") == null ? "" : LOGOUT);
    return ResponseEntity.ok()
        .contentType(HTML_UTF8)
        .cacheControl(CacheControl.noStore())
        .body(html);
  }
}
