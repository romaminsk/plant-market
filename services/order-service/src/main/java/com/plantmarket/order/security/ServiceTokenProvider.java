package com.plantmarket.order.security;

import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Mints a short-lived SYSTEM token for order-service -> catalog/payment calls. */
@Component
public class ServiceTokenProvider {

  private static final long TTL_SECONDS = 60;

  private final JwtEncoder encoder;

  public ServiceTokenProvider(JwtEncoder encoder) {
    this.encoder = encoder;
  }

  public String token() {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("plant-market-order")
            .subject("order-service")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(TTL_SECONDS))
            .claim("roles", List.of("SYSTEM"))
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
