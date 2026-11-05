package com.plantmarket.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RoleEntity {

  @Id
  @Column(name = "name", length = 32, nullable = false)
  private String name;

  protected RoleEntity() {}

  public RoleEntity(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
