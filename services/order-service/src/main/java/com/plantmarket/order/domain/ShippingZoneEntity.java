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
@Table(name = "shipping_zones")
public class ShippingZoneEntity {

  @Id private UUID id;

  @Column(name = "name", length = 120, nullable = false)
  private String name;

  @Column(name = "city", length = 120, nullable = false)
  private String city;

  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  @Column(name = "free_from_minor", nullable = false)
  private long freeFromMinor;

  @Column(name = "min_days", nullable = false)
  private int minDays;

  @Column(name = "max_days", nullable = false)
  private int maxDays;

  @Column(name = "winter_requires_thermal", nullable = false)
  private boolean winterRequiresThermal;

  @Column(name = "thermal_pack_minor", nullable = false)
  private long thermalPackMinor;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ShippingZoneEntity() {}

  public ShippingZoneEntity(
      String name,
      String city,
      long priceMinor,
      long freeFromMinor,
      int minDays,
      int maxDays,
      boolean winterRequiresThermal,
      long thermalPackMinor) {
    this.name = name;
    this.city = city;
    this.priceMinor = priceMinor;
    this.freeFromMinor = freeFromMinor;
    this.minDays = minDays;
    this.maxDays = maxDays;
    this.winterRequiresThermal = winterRequiresThermal;
    this.thermalPackMinor = thermalPackMinor;
    this.active = true;
  }

  public void update(
      String name,
      String city,
      long priceMinor,
      long freeFromMinor,
      int minDays,
      int maxDays,
      boolean winterRequiresThermal,
      long thermalPackMinor,
      boolean active) {
    this.name = name;
    this.city = city;
    this.priceMinor = priceMinor;
    this.freeFromMinor = freeFromMinor;
    this.minDays = minDays;
    this.maxDays = maxDays;
    this.winterRequiresThermal = winterRequiresThermal;
    this.thermalPackMinor = thermalPackMinor;
    this.active = active;
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

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }

  public long getPriceMinor() {
    return priceMinor;
  }

  public long getFreeFromMinor() {
    return freeFromMinor;
  }

  public int getMinDays() {
    return minDays;
  }

  public int getMaxDays() {
    return maxDays;
  }

  public boolean isWinterRequiresThermal() {
    return winterRequiresThermal;
  }

  public long getThermalPackMinor() {
    return thermalPackMinor;
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
