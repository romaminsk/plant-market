package com.plantmarket.payment.web;

import com.plantmarket.payment.service.PaymentService;
import com.plantmarket.payment.web.dto.CreatePaymentRequest;
import com.plantmarket.payment.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
    return paymentService.create(request);
  }

  @PostMapping("/{id}/refund")
  public PaymentResponse refund(@PathVariable UUID id) {
    return paymentService.refund(id);
  }

  @GetMapping("/{id}")
  public PaymentResponse get(@PathVariable UUID id) {
    return paymentService.get(id);
  }

  @PostMapping("/webhook")
  public PaymentResponse webhook(
      @RequestHeader(value = "X-Provider-Timestamp", required = false) String timestamp,
      @RequestHeader(value = "X-Provider-Event-Id", required = false) String eventId,
      @RequestHeader(value = "X-Provider-Signature", required = false) String signature,
      @RequestBody String body) {
    return paymentService.webhook(timestamp, eventId, signature, body);
  }
}
