package com.plantmarket.payment.domain;

import com.plantmarket.common.id.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

  @Id private UUID id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "currency", length = 3, nullable = false)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private PaymentStatus status;

  @Column(name = "provider_ref", length = 128, unique = true)
  private String providerRef;

  @Column(name = "redirect_url", length = 1000)
  private String redirectUrl;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected PaymentEntity() {}

  public PaymentEntity(
      UUID orderId,
      long amountMinor,
      String currency,
      PaymentStatus status,
      String providerRef,
      String redirectUrl) {
    this.orderId = orderId;
    this.amountMinor = amountMinor;
    this.currency = currency;
    this.status = status;
    this.providerRef = providerRef;
    this.redirectUrl = redirectUrl;
  }

  public void updateStatus(PaymentStatus status) {
    this.status = status;
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

  public long getAmountMinor() {
    return amountMinor;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getProviderRef() {
    return providerRef;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
