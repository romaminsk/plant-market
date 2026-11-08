package com.plantmarket.order.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class ServiceTokenProviderTest {

  private static final String SECRET = "test-only-plant-market-jwt-secret-0123456789";

  @Test
  void issuesShortLivedSystemToken() {
    SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    JwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

    Jwt jwt = decoder.decode(new ServiceTokenProvider(encoder).token());

    assertThat(jwt.getSubject()).isEqualTo("order-service");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("SYSTEM");
    assertThat(jwt.getExpiresAt()).isNotNull();
  }
}
