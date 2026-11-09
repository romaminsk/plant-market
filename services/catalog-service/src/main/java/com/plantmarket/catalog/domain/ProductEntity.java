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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {

  @Id private UUID id;

  @Column(name = "sku", length = 64, nullable = false, unique = true)
  private String sku;

  @Column(name = "name", length = 200, nullable = false)
  private String name;

  @Column(name = "slug", length = 200, nullable = false, unique = true)
  private String slug;

  @Column(name = "description", length = 5000)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CategoryEntity category;

  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", length = 3, nullable = false)
  private Currency currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private ProductStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "care_level", length = 20, nullable = false)
  private CareLevel careLevel;

  @Enumerated(EnumType.STRING)
  @Column(name = "light", length = 20, nullable = false)
  private LightLevel light;

  @Column(name = "pet_safe", nullable = false)
  private boolean petSafe;

  @Column(name = "variegated", nullable = false)
  private boolean variegated;

  @Column(name = "size", length = 32)
  private String size;

  @Column(name = "care_guide", length = 5000)
  private String careGuide;

  @Column(name = "image_url", length = 1000)
  private String imageUrl;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ProductEntity() {}

  public ProductEntity(
      String sku,
      String name,
      String slug,
      String description,
      CategoryEntity category,
      long priceMinor,
      Currency currency,
      ProductStatus status,
      CareLevel careLevel,
      LightLevel light,
      boolean petSafe,
      boolean variegated,
      String size,
      String careGuide,
      String imageUrl) {
    this.sku = sku;
    this.name = name;
    this.slug = slug;
    this.description = description;
    this.category = category;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.status = status;
    this.careLevel = careLevel;
    this.light = light;
    this.petSafe = petSafe;
    this.variegated = variegated;
    this.size = size;
    this.careGuide = careGuide;
    this.imageUrl = imageUrl;
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
      String slug,
      String description,
      CategoryEntity category,
      long priceMinor,
      Currency currency,
      ProductStatus status,
      CareLevel careLevel,
      LightLevel light,
      boolean petSafe,
      boolean variegated,
      String size,
      String careGuide,
      String imageUrl) {
    this.sku = sku;
    this.name = name;
    this.slug = slug;
    this.description = description;
    this.category = category;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.status = status;
    this.careLevel = careLevel;
    this.light = light;
    this.petSafe = petSafe;
    this.variegated = variegated;
    this.size = size;
    this.careGuide = careGuide;
    this.imageUrl = imageUrl;
  }

  public void archive() {
    this.status = ProductStatus.ARCHIVED;
  }

  public UUID getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getDescription() {
    return description;
  }

  public CategoryEntity getCategory() {
    return category;
  }

  public long getPriceMinor() {
    return priceMinor;
  }

  public Currency getCurrency() {
    return currency;
  }

  public ProductStatus getStatus() {
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

  public boolean isVariegated() {
    return variegated;
  }

  public String getSize() {
    return size;
  }

  public String getCareGuide() {
    return careGuide;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
