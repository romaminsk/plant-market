package com.plantmarket.payment.service;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.common.security.SecretPolicy;
import com.plantmarket.payment.client.PaymentProviderClient;
import com.plantmarket.payment.client.ProviderPayment;
import com.plantmarket.payment.domain.PaymentEntity;
import com.plantmarket.payment.domain.PaymentStatus;
import com.plantmarket.payment.repository.PaymentRepository;
import com.plantmarket.payment.repository.WebhookEventRepository;
import com.plantmarket.payment.web.dto.CreatePaymentRequest;
import com.plantmarket.payment.web.dto.PaymentResponse;
import com.plantmarket.payment.web.dto.PaymentWebhookRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final WebhookEventRepository webhookEventRepository;
  private final PaymentProviderClient providerClient;
  private final ObjectMapper objectMapper;
  private final String webhookSecret;
  private final long webhookToleranceSeconds;

  public PaymentService(
      PaymentRepository paymentRepository,
      WebhookEventRepository webhookEventRepository,
      PaymentProviderClient providerClient,
      ObjectMapper objectMapper,
      @Value("${app.payment.webhook-secret}") String webhookSecret,
      @Value("${app.payment.webhook-tolerance-seconds:300}") long webhookToleranceSeconds,
      @Value("${app.environment:dev}") String environment) {
    this.paymentRepository = paymentRepository;
    this.webhookEventRepository = webhookEventRepository;
    this.providerClient = providerClient;
    this.objectMapper = objectMapper;
    this.webhookSecret = webhookSecret;
    this.webhookToleranceSeconds = webhookToleranceSeconds;
    SecretPolicy.validateWebhookSecret(webhookSecret, environment);
  }

  @Transactional
  public PaymentResponse create(CreatePaymentRequest request) {
    return paymentRepository
        .findByOrderId(request.orderId())
        .map(existing -> {
          if (existing.getAmountMinor() != request.amountMinor()
              || !existing.getCurrency().equals(request.currency())) {
            throw ApiException.conflict(
                "Payment for order " + request.orderId() + " has different amount or currency");
          }
          return toResponse(existing);
        })
        .orElseGet(() -> {
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
          try {
            return toResponse(paymentRepository.saveAndFlush(payment));
          } catch (DataIntegrityViolationException exception) {
            return paymentRepository
                .findByOrderId(request.orderId())
                .map(this::toResponse)
                .orElseThrow(() -> exception);
          }
        });
  }

  @Transactional
  public PaymentResponse refund(UUID paymentId) {
    PaymentEntity payment =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> ApiException.notFound("Payment not found: " + paymentId));
    if (payment.getStatus() == PaymentStatus.REFUNDED) {
      return toResponse(payment);
    }
    if (payment.getStatus() != PaymentStatus.CONFIRMED) {
      throw ApiException.conflict("Only CONFIRMED payments can be refunded");
    }
    providerClient.refund(payment.getProviderRef());
    payment.updateStatus(PaymentStatus.REFUNDED);
    return toResponse(payment);
  }

  @Transactional(readOnly = true)
  public PaymentResponse get(UUID id) {
    return toResponse(
        paymentRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("Payment not found: " + id)));
  }

  @Transactional
  public PaymentResponse webhook(String timestamp, String eventId, String signature, String body) {
    long timestampSeconds = parseTimestamp(timestamp);
    if (Math.abs(Instant.now().getEpochSecond() - timestampSeconds) > webhookToleranceSeconds) {
      throw ApiException.unauthorized("Webhook timestamp is outside the allowed tolerance");
    }
    if (eventId == null || eventId.isBlank() || eventId.length() > 128) {
      throw ApiException.badRequest("Invalid webhook event id");
    }
    if (!WebhookSignature.matches(webhookSecret, timestamp, eventId, body, signature)) {
      throw ApiException.unauthorized("Invalid payment webhook signature");
    }
    PaymentWebhookRequest request = parseBody(body);
    if (webhookEventRepository.claim(eventId) == 0) {
      return toResponse(
          paymentRepository
              .findByProviderRef(request.providerRef())
              .orElseThrow(() -> ApiException.notFound("Payment not found")));
    }
    PaymentEntity payment =
        paymentRepository
            .findByProviderRef(request.providerRef())
            .orElseThrow(() -> ApiException.notFound("Payment not found"));
    PaymentStatus target = mapStatus(request.status());
    if (!canTransition(payment.getStatus(), target)) {
      throw ApiException.conflict(
          "Illegal payment transition from " + payment.getStatus() + " to " + target);
    }
    if (payment.getStatus() != target) {
      payment.updateStatus(target);
    }
    return toResponse(payment);
  }

  private static long parseTimestamp(String timestamp) {
    try {
      return Long.parseLong(timestamp);
    } catch (NumberFormatException exception) {
      throw ApiException.unauthorized("Invalid webhook timestamp");
    }
  }

  private PaymentWebhookRequest parseBody(String body) {
    PaymentWebhookRequest request;
    try {
      request = objectMapper.readValue(body, PaymentWebhookRequest.class);
    } catch (Exception exception) {
      throw ApiException.badRequest("Malformed webhook payload");
    }
    if (request == null
        || request.providerRef() == null
        || request.providerRef().isBlank()
        || request.status() == null
        || request.status().isBlank()) {
      throw ApiException.badRequest("Malformed webhook payload");
    }
    return request;
  }

  private static boolean canTransition(PaymentStatus from, PaymentStatus to) {
    if (from == to) {
      return true;
    }
    return switch (from) {
      case CREATED ->
          to == PaymentStatus.PENDING
              || to == PaymentStatus.CONFIRMED
              || to == PaymentStatus.FAILED;
      case PENDING -> to == PaymentStatus.CONFIRMED || to == PaymentStatus.FAILED;
      case CONFIRMED -> to == PaymentStatus.REFUNDED;
      case FAILED, REFUNDED -> false;
    };
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
