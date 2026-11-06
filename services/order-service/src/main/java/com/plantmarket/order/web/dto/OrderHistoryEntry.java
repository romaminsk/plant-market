package com.plantmarket.order.web.dto;

import com.plantmarket.order.domain.OrderStatus;
import java.time.Instant;

public record OrderHistoryEntry(OrderStatus status, Instant at) {}
