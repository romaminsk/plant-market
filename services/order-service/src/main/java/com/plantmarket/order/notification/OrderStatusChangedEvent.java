package com.plantmarket.order.notification;

import com.plantmarket.order.domain.OrderStatus;
import java.util.UUID;

public record OrderStatusChangedEvent(UUID orderId, String customerEmail, OrderStatus status) {}
