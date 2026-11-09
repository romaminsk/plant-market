package com.plantmarket.payment.repository;

import com.plantmarket.payment.domain.WebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, String> {

  /** Claims the event id; returns 0 when the event was already processed. */
  @Modifying
  @Query(
      value =
          """
          insert into payment_webhook_events (event_id, received_at)
          values (:eventId, now())
          on conflict (event_id) do nothing
          """,
      nativeQuery = true)
  int claim(@Param("eventId") String eventId);
}
