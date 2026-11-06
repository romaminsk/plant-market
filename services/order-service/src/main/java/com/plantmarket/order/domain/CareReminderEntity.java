package com.plantmarket.order.domain;

import com.plantmarket.common.id.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "care_reminders")
public class CareReminderEntity {

  @Id private UUID id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "order_item_id", nullable = false)
  private UUID orderItemId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "customer_email", length = 254, nullable = false)
  private String customerEmail;

  @Column(name = "plant_name", length = 200, nullable = false)
  private String plantName;

  @Column(name = "interval_days", nullable = false)
  private int intervalDays;

  @Column(name = "next_send_at", nullable = false)
  private Instant nextSendAt;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CareReminderEntity() {}

  public CareReminderEntity(
      UUID orderId,
      UUID orderItemId,
      UUID customerId,
      String customerEmail,
      String plantName,
      int intervalDays,
      Instant nextSendAt) {
    this.orderId = orderId;
    this.orderItemId = orderItemId;
    this.customerId = customerId;
    this.customerEmail = customerEmail;
    this.plantName = plantName;
    this.intervalDays = intervalDays;
    this.nextSendAt = nextSendAt;
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }

  public void scheduleAt(Instant instant) {
    this.nextSendAt = instant;
  }

  public void advance() {
    this.nextSendAt = Instant.now().plusSeconds(intervalDays * 24L * 3600L);
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UuidV7.generate();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public UUID getOrderItemId() {
    return orderItemId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public String getCustomerEmail() {
    return customerEmail;
  }

  public String getPlantName() {
    return plantName;
  }

  public int getIntervalDays() {
    return intervalDays;
  }

  public Instant getNextSendAt() {
    return nextSendAt;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
