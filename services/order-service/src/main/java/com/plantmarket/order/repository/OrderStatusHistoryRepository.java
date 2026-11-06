package com.plantmarket.order.repository;

import com.plantmarket.order.domain.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository
    extends JpaRepository<OrderStatusHistoryEntity, java.util.UUID> {}
