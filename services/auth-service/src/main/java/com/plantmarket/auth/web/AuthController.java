package com.plantmarket.auth.web;

import com.plantmarket.auth.service.AuthService;
import com.plantmarket.auth.web.dto.LoginRequest;
import com.plantmarket.auth.web.dto.RefreshRequest;
import com.plantmarket.auth.web.dto.RegisterRequest;
import com.plantmarket.auth.web.dto.TokenResponse;
import com.plantmarket.auth.web.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@AuthenticationPrincipal Jwt jwt) {
    authService.logout(UUID.fromString(jwt.getSubject()));
  }

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return authService.me(UUID.fromString(jwt.getSubject()));
  }
}
