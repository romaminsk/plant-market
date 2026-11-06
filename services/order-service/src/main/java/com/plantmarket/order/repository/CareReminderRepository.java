package com.plantmarket.order.repository;

import com.plantmarket.order.domain.CareReminderEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareReminderRepository extends JpaRepository<CareReminderEntity, UUID> {

  List<CareReminderEntity> findByOrderIdAndActiveTrueOrderByCreatedAtAsc(UUID orderId);

  List<CareReminderEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  List<CareReminderEntity> findTop50ByActiveTrueAndNextSendAtBeforeOrderByNextSendAtAsc(
      Instant now);
}
