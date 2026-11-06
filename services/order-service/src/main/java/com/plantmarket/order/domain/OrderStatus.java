package com.plantmarket.order.domain;

public enum OrderStatus {
  DRAFT,
  PENDING_PAYMENT,
  PAID,
  FULFILLING,
  SHIPPED,
  DELIVERED,
  COMPLETED,
  CANCELLED,
  FAILED
}
