package com.plantmarket.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_webhook_events")
public class WebhookEventEntity {

  @Id
  @Column(name = "event_id", length = 128)
  private String eventId;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected WebhookEventEntity() {}

  public String getEventId() {
    return eventId;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}
