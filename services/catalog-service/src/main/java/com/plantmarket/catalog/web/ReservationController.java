package com.plantmarket.catalog.web;

import com.plantmarket.catalog.service.ReservationService;
import com.plantmarket.catalog.web.dto.ReservationRequest;
import com.plantmarket.catalog.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

  private final ReservationService reservationService;

  public ReservationController(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  @PostMapping
  public ReservationResponse reserve(@Valid @RequestBody ReservationRequest request) {
    return reservationService.reserve(request);
  }

  @PostMapping("/{orderId}/release")
  public ReservationResponse release(@PathVariable UUID orderId) {
    return reservationService.release(orderId);
  }

  @PostMapping("/{orderId}/confirm")
  public ReservationResponse confirm(@PathVariable UUID orderId) {
    return reservationService.confirm(orderId);
  }
}
