package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.ReservationEntity;
import com.plantmarket.catalog.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

  List<ReservationEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

  List<ReservationEntity> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
}
