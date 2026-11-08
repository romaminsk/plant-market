package com.plantmarket.auth.web;

import com.plantmarket.auth.service.AuthService;
import com.plantmarket.auth.web.dto.LoginRequest;
import com.plantmarket.auth.web.dto.RefreshRequest;
import com.plantmarket.auth.web.dto.TokenResponse;
import com.plantmarket.auth.web.dto.UserResponse;
import com.plantmarket.common.error.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HttpOnly cookie session for the admin SPA. The access token never reaches JavaScript. */
@RestController
@RequestMapping("/api/v1/auth/session")
public class SessionController {

  private static final String ACCESS_COOKIE = "pm.admin.access";
  private static final String REFRESH_COOKIE = "pm.admin.refresh";
  private static final Duration REFRESH_TTL = Duration.ofDays(7);

  private final AuthService authService;
  private final JwtDecoder jwtDecoder;
  private final boolean secure;

  public SessionController(
      AuthService authService,
      JwtDecoder jwtDecoder,
      @Value("${app.session.cookie-secure:false}") boolean secure) {
    this.authService = authService;
    this.jwtDecoder = jwtDecoder;
    this.secure = secure;
  }

  @PostMapping
  public UserResponse login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    AuthService.LoginResult result = authService.loginWithUser(request);
    setCookies(response, result.tokens());
    return result.user();
  }

  @PostMapping("/refresh")
  public ResponseEntity<Void> refresh(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refresh,
      HttpServletResponse response) {
    if (refresh == null || refresh.isBlank()) {
      throw ApiException.unauthorized("No admin session");
    }
    TokenResponse tokens = authService.refresh(new RefreshRequest(refresh));
    setCookies(response, tokens);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = ACCESS_COOKIE, required = false) String access,
      HttpServletResponse response) {
    if (access != null && !access.isBlank()) {
      try {
        Jwt jwt = jwtDecoder.decode(access);
        authService.logout(UUID.fromString(jwt.getSubject()));
      } catch (RuntimeException ignored) {
        // Expired or malformed cookies are simply cleared.
      }
    }
    clearCookies(response);
    return ResponseEntity.noContent().build();
  }

  private void setCookies(HttpServletResponse response, TokenResponse tokens) {
    ResponseCookie access =
        ResponseCookie.from(ACCESS_COOKIE, tokens.accessToken())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/")
            .maxAge(Duration.ofSeconds(tokens.expiresIn()))
            .build();
    ResponseCookie refresh =
        ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Strict")
            .path("/")
            .maxAge(REFRESH_TTL)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, access.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, refresh.toString());
  }

  private void clearCookies(HttpServletResponse response) {
    for (String name : new String[] {ACCESS_COOKIE, REFRESH_COOKIE}) {
      ResponseCookie cookie =
          ResponseCookie.from(name, "")
              .httpOnly(true)
              .secure(secure)
              .sameSite("Strict")
              .path("/")
              .maxAge(Duration.ZERO)
              .build();
      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
  }
}
