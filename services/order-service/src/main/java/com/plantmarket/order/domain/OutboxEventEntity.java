package com.plantmarket.order.domain;

import com.plantmarket.common.id.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

  @Id private UUID id;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", length = 100, nullable = false)
  private String eventType;

  @Column(name = "payload", nullable = false)
  private String payload;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "dead", nullable = false)
  private boolean dead;

  protected OutboxEventEntity() {}

  public OutboxEventEntity(UUID aggregateId, String eventType, String payload) {
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UuidV7.generate();
    }
    createdAt = Instant.now();
  }

  public void markPublished() {
    this.publishedAt = Instant.now();
    this.lastError = null;
    this.nextAttemptAt = null;
  }

  public void markFailed(String error, Instant nextAttemptAt) {
    this.attempts += 1;
    this.lastError = error;
    this.nextAttemptAt = nextAttemptAt;
  }

  public void markDead() {
    this.dead = true;
    this.publishedAt = Instant.now();
  }

  public void scheduleNextAttempt(Instant instant) {
    this.nextAttemptAt = instant;
  }

  public void resetForRedrive() {
    this.dead = false;
    this.publishedAt = null;
    this.attempts = 0;
    this.lastError = null;
    this.nextAttemptAt = null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public boolean isDead() {
    return dead;
  }
}
