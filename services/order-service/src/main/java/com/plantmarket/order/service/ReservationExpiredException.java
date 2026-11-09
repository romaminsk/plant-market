package com.plantmarket.order.service;

import com.plantmarket.common.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Raised when payment is attempted after the stock reservation expired. */
public class ReservationExpiredException extends ApiException {

  public ReservationExpiredException(UUID orderId) {
    super(
        HttpStatus.CONFLICT,
        PROBLEM_BASE + "reservation-expired",
        "Reservation expired for order " + orderId);
  }
}
