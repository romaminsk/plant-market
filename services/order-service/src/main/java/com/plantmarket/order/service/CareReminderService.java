package com.plantmarket.order.service;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.order.domain.CareReminderEntity;
import com.plantmarket.order.domain.OrderEntity;
import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.notification.CareReminderNotifier;
import com.plantmarket.order.repository.CareReminderRepository;
import com.plantmarket.order.repository.OrderRepository;
import com.plantmarket.order.web.dto.CareReminderRequest;
import com.plantmarket.order.web.dto.CareReminderResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareReminderService {

  private static final Set<OrderStatus> REMINDABLE_STATUSES =
      Set.of(
          OrderStatus.PAID,
          OrderStatus.FULFILLING,
          OrderStatus.SHIPPED,
          OrderStatus.DELIVERED,
          OrderStatus.COMPLETED);

  private final CareReminderRepository reminderRepository;
  private final OrderRepository orderRepository;
  private final CareReminderNotifier notifier;

  public CareReminderService(
      CareReminderRepository reminderRepository,
      OrderRepository orderRepository,
      CareReminderNotifier notifier) {
    this.reminderRepository = reminderRepository;
    this.orderRepository = orderRepository;
    this.notifier = notifier;
  }

  @Transactional
  public List<CareReminderResponse> create(
      UUID customerId, UUID orderId, CareReminderRequest request) {
    OrderEntity order = findOwned(customerId, orderId);
    if (!REMINDABLE_STATUSES.contains(order.getStatus())) {
      throw ApiException.conflict("Care reminders are only available for paid orders");
    }
    List<CareReminderEntity> existing =
        reminderRepository.findByOrderIdAndActiveTrueOrderByCreatedAtAsc(orderId);
    if (!existing.isEmpty()) {
      return existing.stream().map(CareReminderService::toResponse).toList();
    }
    Instant nextSendAt = Instant.now().plus(Duration.ofDays(request.intervalDays()));
    List<CareReminderEntity> created =
        order.getItems().stream()
            .map(item ->
                new CareReminderEntity(
                    orderId,
                    item.getId(),
                    customerId,
                    order.getCustomerEmail(),
                    item.getName(),
                    request.intervalDays(),
                    nextSendAt))
            .toList();
    return reminderRepository.saveAll(created).stream()
        .map(CareReminderService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CareReminderResponse> list(UUID customerId, UUID orderId) {
    findOwned(customerId, orderId);
    return reminderRepository.findByOrderIdAndActiveTrueOrderByCreatedAtAsc(orderId).stream()
        .map(CareReminderService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CareReminderResponse> listForCustomer(UUID customerId) {
    return reminderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
        .map(CareReminderService::toResponse)
        .toList();
  }

  @Transactional
  public void deactivate(UUID customerId, UUID reminderId) {
    CareReminderEntity reminder =
        reminderRepository
            .findById(reminderId)
            .filter(entity -> customerId.equals(entity.getCustomerId()))
            .orElseThrow(() -> ApiException.notFound("Care reminder not found: " + reminderId));
    reminder.deactivate();
  }

  @Scheduled(fixedDelayString = "${app.care.reminder-scan-ms:60000}")
  @Transactional
  public void sendDue() {
    for (CareReminderEntity reminder :
        reminderRepository.findTop50ByActiveTrueAndNextSendAtBeforeOrderByNextSendAtAsc(
            Instant.now())) {
      if (notifier.send(reminder.getCustomerEmail(), reminder.getPlantName())) {
        reminder.advance();
      }
    }
  }

  private OrderEntity findOwned(UUID customerId, UUID orderId) {
    return orderRepository
        .findById(orderId)
        .filter(order -> customerId.equals(order.getCustomerId()))
        .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
  }

  private static CareReminderResponse toResponse(CareReminderEntity reminder) {
    return new CareReminderResponse(
        reminder.getId(),
        reminder.getOrderId(),
        reminder.getOrderItemId(),
        reminder.getPlantName(),
        reminder.getIntervalDays(),
        reminder.getNextSendAt(),
        reminder.isActive(),
        reminder.getCreatedAt());
  }
}
