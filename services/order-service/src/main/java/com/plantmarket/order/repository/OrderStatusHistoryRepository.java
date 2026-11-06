package com.plantmarket.order.repository;

import com.plantmarket.order.domain.OrderStatusHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository
    extends JpaRepository<OrderStatusHistoryEntity, UUID> {

  List<OrderStatusHistoryEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
