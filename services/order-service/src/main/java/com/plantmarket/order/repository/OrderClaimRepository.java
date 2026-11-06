package com.plantmarket.order.repository;

import com.plantmarket.order.domain.ClaimStatus;
import com.plantmarket.order.domain.OrderClaimEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderClaimRepository extends JpaRepository<OrderClaimEntity, UUID> {

  List<OrderClaimEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

  List<OrderClaimEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  List<OrderClaimEntity> findByStatusOrderByCreatedAtDesc(ClaimStatus status);
}
