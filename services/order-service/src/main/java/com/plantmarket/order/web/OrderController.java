package com.plantmarket.order.web;

import com.plantmarket.order.service.OrderService;
import com.plantmarket.order.web.dto.CheckoutRequest;
import com.plantmarket.order.web.dto.CreateOrderRequest;
import com.plantmarket.order.web.dto.OrderHistoryEntry;
import com.plantmarket.order.web.dto.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequest request) {
    return orderService.create(customerId(jwt), jwt.getClaimAsString("email"), request);
  }

  @GetMapping
  public List<OrderResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return orderService.list(customerId(jwt));
  }

  @GetMapping("/{id}/history")
  public List<OrderHistoryEntry> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return orderService.history(customerId(jwt), id);
  }

  @GetMapping("/{id}")
  public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return orderService.get(customerId(jwt), id);
  }

  @PostMapping("/{id}/checkout")
  public OrderResponse checkout(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody(required = false) CheckoutRequest request) {
    return orderService.checkout(customerId(jwt), id, request);
  }

  @PostMapping("/{id}/pay")
  public OrderResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return orderService.pay(customerId(jwt), id);
  }

  @PostMapping("/{id}/cancel")
  public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return orderService.cancel(customerId(jwt), id);
  }

  private UUID customerId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
