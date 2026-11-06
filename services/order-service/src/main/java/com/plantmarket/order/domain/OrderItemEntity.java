package com.plantmarket.order.domain;

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
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderEntity order;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", length = 20, nullable = false)
  private OrderItemType itemType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Column(name = "name", length = 200, nullable = false)
  private String name;

  @Column(name = "unit_price_minor", nullable = false)
  private long unitPriceMinor;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "line_total_minor", nullable = false)
  private long lineTotalMinor;

  protected OrderItemEntity() {}

  public OrderItemEntity(
      OrderEntity order,
      OrderItemType itemType,
      UUID targetId,
      String name,
      long unitPriceMinor,
      int quantity) {
    this.order = order;
    this.itemType = itemType;
    this.targetId = targetId;
    this.name = name;
    this.unitPriceMinor = unitPriceMinor;
    this.quantity = quantity;
    this.lineTotalMinor = unitPriceMinor * quantity;
  }

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UuidV7.generate();
    }
  }

  public UUID getId() {
    return id;
  }

  public OrderEntity getOrder() {
    return order;
  }

  public OrderItemType getItemType() {
    return itemType;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public String getName() {
    return name;
  }

  public long getUnitPriceMinor() {
    return unitPriceMinor;
  }

  public int getQuantity() {
    return quantity;
  }

  public long getLineTotalMinor() {
    return lineTotalMinor;
  }
}
