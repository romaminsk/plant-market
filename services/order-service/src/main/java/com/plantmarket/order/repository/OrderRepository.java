package com.plantmarket.order.repository;

import com.plantmarket.order.domain.OrderEntity;
import com.plantmarket.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

  List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select orders from OrderEntity orders where orders.id = :id")
  Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);

  @Query(
      """
      select orders.id from OrderEntity orders
      where orders.status = :status
        and orders.reservationExpiresAt is not null
        and orders.reservationExpiresAt <= :now
      """)
  List<UUID> findExpiredIds(@Param("status") OrderStatus status, @Param("now") Instant now);
}
