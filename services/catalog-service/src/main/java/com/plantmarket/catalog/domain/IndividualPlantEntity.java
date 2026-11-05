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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "individual_plants")
public class IndividualPlantEntity {

  private static final Map<IndividualPlantStatus, Set<IndividualPlantStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          IndividualPlantStatus.ACQUIRED,
          Set.of(IndividualPlantStatus.QUARANTINE, IndividualPlantStatus.MORTALITY),
          IndividualPlantStatus.QUARANTINE,
          Set.of(IndividualPlantStatus.AVAILABLE, IndividualPlantStatus.MORTALITY),
          IndividualPlantStatus.AVAILABLE,
          Set.of(IndividualPlantStatus.RESERVED, IndividualPlantStatus.MORTALITY),
          IndividualPlantStatus.RESERVED,
          Set.of(
              IndividualPlantStatus.SOLD,
              IndividualPlantStatus.AVAILABLE,
              IndividualPlantStatus.MORTALITY),
          IndividualPlantStatus.SOLD,
          Set.of(IndividualPlantStatus.DELIVERED, IndividualPlantStatus.RETURNED),
          IndividualPlantStatus.RETURNED,
          Set.of(IndividualPlantStatus.AVAILABLE, IndividualPlantStatus.MORTALITY),
          IndividualPlantStatus.DELIVERED,
          Set.of(),
          IndividualPlantStatus.MORTALITY,
          Set.of());

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductEntity product;

  @Column(name = "code", length = 64, nullable = false, unique = true)
  private String code;

  @Column(name = "description", length = 5000)
  private String description;

  @Column(name = "photo_url", length = 1000)
  private String photoUrl;

  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", length = 3, nullable = false)
  private Currency currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private IndividualPlantStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "care_level", length = 20, nullable = false)
  private CareLevel careLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "light", length = 20, nullable = false)
  private LightLevel light;

  @Column(name = "pet_safe", nullable = false)
  private boolean petSafe;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected IndividualPlantEntity() {}

  public IndividualPlantEntity(
      ProductEntity product,
      String code,
      String description,
      String photoUrl,
      long priceMinor,
      Currency currency,
      CareLevel careLevel,
      LightLevel light,
      boolean petSafe) {
    this.product = product;
    this.code = code;
    this.description = description;
    this.photoUrl = photoUrl;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.careLevel = careLevel;
    this.light = light;
    this.petSafe = petSafe;
    this.status = IndividualPlantStatus.ACQUIRED;
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
      String code,
      String description,
      String photoUrl,
      long priceMinor,
      Currency currency,
      CareLevel careLevel,
      LightLevel light,
      boolean petSafe) {
    this.code = code;
    this.description = description;
    this.photoUrl = photoUrl;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.careLevel = careLevel;
    this.light = light;
    this.petSafe = petSafe;
  }

  public boolean canTransitionTo(IndividualPlantStatus target) {
    return ALLOWED_TRANSITIONS.getOrDefault(status, Set.of()).contains(target);
  }

  public void transitionTo(IndividualPlantStatus target) {
    if (!canTransitionTo(target)) {
      throw new IllegalStateException(
          "Cannot move individual plant from " + status + " to " + target);
    }
    this.status = target;
  }

  public UUID getId() {
    return id;
  }

  public ProductEntity getProduct() {
    return product;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public long getPriceMinor() {
    return priceMinor;
  }

  public Currency getCurrency() {
    return currency;
  }

  public IndividualPlantStatus getStatus() {
    return status;
  }

  public CareLevel getCareLevel() {
    return careLevel;
  }

  public LightLevel getLight() {
    return light;
  }

  public boolean isPetSafe() {
    return petSafe;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
