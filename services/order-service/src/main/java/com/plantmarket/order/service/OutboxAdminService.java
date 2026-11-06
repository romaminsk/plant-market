package com.plantmarket.order.service;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.order.domain.OutboxEventEntity;
import com.plantmarket.order.repository.OutboxEventRepository;
import com.plantmarket.order.web.dto.OutboxEventResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxAdminService {

  private final OutboxEventRepository outboxRepository;

  public OutboxAdminService(OutboxEventRepository outboxRepository) {
    this.outboxRepository = outboxRepository;
  }

  @Transactional(readOnly = true)
  public List<OutboxEventResponse> listDead() {
    return outboxRepository.findByDeadTrueOrderByCreatedAtDesc().stream()
        .map(OutboxAdminService::toResponse)
        .toList();
  }

  @Transactional
  public OutboxEventResponse redrive(UUID id) {
    OutboxEventEntity event =
        outboxRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("Outbox event not found: " + id));
    if (!event.isDead()) {
      throw ApiException.conflict("Only dead-letter events can be redriven: " + id);
    }
    event.resetForRedrive();
    return toResponse(event);
  }

  private static OutboxEventResponse toResponse(OutboxEventEntity event) {
    return new OutboxEventResponse(
        event.getId(),
        event.getAggregateId(),
        event.getEventType(),
        event.getAttempts(),
        event.getLastError(),
        event.isDead(),
        event.getCreatedAt(),
        event.getPublishedAt(),
        event.getNextAttemptAt());
  }
}
