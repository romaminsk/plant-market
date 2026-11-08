package com.plantmarket.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.plantmarket.auth.domain.RoleEntity;
import com.plantmarket.auth.domain.UserEntity;
import com.plantmarket.auth.domain.UserStatus;
import com.plantmarket.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  private static final List<String> PUBLIC_POST_ENDPOINTS =
      List.of(
          "/api/v1/auth/register",
          "/api/v1/auth/login",
          "/api/v1/auth/refresh",
          "/api/v1/auth/session",
          "/api/v1/auth/session/refresh",
          "/api/v1/auth/session/logout");

  @Bean
  public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  @Bean
  public JwtEncoder jwtEncoder(JwtProperties properties) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
  }

  @Bean
  public JwtDecoder jwtDecoder(JwtProperties properties) {
    return NimbusJwtDecoder.withSecretKey(secretKey(properties))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(
      HttpSecurity http,
      OAuth2TokenGenerator<?> tokenGenerator,
      RegisteredClientRepository registeredClientRepository)
      throws Exception {
    http.securityMatcher("/oauth2/**", "/.well-known/**")
        .oauth2AuthorizationServer(authorizationServer ->
            authorizationServer
                .tokenGenerator(tokenGenerator)
                .clientAuthentication(clientAuthentication ->
                    clientAuthentication
                        .authenticationConverter(
                            new PublicClientRefreshTokenAuthenticationConverter())
                        .authenticationProviders(providers ->
                            providers.add(
                                0,
                                new PublicClientRefreshTokenAuthenticationProvider(
                                    registeredClientRepository)))))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .exceptionHandling(exceptions ->
            exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        .csrf(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http, ProblemAuthenticationEntryPoint authenticationEntryPoint)
      throws Exception {
    http.securityMatcher("/api/**", "/actuator/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
            auth.requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS.toArray(String[]::new))
                .permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info")
                .permitAll()
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(oauth2 ->
            oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(authenticationEntryPoint))
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint));
    return http.build();
  }

  @Bean
  @Order(3)
  public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth ->
            auth.requestMatchers(
                    "/login",
                    "/register",
                    "/auth.css",
                    "/error",
                    "/fonts/**",
                    "/images/**",
                    "/favicon.ico")
                .permitAll()
                .anyRequest()
                .authenticated())
        .formLogin(form -> form.loginPage("/login").permitAll())
        .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepository) {
    return username ->
        userRepository
            .findByEmailIgnoreCase(username)
            .map(this::toUserDetails)
            .orElseThrow(() -> new UsernameNotFoundException(username));
  }

  @Bean
  public RegisteredClientRepository registeredClientRepository(
      JwtProperties properties,
      @Value("${app.auth.storefront-redirect-uris:http://localhost:3000/auth/callback}")
          String storefrontRedirects,
      @Value("${app.auth.admin-redirect-uris:http://localhost:5173/auth/callback}")
          String adminRedirects) {
    return new InMemoryRegisteredClientRepository(
        registeredClient(
            "storefront", "Plant Market Storefront", properties, split(storefrontRedirects)),
        registeredClient("admin", "Plant Market Admin", properties, split(adminRedirects)));
  }

  private static String[] split(String value) {
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(entry -> !entry.isBlank())
        .toArray(String[]::new);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings(JwtProperties properties) {
    return AuthorizationServerSettings.builder().issuer(properties.issuer()).build();
  }

  @Bean
  public OAuth2TokenGenerator<?> tokenGenerator(
      JwtEncoder jwtEncoder, OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
    JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
    jwtGenerator.setJwtCustomizer(jwtCustomizer);
    return new DelegatingOAuth2TokenGenerator(
        jwtGenerator, new OAuth2AccessTokenGenerator(), new PublicClientRefreshTokenGenerator());
  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(UserRepository userRepository) {
    return context -> {
      context.getJwsHeader().algorithm(MacAlgorithm.HS256);
      if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        return;
      }
      userRepository
          .findByEmailIgnoreCase(context.getPrincipal().getName())
          .ifPresent(user -> {
            context.getClaims().subject(user.getId().toString());
            context.getClaims().claim("email", user.getEmail());
            context
                .getClaims()
                .claim(
                    "roles", user.getRoles().stream().map(RoleEntity::getName).sorted().toList());
          });
    };
  }

  private RegisteredClient registeredClient(
      String clientId, String clientName, JwtProperties properties, String... redirectUris) {
    RegisteredClient.Builder builder =
        RegisteredClient.withId(clientId)
            .clientId(clientId)
            .clientName(clientName)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
    for (String redirectUri : redirectUris) {
      builder.redirectUri(redirectUri);
    }
    return builder
        .scope("catalog.read")
        .scope("catalog.write")
        .clientSettings(
            ClientSettings.builder()
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .build())
        .tokenSettings(
            TokenSettings.builder()
                .accessTokenTimeToLive(properties.accessTtl())
                .refreshTokenTimeToLive(properties.refreshTtl())
                .reuseRefreshTokens(false)
                .build())
        .build();
  }

  private UserDetails toUserDetails(UserEntity user) {
    List<SimpleGrantedAuthority> authorities =
        user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .toList();
    return User.withUsername(user.getEmail())
        .password(user.getPasswordHash())
        .authorities(authorities)
        .disabled(user.getStatus() != UserStatus.ACTIVE)
        .build();
  }

  private SecretKey secretKey(JwtProperties properties) {
    return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
