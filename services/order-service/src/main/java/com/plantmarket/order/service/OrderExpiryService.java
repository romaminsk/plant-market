package com.plantmarket.order.service;

import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cancels PENDING_PAYMENT orders whose catalog reservation has expired. */
@Component
public class OrderExpiryService {

  private static final Logger log = LoggerFactory.getLogger(OrderExpiryService.class);

  private final OrderRepository orderRepository;
  private final OrderService orderService;

  public OrderExpiryService(OrderRepository orderRepository, OrderService orderService) {
    this.orderRepository = orderRepository;
    this.orderService = orderService;
  }

  @Scheduled(fixedDelayString = "${app.orders.expiry-scan-ms:60000}")
  public void expireReservations() {
    expireNow();
  }

  /** Runs one scan synchronously; returns the number of cancelled orders. */
  public int expireNow() {
    List<UUID> expired = orderRepository.findExpiredIds(OrderStatus.PENDING_PAYMENT, Instant.now());
    int cancelled = 0;
    for (UUID orderId : expired) {
      try {
        if (orderService.cancelExpired(orderId)) {
          cancelled++;
        }
      } catch (RuntimeException exception) {
        log.warn("Could not cancel expired order {}", orderId, exception);
      }
    }
    return cancelled;
  }
}
