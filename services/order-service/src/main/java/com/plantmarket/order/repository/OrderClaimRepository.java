package com.plantmarket.order.repository;

import com.plantmarket.order.domain.ClaimStatus;
import com.plantmarket.order.domain.OrderClaimEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderClaimRepository extends JpaRepository<OrderClaimEntity, UUID> {

  List<OrderClaimEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

  List<OrderClaimEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  List<OrderClaimEntity> findByStatusOrderByCreatedAtDesc(ClaimStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select claim from OrderClaimEntity claim where claim.id = :id")
  Optional<OrderClaimEntity> findByIdForUpdate(@Param("id") UUID id);
}
