package com.plantmarket.payment.service;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.payment.client.PaymentProviderClient;
import com.plantmarket.payment.client.ProviderPayment;
import com.plantmarket.payment.domain.PaymentEntity;
import com.plantmarket.payment.domain.PaymentStatus;
import com.plantmarket.payment.repository.PaymentRepository;
import com.plantmarket.payment.web.dto.CreatePaymentRequest;
import com.plantmarket.payment.web.dto.PaymentResponse;
import com.plantmarket.payment.web.dto.PaymentWebhookRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentProviderClient providerClient;
  private final String webhookSecret;

  public PaymentService(
      PaymentRepository paymentRepository,
      PaymentProviderClient providerClient,
      @Value("${app.payment.webhook-secret}") String webhookSecret) {
    this.paymentRepository = paymentRepository;
    this.providerClient = providerClient;
    this.webhookSecret = webhookSecret;
  }

  @Transactional
  public PaymentResponse create(CreatePaymentRequest request) {
    ProviderPayment providerPayment =
        providerClient.create(request.orderId(), request.amountMinor(), request.currency());
    PaymentEntity payment =
        new PaymentEntity(
            request.orderId(),
            request.amountMinor(),
            request.currency(),
            mapStatus(providerPayment.status()),
            providerPayment.id(),
            providerPayment.redirectUrl());
    return toResponse(paymentRepository.save(payment));
  }

  @Transactional(readOnly = true)
  public PaymentResponse get(UUID id) {
    return toResponse(
        paymentRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("Payment not found: " + id)));
  }

  @Transactional
  public PaymentResponse webhook(String secret, PaymentWebhookRequest request) {
    if (!webhookSecret.equals(secret)) {
      throw ApiException.unauthorized("Invalid payment webhook secret");
    }
    PaymentEntity payment =
        paymentRepository
            .findByProviderRef(request.providerRef())
            .orElseThrow(() -> ApiException.notFound("Payment not found"));
    payment.updateStatus(mapStatus(request.status()));
    return toResponse(payment);
  }

  private PaymentStatus mapStatus(String providerStatus) {
    return switch (providerStatus) {
      case "AUTHORIZED", "CONFIRMED" -> PaymentStatus.CONFIRMED;
      case "PENDING" -> PaymentStatus.PENDING;
      case "REFUNDED" -> PaymentStatus.REFUNDED;
      case "FAILED" -> PaymentStatus.FAILED;
      default -> throw ApiException.badRequest("Unknown provider status: " + providerStatus);
    };
  }

  private PaymentResponse toResponse(PaymentEntity payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getOrderId(),
        payment.getAmountMinor(),
        payment.getCurrency(),
        payment.getStatus(),
        payment.getProviderRef(),
        payment.getRedirectUrl(),
        payment.getCreatedAt(),
        payment.getUpdatedAt());
  }
}
