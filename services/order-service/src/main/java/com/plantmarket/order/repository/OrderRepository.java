package com.plantmarket.order.repository;

import com.plantmarket.order.domain.OrderEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

  List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
