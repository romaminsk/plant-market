package com.plantmarket.catalog.domain;

import com.plantmarket.common.id.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restock_subscriptions")
public class RestockSubscriptionEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", length = 20, nullable = false)
  private RestockTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Column(name = "email", length = 254, nullable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private RestockSubscriptionStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "notified_at")
  private Instant notifiedAt;

  protected RestockSubscriptionEntity() {}

  public RestockSubscriptionEntity(RestockTargetType targetType, UUID targetId, String email) {
    this.targetType = targetType;
    this.targetId = targetId;
    this.email = email;
    this.status = RestockSubscriptionStatus.PENDING;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UuidV7.generate();
    }
    createdAt = Instant.now();
  }

  public void markNotified() {
    this.status = RestockSubscriptionStatus.NOTIFIED;
    this.notifiedAt = Instant.now();
  }

  public void resetToPending() {
    this.status = RestockSubscriptionStatus.PENDING;
    this.notifiedAt = null;
  }

  public UUID getId() {
    return id;
  }

  public RestockTargetType getTargetType() {
    return targetType;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public String getEmail() {
    return email;
  }

  public RestockSubscriptionStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getNotifiedAt() {
    return notifiedAt;
  }
}
