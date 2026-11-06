package com.plantmarket.catalog.domain;

import com.plantmarket.common.id.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_batches")
public class StockBatchEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_variant_id", nullable = false)
  private ProductVariantEntity productVariant;

  @Column(name = "supplier", length = 200, nullable = false)
  private String supplier;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "remaining_quantity", nullable = false)
  private int remainingQuantity;

  @Column(name = "unit_cost_minor", nullable = false)
  private long unitCostMinor;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", length = 3, nullable = false)
  private Currency currency;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "quarantine_until", nullable = false)
  private Instant quarantineUntil;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private StockBatchStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected StockBatchEntity() {}

  public StockBatchEntity(
      ProductVariantEntity productVariant,
      String supplier,
      int quantity,
      long unitCostMinor,
      Currency currency,
      Instant receivedAt,
      Instant quarantineUntil) {
    this.productVariant = productVariant;
    this.supplier = supplier;
    this.quantity = quantity;
    this.remainingQuantity = quantity;
    this.unitCostMinor = unitCostMinor;
    this.currency = currency;
    this.receivedAt = receivedAt;
    this.quarantineUntil = quarantineUntil;
    this.status = StockBatchStatus.QUARANTINE;
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

  public void release() {
    if (status != StockBatchStatus.QUARANTINE) {
      throw new IllegalStateException("Only quarantine batches can be released");
    }
    status = StockBatchStatus.AVAILABLE;
  }

  public void adjustRemaining(int remaining) {
    if (remaining < 0 || remaining > quantity) {
      throw new IllegalArgumentException("remaining must be between 0 and " + quantity);
    }
    this.remainingQuantity = remaining;
    if (remaining == 0) {
      this.status = StockBatchStatus.DEPLETED;
    } else if (this.status == StockBatchStatus.DEPLETED) {
      this.status = StockBatchStatus.AVAILABLE;
    }
  }

  public UUID getId() {
    return id;
  }

  public ProductVariantEntity getProductVariant() {
    return productVariant;
  }

  public String getSupplier() {
    return supplier;
  }

  public int getQuantity() {
    return quantity;
  }

  public int getRemainingQuantity() {
    return remainingQuantity;
  }

  public long getUnitCostMinor() {
    return unitCostMinor;
  }

  public Currency getCurrency() {
    return currency;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getQuarantineUntil() {
    return quarantineUntil;
  }

  public StockBatchStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
