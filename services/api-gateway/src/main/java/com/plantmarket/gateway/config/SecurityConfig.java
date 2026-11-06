package com.plantmarket.gateway.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  @Bean
  public ReactiveJwtDecoder jwtDecoder(JwtProperties properties) {
    SecretKey key =
        new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("ROLE_");
    ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> Flux.fromIterable(authorities.convert(jwt)));
    return converter;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      ProblemAuthenticationEntryPoint authenticationEntryPoint,
      ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .headers(headers ->
            headers
                .hsts(hsts -> hsts.includeSubdomains(true).maxAge(Duration.ofDays(365)))
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer ->
                    referrer.policy(
                        ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER))
                .permissionsPolicy(
                    permissions -> permissions.policy("geolocation=(), microphone=(), camera=()"))
                .contentSecurityPolicy(
                    csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
        .authorizeExchange(exchange ->
            exchange
                .pathMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()
                .pathMatchers("/actuator/health/**", "/actuator/info")
                .permitAll()
                .pathMatchers("/api/v1/auth/**", "/api/v1/restock-subscriptions/**")
                .permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/orders/guest")
                .permitAll()
                .pathMatchers(
                    HttpMethod.GET,
                    "/api/v1/categories/**",
                    "/api/v1/products/**",
                    "/api/v1/variants/**",
                    "/api/v1/individual-plants/**",
                    "/api/v1/shipping/**")
                .permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/stock-batches/**")
                .hasAnyRole("ADMIN", "OPERATOR", "MANAGER")
                .pathMatchers(
                    HttpMethod.POST,
                    "/api/v1/categories/**",
                    "/api/v1/products/**",
                    "/api/v1/variants/**",
                    "/api/v1/individual-plants/**",
                    "/api/v1/stock-batches/**",
                    "/api/v1/shipping/**",
                    "/api/v1/media/**")
                .hasAnyRole("ADMIN", "OPERATOR", "MANAGER")
                .pathMatchers(
                    HttpMethod.PUT,
                    "/api/v1/categories/**",
                    "/api/v1/products/**",
                    "/api/v1/variants/**",
                    "/api/v1/individual-plants/**",
                    "/api/v1/stock-batches/**",
                    "/api/v1/shipping/**")
                .hasAnyRole("ADMIN", "OPERATOR", "MANAGER")
                .pathMatchers(
                    HttpMethod.DELETE,
                    "/api/v1/categories/**",
                    "/api/v1/products/**",
                    "/api/v1/variants/**",
                    "/api/v1/individual-plants/**",
                    "/api/v1/stock-batches/**",
                    "/api/v1/shipping/**")
                .hasAnyRole("ADMIN", "OPERATOR", "MANAGER")
                .pathMatchers("/api/v1/admin/**", "/api/v1/claims/**")
                .hasAnyRole("ADMIN", "OPERATOR", "MANAGER")
                .anyExchange()
                .authenticated())
        .oauth2ResourceServer(oauth2 ->
            oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(authenticationEntryPoint))
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
        .build();
  }
}
