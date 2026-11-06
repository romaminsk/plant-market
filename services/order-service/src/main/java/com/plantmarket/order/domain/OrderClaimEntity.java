package com.plantmarket.order.domain;

import com.plantmarket.common.id.UuidV7;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_claims")
public class OrderClaimEntity {

  @Id private UUID id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "reason", length = 2000, nullable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private ClaimStatus status;

  @Column(name = "decision_comment", length = 2000)
  private String decisionComment;

  @ElementCollection
  @CollectionTable(name = "order_claim_photos", joinColumns = @JoinColumn(name = "claim_id"))
  @OrderColumn(name = "position")
  @Column(name = "url", length = 1000, nullable = false)
  private List<String> photoUrls = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  protected OrderClaimEntity() {}

  public static OrderClaimEntity create(
      UUID orderId, UUID customerId, String reason, List<String> photoUrls) {
    OrderClaimEntity claim = new OrderClaimEntity();
    claim.orderId = orderId;
    claim.customerId = customerId;
    claim.reason = reason;
    claim.photoUrls.addAll(photoUrls);
    claim.status = ClaimStatus.OPEN;
    return claim;
  }

  public void decide(ClaimStatus target, String comment) {
    if (status != ClaimStatus.OPEN) {
      throw new IllegalStateException("Claim is already decided: " + status);
    }
    this.status = target;
    this.decisionComment = comment;
    this.decidedAt = Instant.now();
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

  public UUID getCustomerId() {
    return customerId;
  }

  public String getReason() {
    return reason;
  }

  public ClaimStatus getStatus() {
    return status;
  }

  public String getDecisionComment() {
    return decisionComment;
  }

  public List<String> getPhotoUrls() {
    return List.copyOf(photoUrls);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }
}
