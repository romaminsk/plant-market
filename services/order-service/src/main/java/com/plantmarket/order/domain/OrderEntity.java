package com.plantmarket.order.domain;

import com.plantmarket.common.id.UuidV7;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 30, nullable = false)
  private OrderStatus status;

  @Column(name = "subtotal_minor", nullable = false)
  private long subtotalMinor;

  @Column(name = "shipping_minor", nullable = false)
  private long shippingMinor;

  @Column(name = "tax_minor", nullable = false)
  private long taxMinor;

  @Column(name = "discount_minor", nullable = false)
  private long discountMinor;

  @Column(name = "total_minor", nullable = false)
  private long totalMinor;

  @Column(name = "currency", length = 3, nullable = false)
  private String currency;

  @Column(name = "reservation_expires_at")
  private Instant reservationExpiresAt;

  @OneToMany(
      mappedBy = "order",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<OrderItemEntity> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected OrderEntity() {}

  public OrderEntity(UUID customerId, String currency) {
    this.customerId = customerId;
    this.currency = currency;
    this.status = OrderStatus.DRAFT;
  }

  public void addItem(OrderItemEntity item) {
    items.add(item);
    subtotalMinor += item.getLineTotalMinor();
    totalMinor = subtotalMinor + shippingMinor + taxMinor - discountMinor;
  }

  public void markPendingPayment(Instant reservationExpiresAt) {
    transitionTo(OrderStatus.PENDING_PAYMENT);
    this.reservationExpiresAt = reservationExpiresAt;
  }

  public void markPaid() {
    transitionTo(OrderStatus.PAID);
  }

  public void cancel() {
    if (status != OrderStatus.DRAFT && status != OrderStatus.PENDING_PAYMENT) {
      throw new IllegalStateException("Order cannot be cancelled from " + status);
    }
    transitionTo(OrderStatus.CANCELLED);
  }

  private void transitionTo(OrderStatus target) {
    if (!isAllowed(status, target)) {
      throw new IllegalStateException("Illegal order transition from " + status + " to " + target);
    }
    this.status = target;
  }

  private boolean isAllowed(OrderStatus from, OrderStatus to) {
    return switch (from) {
      case DRAFT -> to == OrderStatus.PENDING_PAYMENT || to == OrderStatus.CANCELLED;
      case PENDING_PAYMENT -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
      case PAID -> to == OrderStatus.FULFILLING || to == OrderStatus.CANCELLED;
      case FULFILLING -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
      case SHIPPED -> to == OrderStatus.DELIVERED;
      case DELIVERED -> to == OrderStatus.COMPLETED;
      case COMPLETED, CANCELLED, FAILED -> false;
    };
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UuidV7.generate();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
    if (status == null) {
      status = OrderStatus.DRAFT;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public long getSubtotalMinor() {
    return subtotalMinor;
  }

  public long getShippingMinor() {
    return shippingMinor;
  }

  public long getTaxMinor() {
    return taxMinor;
  }

  public long getDiscountMinor() {
    return discountMinor;
  }

  public long getTotalMinor() {
    return totalMinor;
  }

  public String getCurrency() {
    return currency;
  }

  public Instant getReservationExpiresAt() {
    return reservationExpiresAt;
  }

  public List<OrderItemEntity> getItems() {
    return List.copyOf(items);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
