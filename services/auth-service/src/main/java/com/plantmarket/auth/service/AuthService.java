package com.plantmarket.auth.service;

import com.plantmarket.auth.config.JwtProperties;
import com.plantmarket.auth.domain.RefreshTokenEntity;
import com.plantmarket.auth.domain.RoleEntity;
import com.plantmarket.auth.domain.UserEntity;
import com.plantmarket.auth.domain.UserStatus;
import com.plantmarket.auth.repository.RefreshTokenRepository;
import com.plantmarket.auth.repository.RoleRepository;
import com.plantmarket.auth.repository.UserRepository;
import com.plantmarket.auth.web.dto.LoginRequest;
import com.plantmarket.auth.web.dto.RefreshRequest;
import com.plantmarket.auth.web.dto.RegisterRequest;
import com.plantmarket.auth.web.dto.TokenResponse;
import com.plantmarket.auth.web.dto.UserResponse;
import com.plantmarket.common.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String CUSTOMER_ROLE = "CUSTOMER";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  public AuthService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      JwtProperties jwtProperties) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw ApiException.conflict("Email already registered");
    }

    RoleEntity customerRole =
        roleRepository
            .findById(CUSTOMER_ROLE)
            .orElseThrow(() -> new IllegalStateException("CUSTOMER role is missing"));
    UserEntity user =
        new UserEntity(
            email,
            passwordEncoder.encode(request.password()),
            request.fullName().trim(),
            Set.of(customerRole));
    try {
      userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      throw ApiException.conflict("Email already registered");
    }
    return toResponse(user);
  }

  @Transactional
  public TokenResponse login(LoginRequest request) {
    UserEntity user =
        userRepository
            .findByEmailIgnoreCase(normalizeEmail(request.email()))
            .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw ApiException.unauthorized("Invalid email or password");
    }
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw ApiException.forbidden("Account is disabled");
    }
    return issueTokens(user);
  }

  @Transactional(noRollbackFor = TokenReuseException.class)
  public TokenResponse refresh(RefreshRequest request) {
    RefreshTokenEntity stored =
        refreshTokenRepository
            .findByTokenHash(sha256(request.refreshToken()))
            .orElseThrow(() -> ApiException.unauthorized("Refresh token is invalid"));

    Instant now = Instant.now();
    if (stored.getRevokedAt() != null) {
      refreshTokenRepository.revokeAllActive(stored.getUser().getId(), now);
      throw new TokenReuseException();
    }
    if (!stored.getExpiresAt().isAfter(now)) {
      stored.revoke(now);
      throw ApiException.unauthorized("Refresh token expired");
    }

    stored.revoke(now);
    return issueTokens(stored.getUser());
  }

  @Transactional(readOnly = true)
  public UserResponse me(UUID userId) {
    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
    return toResponse(user);
  }

  private TokenResponse issueTokens(UserEntity user) {
    String accessToken = jwtService.issueAccessToken(user);
    String refreshToken = createRefreshToken(user);
    return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.accessTtlSeconds());
  }

  private String createRefreshToken(UserEntity user) {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expiresAt = Instant.now().plus(jwtProperties.refreshTtl());
    refreshTokenRepository.save(new RefreshTokenEntity(user, sha256(rawToken), expiresAt));
    return rawToken;
  }

  private UserResponse toResponse(UserEntity user) {
    List<String> roles = user.getRoles().stream().map(RoleEntity::getName).sorted().toList();
    return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), roles);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
