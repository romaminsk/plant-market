package com.plantmarket.auth.service;

import com.plantmarket.auth.config.JwtProperties;
import com.plantmarket.auth.domain.RoleEntity;
import com.plantmarket.auth.domain.UserEntity;
import com.plantmarket.common.id.UuidV7;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
  }

  public String issueAccessToken(UserEntity user) {
    Instant now = Instant.now();
    List<String> roles = user.getRoles().stream().map(RoleEntity::getName).sorted().toList();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(now)
            .expiresAt(now.plus(properties.accessTtl()))
            .subject(user.getId().toString())
            .id(UuidV7.generate().toString())
            .claim("email", user.getEmail())
            .claim("roles", roles)
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public long accessTtlSeconds() {
    return properties.accessTtl().toSeconds();
  }
}
