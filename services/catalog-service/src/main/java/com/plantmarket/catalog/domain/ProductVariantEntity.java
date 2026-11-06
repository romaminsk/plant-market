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
@Table(name = "product_variants")
public class ProductVariantEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductEntity product;

  @Column(name = "sku", length = 64, nullable = false, unique = true)
  private String sku;

  @Column(name = "name", length = 120, nullable = false)
  private String name;

  @Column(name = "size", length = 32)
  private String size;

  @Column(name = "pot_size", length = 32)
  private String potSize;

  @Column(name = "form", length = 32)
  private String form;

  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  @Column(name = "reserved_quantity", nullable = false)
  private int reservedQuantity;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", length = 3, nullable = false)
  private Currency currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private VariantStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ProductVariantEntity() {}

  public ProductVariantEntity(
      ProductEntity product,
      String sku,
      String name,
      String size,
      String potSize,
      String form,
      long priceMinor,
      Currency currency,
      VariantStatus status) {
    this.product = product;
    this.sku = sku;
    this.name = name;
    this.size = size;
    this.potSize = potSize;
    this.form = form;
    this.priceMinor = priceMinor;
    this.currency = currency;
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

  public void update(
      String sku,
      String name,
      String size,
      String potSize,
      String form,
      long priceMinor,
      Currency currency,
      VariantStatus status) {
    this.sku = sku;
    this.name = name;
    this.size = size;
    this.potSize = potSize;
    this.form = form;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.status = status;
  }

  public void archive() {
    this.status = VariantStatus.ARCHIVED;
  }

  public void reserve(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    this.reservedQuantity += quantity;
  }

  public void release(int quantity) {
    if (quantity <= 0 || quantity > reservedQuantity) {
      throw new IllegalArgumentException("invalid release quantity");
    }
    this.reservedQuantity -= quantity;
  }

  public UUID getId() {
    return id;
  }

  public ProductEntity getProduct() {
    return product;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public String getSize() {
    return size;
  }

  public String getPotSize() {
    return potSize;
  }

  public String getForm() {
    return form;
  }

  public long getPriceMinor() {
    return priceMinor;
  }

  public int getReservedQuantity() {
    return reservedQuantity;
  }

  public Currency getCurrency() {
    return currency;
  }

  public VariantStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
