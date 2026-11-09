package com.plantmarket.order.service;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.order.client.CatalogClient;
import com.plantmarket.order.client.CatalogPlant;
import com.plantmarket.order.client.CatalogVariant;
import com.plantmarket.order.client.PaymentClient;
import com.plantmarket.order.client.PaymentResult;
import com.plantmarket.order.client.ReservationRequest;
import com.plantmarket.order.client.ReservationResult;
import com.plantmarket.order.client.ReservationTargetType;
import com.plantmarket.order.domain.OrderEntity;
import com.plantmarket.order.domain.OrderItemEntity;
import com.plantmarket.order.domain.OrderItemType;
import com.plantmarket.order.domain.OrderStatus;
import com.plantmarket.order.domain.OrderStatusHistoryEntity;
import com.plantmarket.order.domain.OutboxEventEntity;
import com.plantmarket.order.domain.ShippingZoneEntity;
import com.plantmarket.order.repository.OrderRepository;
import com.plantmarket.order.repository.OrderStatusHistoryRepository;
import com.plantmarket.order.repository.OutboxEventRepository;
import com.plantmarket.order.repository.ShippingZoneRepository;
import com.plantmarket.order.web.dto.CheckoutRequest;
import com.plantmarket.order.web.dto.CreateOrderItemRequest;
import com.plantmarket.order.web.dto.CreateOrderRequest;
import com.plantmarket.order.web.dto.GuestCheckoutRequest;
import com.plantmarket.order.web.dto.OrderHistoryEntry;
import com.plantmarket.order.web.dto.OrderItemResponse;
import com.plantmarket.order.web.dto.OrderResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final OrderStatusHistoryRepository historyRepository;
  private final OutboxEventRepository outboxRepository;
  private final CatalogClient catalogClient;
  private final PaymentClient paymentClient;
  private final ShippingZoneRepository shippingZoneRepository;
  private final ObjectMapper objectMapper;

  public OrderService(
      OrderRepository orderRepository,
      OrderStatusHistoryRepository historyRepository,
      OutboxEventRepository outboxRepository,
      CatalogClient catalogClient,
      PaymentClient paymentClient,
      ShippingZoneRepository shippingZoneRepository,
      ObjectMapper objectMapper) {
    this.orderRepository = orderRepository;
    this.historyRepository = historyRepository;
    this.outboxRepository = outboxRepository;
    this.catalogClient = catalogClient;
    this.paymentClient = paymentClient;
    this.shippingZoneRepository = shippingZoneRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public OrderResponse create(
      UUID customerId, String customerEmail, CreateOrderRequest request, String idempotencyKey) {
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      return orderRepository
          .findByIdempotencyKey(idempotencyKey)
          .map(this::toResponse)
          .orElseGet(() -> createNew(customerId, customerEmail, request, idempotencyKey));
    }
    return createNew(customerId, customerEmail, request, null);
  }

  private OrderResponse createNew(
      UUID customerId, String customerEmail, CreateOrderRequest request, String idempotencyKey) {
    OrderEntity order = new OrderEntity(customerId, customerEmail, "BYN", idempotencyKey);
    for (CreateOrderItemRequest item : request.items()) {
      boolean hasVariant = item.variantId() != null;
      boolean hasPlant = item.individualPlantId() != null;
      if (hasVariant == hasPlant) {
        throw ApiException.badRequest("Exactly one of variantId or individualPlantId is required");
      }
      if (hasVariant) {
        CatalogVariant variant = catalogClient.getVariant(item.variantId());
        if (!"ACTIVE".equals(variant.status())) {
          throw ApiException.conflict("Variant is not active: " + variant.sku());
        }
        order.addItem(
            new OrderItemEntity(
                order,
                OrderItemType.VARIANT,
                variant.id(),
                variant.name(),
                variant.priceMinor(),
                item.quantity()));
      } else {
        CatalogPlant plant = catalogClient.getPlant(item.individualPlantId());
        if (!"AVAILABLE".equals(plant.status())) {
          throw ApiException.conflict("Individual plant is not available: " + plant.code());
        }
        order.addItem(
            new OrderItemEntity(
                order,
                OrderItemType.INDIVIDUAL_PLANT,
                plant.id(),
                plant.productName() + " (" + plant.code() + ")",
                plant.priceMinor(),
                item.quantity()));
      }
    }
    try {
      orderRepository.saveAndFlush(order);
    } catch (DataIntegrityViolationException exception) {
      if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        return orderRepository
            .findByIdempotencyKey(idempotencyKey)
            .map(this::toResponse)
            .orElseThrow(() -> exception);
      }
      throw exception;
    }
    record(order, OrderStatus.DRAFT, "order.created");
    return toResponse(order);
  }

  @Transactional
  public OrderResponse checkout(UUID customerId, UUID orderId, CheckoutRequest request) {
    OrderEntity order = findOwnedForUpdate(customerId, orderId);
    checkoutOrder(order, request);
    return toResponse(order);
  }

  @Transactional(noRollbackFor = ReservationExpiredException.class)
  public OrderResponse guestCheckout(GuestCheckoutRequest request, String idempotencyKey) {
    OrderResponse created =
        create(null, request.guestEmail(), new CreateOrderRequest(request.items()), idempotencyKey);
    OrderEntity order =
        orderRepository
            .findByIdForUpdate(created.id())
            .orElseThrow(() -> ApiException.notFound("Order not found: " + created.id()));
    if (order.getStatus() == OrderStatus.DRAFT) {
      checkoutOrder(
          order,
          new CheckoutRequest(
              request.shippingZoneId(), request.thermalPack(), request.deliveryAddress()));
    }
    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
      payOrder(order);
    }
    return toResponse(order);
  }

  private void checkoutOrder(OrderEntity order, CheckoutRequest request) {
    if (order.getStatus() != OrderStatus.DRAFT) {
      throw ApiException.conflict("Only DRAFT orders can be checked out");
    }
    if (request != null && request.shippingZoneId() != null) {
      ShippingZoneEntity zone =
          shippingZoneRepository
              .findById(request.shippingZoneId())
              .filter(ShippingZoneEntity::isActive)
              .orElseThrow(() -> ApiException.badRequest("Shipping zone is not available"));
      String address = request.deliveryAddress() == null ? null : request.deliveryAddress().trim();
      if (address == null || address.isBlank()) {
        throw ApiException.badRequest("deliveryAddress is required for delivery");
      }
      boolean thermal = Boolean.TRUE.equals(request.thermalPack());
      if (zone.isWinterRequiresThermal() && !thermal) {
        throw ApiException.conflict("Thermal packaging is required for this zone");
      }
      long shipping =
          order.getSubtotalMinor() >= zone.getFreeFromMinor() ? 0 : zone.getPriceMinor();
      if (thermal) {
        shipping += zone.getThermalPackMinor();
      }
      order.applyShipping(zone.getId(), shipping, thermal, address);
    }
    List<ReservationRequest.Item> items =
        order.getItems().stream()
            .map(item ->
                new ReservationRequest.Item(
                    item.getItemType() == OrderItemType.VARIANT
                        ? ReservationTargetType.VARIANT
                        : ReservationTargetType.INDIVIDUAL_PLANT,
                    item.getTargetId(),
                    item.getQuantity()))
            .toList();
    ReservationResult reservation = catalogClient.reserve(order.getId(), items);
    order.markPendingPayment(reservation.expiresAt());
    record(order, OrderStatus.PENDING_PAYMENT, "order.pending_payment");
  }

  @Transactional(noRollbackFor = ReservationExpiredException.class)
  public OrderResponse pay(UUID customerId, UUID orderId) {
    OrderEntity order = findOwnedForUpdate(customerId, orderId);
    payOrder(order);
    return toResponse(order);
  }

  private void payOrder(OrderEntity order) {
    if (order.getStatus() == OrderStatus.PAID) {
      return;
    }
    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
      throw ApiException.conflict("Only PENDING_PAYMENT orders can be paid");
    }
    if (isExpired(order)) {
      cancelExpiredOrder(order);
      throw new ReservationExpiredException(order.getId());
    }
    PaymentResult payment =
        paymentClient.createPayment(order.getId(), order.getTotalMinor(), order.getCurrency());
    if (!"CONFIRMED".equals(payment.status())) {
      throw ApiException.conflict("Payment is not confirmed: " + payment.status());
    }
    try {
      catalogClient.confirm(order.getId());
    } catch (RuntimeException exception) {
      log.error("Stock confirmation failed for order {}; compensating", order.getId(), exception);
      try {
        paymentClient.refund(payment.id());
      } catch (RuntimeException refundFailure) {
        log.error("Payment refund failed for payment {}", payment.id(), refundFailure);
      }
      try {
        catalogClient.release(order.getId());
      } catch (RuntimeException releaseFailure) {
        log.warn("Reservation release failed for order {}", order.getId(), releaseFailure);
      }
      throw ApiException.conflict("Could not confirm stock; payment was refunded");
    }
    order.markPaid();
    record(order, OrderStatus.PAID, "order.paid");
  }

  /** Cancels one expired order in its own transaction; safe to call from the expiry scanner. */
  @Transactional
  public boolean cancelExpired(UUID orderId) {
    OrderEntity order = orderRepository.findByIdForUpdate(orderId).orElse(null);
    if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT || !isExpired(order)) {
      return false;
    }
    cancelExpiredOrder(order);
    return true;
  }

  private boolean isExpired(OrderEntity order) {
    Instant expiresAt = order.getReservationExpiresAt();
    return expiresAt != null && !expiresAt.isAfter(Instant.now());
  }

  private void cancelExpiredOrder(OrderEntity order) {
    try {
      catalogClient.release(order.getId());
    } catch (RuntimeException exception) {
      log.warn("Reservation release failed for expired order {}", order.getId(), exception);
    }
    order.cancel();
    record(order, OrderStatus.CANCELLED, "order.expired");
  }

  @Transactional
  public OrderResponse cancel(UUID customerId, UUID orderId) {
    OrderEntity order = findOwnedForUpdate(customerId, orderId);
    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
      catalogClient.release(orderId);
    }
    try {
      order.cancel();
    } catch (IllegalStateException exception) {
      throw ApiException.conflict(exception.getMessage());
    }
    record(order, OrderStatus.CANCELLED, "order.cancelled");
    return toResponse(order);
  }

  @Transactional(readOnly = true)
  public OrderResponse get(UUID customerId, UUID orderId) {
    return toResponse(findOwned(customerId, orderId));
  }

  @Transactional(readOnly = true)
  public List<OrderHistoryEntry> history(UUID customerId, UUID orderId) {
    findOwned(customerId, orderId);
    return historyRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
        .map(entry -> new OrderHistoryEntry(entry.getStatus(), entry.getCreatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> list(UUID customerId) {
    return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
        .map(this::toResponse)
        .toList();
  }

  private OrderEntity findOwned(UUID customerId, UUID orderId) {
    return orderRepository
        .findById(orderId)
        .filter(order -> customerId.equals(order.getCustomerId()))
        .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
  }

  private OrderEntity findOwnedForUpdate(UUID customerId, UUID orderId) {
    return orderRepository
        .findByIdForUpdate(orderId)
        .filter(order -> customerId.equals(order.getCustomerId()))
        .orElseThrow(() -> ApiException.notFound("Order not found: " + orderId));
  }

  private void record(OrderEntity order, OrderStatus status, String eventType) {
    historyRepository.save(new OrderStatusHistoryEntity(order.getId(), status));
    try {
      String payload =
          objectMapper.writeValueAsString(
              Map.of("orderId", order.getId(), "status", status.name()));
      outboxRepository.save(new OutboxEventEntity(order.getId(), eventType, payload));
    } catch (Exception exception) {
      throw new IllegalStateException("Could not serialize outbox event", exception);
    }
  }

  private OrderResponse toResponse(OrderEntity order) {
    List<OrderItemResponse> items =
        order.getItems().stream()
            .map(item ->
                new OrderItemResponse(
                    item.getId(),
                    item.getItemType(),
                    item.getTargetId(),
                    item.getName(),
                    item.getUnitPriceMinor(),
                    item.getQuantity(),
                    item.getLineTotalMinor()))
            .toList();
    return new OrderResponse(
        order.getId(),
        order.getCustomerId(),
        order.getCustomerEmail(),
        order.getShippingZoneId(),
        order.isThermalPack(),
        order.getDeliveryAddress(),
        order.getStatus(),
        order.getSubtotalMinor(),
        order.getShippingMinor(),
        order.getTaxMinor(),
        order.getDiscountMinor(),
        order.getTotalMinor(),
        order.getCurrency(),
        order.getReservationExpiresAt(),
        items,
        order.getCreatedAt(),
        order.getUpdatedAt());
  }
}
