package com.plantmarket.order.notification;

import com.plantmarket.order.domain.OrderEntity;
import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.domain.OutboxEventEntity;
import com.plantmarket.order.repository.OrderRepository;
import com.plantmarket.order.repository.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Phase 2 outbox publisher: claims events with SKIP LOCKED, then notifies outside the transaction.
 */
@Component
public class OutboxPublisher {

  private static final int MAX_ATTEMPTS = 5;
  private static final int BATCH_SIZE = 50;
  private static final Duration LEASE = Duration.ofMinutes(5);

  private final OutboxEventRepository outboxRepository;
  private final OrderRepository orderRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  public OutboxPublisher(
      OutboxEventRepository outboxRepository,
      OrderRepository orderRepository,
      ApplicationEventPublisher eventPublisher,
      ObjectMapper objectMapper,
      TransactionTemplate transactionTemplate) {
    this.outboxRepository = outboxRepository;
    this.orderRepository = orderRepository;
    this.eventPublisher = eventPublisher;
    this.objectMapper = objectMapper;
    this.transactionTemplate = transactionTemplate;
  }

  @Scheduled(fixedDelayString = "${app.outbox.publish-ms:5000}")
  public void publishPending() {
    Instant now = Instant.now();
    List<UUID> claimed = transactionTemplate.execute(status -> claim(now));
    if (claimed == null) {
      return;
    }
    for (UUID eventId : claimed) {
      publishOne(eventId);
    }
  }

  private List<UUID> claim(Instant now) {
    List<OutboxEventEntity> events = outboxRepository.findPendingForUpdate(now, BATCH_SIZE);
    Instant leaseUntil = now.plus(LEASE);
    for (OutboxEventEntity event : events) {
      // The lease keeps other instances from claiming the row while the notification is sent.
      event.scheduleNextAttempt(leaseUntil);
    }
    return events.stream().map(OutboxEventEntity::getId).toList();
  }

  private void publishOne(UUID eventId) {
    try {
      Publishable payload =
          transactionTemplate.execute(status -> {
            OutboxEventEntity event = outboxRepository.findById(eventId).orElse(null);
            if (event == null) {
              return null;
            }
            OrderStatus orderStatus =
                OrderStatus.valueOf(
                    objectMapper.readTree(event.getPayload()).path("status").asString());
            String customerEmail =
                orderRepository
                    .findById(event.getAggregateId())
                    .map(OrderEntity::getCustomerEmail)
                    .orElse(null);
            return new Publishable(event.getAggregateId(), customerEmail, orderStatus);
          });
      if (payload == null) {
        return;
      }
      eventPublisher.publishEvent(
          new OrderStatusChangedEvent(
              payload.aggregateId(), payload.customerEmail(), payload.status()));
      transactionTemplate.executeWithoutResult(
          status -> outboxRepository.findById(eventId).ifPresent(OutboxEventEntity::markPublished));
    } catch (RuntimeException exception) {
      markFailed(eventId, exception);
    }
  }

  private void markFailed(UUID eventId, RuntimeException exception) {
    transactionTemplate.executeWithoutResult(status ->
        outboxRepository
            .findById(eventId)
            .filter(event -> event.getPublishedAt() == null && !event.isDead())
            .ifPresent(event -> {
              long backoff = backoffSeconds(event);
              event.markFailed(
                  truncate(exception.getMessage()), Instant.now().plusSeconds(backoff));
              if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.markDead();
              }
            }));
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

  private record Publishable(UUID aggregateId, String customerEmail, OrderStatus status) {}
}
