package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.ReservationEntity;
import com.plantmarket.catalog.domain.ReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ReservationEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ReservationEntity> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
