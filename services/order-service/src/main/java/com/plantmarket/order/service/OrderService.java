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
import com.plantmarket.order.web.dto.OrderItemResponse;
import com.plantmarket.order.web.dto.OrderResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class OrderService {

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
  public OrderResponse create(UUID customerId, String customerEmail, CreateOrderRequest request) {
    OrderEntity order = new OrderEntity(customerId, customerEmail, "BYN");
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
    orderRepository.save(order);
    record(order, OrderStatus.DRAFT, "order.created");
    return toResponse(order);
  }

  @Transactional
  public OrderResponse checkout(UUID customerId, UUID orderId, CheckoutRequest request) {
    OrderEntity order = findOwned(customerId, orderId);
    if (order.getStatus() != OrderStatus.DRAFT) {
      throw ApiException.conflict("Only DRAFT orders can be checked out");
    }
    if (request != null && request.shippingZoneId() != null) {
      ShippingZoneEntity zone =
          shippingZoneRepository
              .findById(request.shippingZoneId())
              .filter(ShippingZoneEntity::isActive)
              .orElseThrow(() -> ApiException.badRequest("Shipping zone is not available"));
      boolean thermal = Boolean.TRUE.equals(request.thermalPack());
      if (zone.isWinterRequiresThermal() && !thermal) {
        throw ApiException.conflict("Thermal packaging is required for this zone");
      }
      long shipping =
          order.getSubtotalMinor() >= zone.getFreeFromMinor() ? 0 : zone.getPriceMinor();
      if (thermal) {
        shipping += zone.getThermalPackMinor();
      }
      order.applyShipping(zone.getId(), shipping, thermal);
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
    ReservationResult reservation = catalogClient.reserve(orderId, items);
    order.markPendingPayment(reservation.expiresAt());
    record(order, OrderStatus.PENDING_PAYMENT, "order.pending_payment");
    return toResponse(order);
  }

  @Transactional
  public OrderResponse pay(UUID customerId, UUID orderId) {
    OrderEntity order = findOwned(customerId, orderId);
    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
      throw ApiException.conflict("Only PENDING_PAYMENT orders can be paid");
    }
    PaymentResult payment =
        paymentClient.createPayment(orderId, order.getTotalMinor(), order.getCurrency());
    if (!"CONFIRMED".equals(payment.status())) {
      throw ApiException.conflict("Payment is not confirmed: " + payment.status());
    }
    catalogClient.confirm(orderId);
    order.markPaid();
    record(order, OrderStatus.PAID, "order.paid");
    return toResponse(order);
  }

  @Transactional
  public OrderResponse cancel(UUID customerId, UUID orderId) {
    OrderEntity order = findOwned(customerId, orderId);
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
  public List<OrderResponse> list(UUID customerId) {
    return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
        .map(this::toResponse)
        .toList();
  }

  private OrderEntity findOwned(UUID customerId, UUID orderId) {
    return orderRepository
        .findById(orderId)
        .filter(order -> order.getCustomerId().equals(customerId))
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
