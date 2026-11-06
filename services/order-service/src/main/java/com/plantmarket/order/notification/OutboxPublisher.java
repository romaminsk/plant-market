package com.plantmarket.order.notification;

import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.repository.OrderRepository;
import com.plantmarket.order.repository.OutboxEventRepository;
import org.springframework.context.ApplicationEventPublisher;
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

  @Scheduled(fixedDelayString = "${app.outbox.publish-ms:5000}")
  @Transactional
  public void publishPending() {
    outboxRepository
        .findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()
        .forEach(event -> {
          OrderStatus status =
              OrderStatus.valueOf(
                  objectMapper.readTree(event.getPayload()).path("status").asString());
          orderRepository
              .findById(event.getAggregateId())
              .ifPresent(order ->
                  eventPublisher.publishEvent(
                      new OrderStatusChangedEvent(
                          order.getId(), order.getCustomerEmail(), status)));
          event.markPublished();
        });
  }
}
