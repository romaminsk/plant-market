package com.plantmarket.auth.repository;

import com.plantmarket.auth.domain.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update RefreshTokenEntity token set token.revokedAt = :revokedAt "
          + "where token.user.id = :userId and token.revokedAt is null")
  int revokeAllActive(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
