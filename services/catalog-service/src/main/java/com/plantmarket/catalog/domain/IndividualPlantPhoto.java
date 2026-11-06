package com.plantmarket.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class IndividualPlantPhoto {

  @Column(name = "url", length = 1000, nullable = false)
  private String url;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", length = 20, nullable = false)
  private PlantPhotoKind kind;

  protected IndividualPlantPhoto() {}

  public IndividualPlantPhoto(String url, PlantPhotoKind kind) {
    this.url = url;
    this.kind = kind;
  }

  public String getUrl() {
    return url;
  }

  public PlantPhotoKind getKind() {
    return kind;
  }
}
