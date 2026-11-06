package com.plantmarket.auth.web;

import com.plantmarket.auth.service.AuthService;
import com.plantmarket.auth.web.dto.RegisterRequest;
import com.plantmarket.common.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

/** Renders the branded login and registration pages instead of Spring Security defaults. */
@Controller
public class LoginPageController {

  private static final MediaType HTML_UTF8 =
      new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
  private static final String ERROR =
      "<p class=\"alert\" role=\"alert\">Неверный e-mail или пароль.</p>";
  private static final String LOGOUT =
      "<p class=\"notice\" role=\"status\">Вы вышли из аккаунта.</p>";
  private static final String REGISTERED =
      "<p class=\"notice\" role=\"status\">Аккаунт создан. Войдите.</p>";

  private final AuthService authService;
  private final String loginTemplate;
  private final String registerTemplate;

  public LoginPageController(AuthService authService) throws IOException {
    this.authService = authService;
    this.loginTemplate = readTemplate("templates/login.html");
    this.registerTemplate = readTemplate("templates/register.html");
  }

  @GetMapping("/login")
  public ResponseEntity<String> login(HttpServletRequest request) {
    String email = request.getParameter("email");
    String html =
        loginTemplate
            .replace("{{csrfToken}}", csrfToken(request))
            .replace("{{email}}", email == null ? "" : HtmlUtils.htmlEscape(email))
            .replace("{{registered}}", request.getParameter("registered") == null ? "" : REGISTERED)
            .replace("{{error}}", request.getParameter("error") == null ? "" : ERROR)
            .replace("{{logout}}", request.getParameter("logout") == null ? "" : LOGOUT);
    return html(html);
  }

  @GetMapping("/register")
  public ResponseEntity<String> registerPage(HttpServletRequest request) {
    return html(renderRegister(request, null, "", ""));
  }

  @PostMapping("/register")
  public ResponseEntity<String> register(
      @RequestParam String fullName,
      @RequestParam String email,
      @RequestParam String password,
      HttpServletRequest request) {
    String name = fullName == null ? "" : fullName.trim();
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    String error = validate(name, normalizedEmail, password);
    if (error == null) {
      try {
        authService.register(new RegisterRequest(normalizedEmail, password, name));
        String target =
            "/login?registered=1&email="
                + URLEncoder.encode(normalizedEmail, StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, target).build();
      } catch (ApiException exception) {
        error =
            exception.getStatus() == HttpStatus.CONFLICT
                ? "Этот e-mail уже зарегистрирован."
                : exception.getMessage();
      }
    }
    return html(renderRegister(request, error, name, normalizedEmail));
  }

  private String renderRegister(
      HttpServletRequest request, String error, String fullName, String email) {
    String errorHtml =
        error == null || error.isBlank()
            ? ""
            : "<p class=\"alert\" role=\"alert\">" + HtmlUtils.htmlEscape(error) + "</p>";
    return registerTemplate
        .replace("{{csrfToken}}", csrfToken(request))
        .replace("{{error}}", errorHtml)
        .replace("{{fullName}}", HtmlUtils.htmlEscape(fullName))
        .replace("{{email}}", HtmlUtils.htmlEscape(email));
  }

  private static String validate(String name, String email, String password) {
    if (name.isBlank() || name.length() > 200) {
      return "Укажите имя (до 200 символов).";
    }
    if (email.isBlank()
        || email.length() > 254
        || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      return "Укажите корректный e-mail.";
    }
    if (password == null || password.length() < 12 || password.length() > 128) {
      return "Пароль должен быть от 12 до 128 символов.";
    }
    return null;
  }

  private static String csrfToken(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return csrfToken == null ? "" : csrfToken.getToken();
  }

  private static String readTemplate(String path) throws IOException {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static ResponseEntity<String> html(String html) {
    return ResponseEntity.ok()
        .contentType(HTML_UTF8)
        .cacheControl(CacheControl.noStore())
        .body(html);
  }
}
