package com.plantmarket.order.service;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.order.domain.ClaimDecision;
import com.plantmarket.order.domain.ClaimStatus;
import com.plantmarket.order.domain.OrderClaimEntity;
import com.plantmarket.order.domain.OrderEntity;
import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.notification.ClaimDecidedEvent;
import com.plantmarket.order.repository.OrderClaimRepository;
import com.plantmarket.order.repository.OrderRepository;
import com.plantmarket.order.web.dto.ClaimDecisionRequest;
import com.plantmarket.order.web.dto.ClaimRequest;
import com.plantmarket.order.web.dto.ClaimResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {

  private static final Set<OrderStatus> CLAIMABLE_STATUSES =
      Set.of(
          OrderStatus.PAID,
          OrderStatus.FULFILLING,
          OrderStatus.SHIPPED,
          OrderStatus.DELIVERED,
          OrderStatus.COMPLETED);

  private final OrderClaimRepository claimRepository;
  private final OrderRepository orderRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ClaimService(
      OrderClaimRepository claimRepository,
      OrderRepository orderRepository,
      ApplicationEventPublisher eventPublisher) {
    this.claimRepository = claimRepository;
    this.orderRepository = orderRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public ClaimResponse open(UUID customerId, UUID orderId, ClaimRequest request) {
    OrderEntity order = findOwnedOrder(customerId, orderId);
    if (!CLAIMABLE_STATUSES.contains(order.getStatus())) {
      throw ApiException.conflict("Claims are only allowed for paid orders");
    }
    OrderClaimEntity claim =
        OrderClaimEntity.create(orderId, customerId, request.reason(), request.photoUrls());
    claimRepository.save(claim);
    return toResponse(claim);
  }

  @Transactional(readOnly = true)
  public List<ClaimResponse> listForOrder(UUID customerId, UUID orderId) {
    findOwnedOrder(customerId, orderId);
    return claimRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClaimResponse> list(ClaimStatus status) {
    List<OrderClaimEntity> claims =
        status == null
            ? claimRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            : claimRepository.findByStatusOrderByCreatedAtDesc(status);
    return claims.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ClaimResponse get(UUID claimId) {
    return toResponse(find(claimId));
  }

  @Transactional
  public ClaimResponse decide(UUID claimId, ClaimDecisionRequest request) {
    OrderClaimEntity claim = find(claimId);
    ClaimStatus target =
        request.decision() == ClaimDecision.REJECT
            ? ClaimStatus.REJECTED
            : ClaimStatus.valueOf(request.decision().name());
    try {
      claim.decide(target, request.comment());
    } catch (IllegalStateException exception) {
      throw ApiException.conflict(exception.getMessage());
    }
    claimRepository.save(claim);
    String customerEmail =
        orderRepository
            .findById(claim.getOrderId())
            .map(OrderEntity::getCustomerEmail)
            .orElse(null);
    eventPublisher.publishEvent(
        new ClaimDecidedEvent(claim.getId(), claim.getOrderId(), customerEmail, claim.getStatus()));
    return toResponse(claim);
  }

  private OrderEntity findOwnedOrder(UUID customerId, UUID orderId) {
    return orderRepository
        .findById(orderId)
        .filter(order -> order.getCustomerId().equals(customerId))
        .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
  }

  private OrderClaimEntity find(UUID claimId) {
    return claimRepository
        .findById(claimId)
        .orElseThrow(() -> ApiException.notFound("Claim not found: " + claimId));
  }

  private ClaimResponse toResponse(OrderClaimEntity claim) {
    return new ClaimResponse(
        claim.getId(),
        claim.getOrderId(),
        claim.getCustomerId(),
        claim.getStatus(),
        claim.getReason(),
        claim.getPhotoUrls(),
        claim.getDecisionComment(),
        claim.getCreatedAt(),
        claim.getUpdatedAt(),
        claim.getDecidedAt());
  }
}
