package com.plantmarket.order.notification;

import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.domain.OutboxEventEntity;
import com.plantmarket.order.repository.OrderRepository;
import com.plantmarket.order.repository.OutboxEventRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Phase 2 outbox publisher: emits Spring events instead of Kafka. */
@Component
public class OutboxPublisher {

  private final OutboxEventRepository outboxRepository;
  private final OrderRepository orderRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  public OutboxPublisher(
      OutboxEventRepository outboxRepository,
      OrderRepository orderRepository,
      ApplicationEventPublisher eventPublisher,
      ObjectMapper objectMapper) {
    this.outboxRepository = outboxRepository;
    this.orderRepository = orderRepository;
    this.eventPublisher = eventPublisher;
    this.objectMapper = objectMapper;
  }

  private static final int MAX_ATTEMPTS = 5;

  @Scheduled(fixedDelayString = "${app.outbox.publish-ms:5000}")
  @Transactional
  public void publishPending() {
    for (OutboxEventEntity event :
        outboxRepository.findPending(Instant.now(), PageRequest.of(0, 50))) {
      try {
        OrderStatus status =
            OrderStatus.valueOf(
                objectMapper.readTree(event.getPayload()).path("status").asString());
        orderRepository
            .findById(event.getAggregateId())
            .ifPresent(order ->
                eventPublisher.publishEvent(
                    new OrderStatusChangedEvent(order.getId(), order.getCustomerEmail(), status)));
        event.markPublished();
      } catch (RuntimeException exception) {
        event.markFailed(
            truncate(exception.getMessage()), Instant.now().plusSeconds(backoffSeconds(event)));
        if (event.getAttempts() >= MAX_ATTEMPTS) {
          event.markDead();
        }
      }
    }
  }

  private static long backoffSeconds(OutboxEventEntity event) {
    return Math.min(3600L, 30L << Math.min(event.getAttempts(), 7));
  }

  private static String truncate(String message) {
    if (message == null) {
      return "unknown error";
    }
    return message.length() <= 500 ? message : message.substring(0, 500);
  }
}
