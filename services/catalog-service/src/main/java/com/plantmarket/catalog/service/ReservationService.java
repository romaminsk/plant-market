package com.plantmarket.catalog.service;

import com.plantmarket.catalog.domain.IndividualPlantEntity;
import com.plantmarket.catalog.domain.IndividualPlantStatus;
import com.plantmarket.catalog.domain.ProductStatus;
import com.plantmarket.catalog.domain.ProductVariantEntity;
import com.plantmarket.catalog.domain.ReservationEntity;
import com.plantmarket.catalog.domain.ReservationStatus;
import com.plantmarket.catalog.domain.ReservationTargetType;
import com.plantmarket.catalog.domain.StockBatchEntity;
import com.plantmarket.catalog.domain.StockBatchStatus;
import com.plantmarket.catalog.repository.IndividualPlantRepository;
import com.plantmarket.catalog.repository.ProductVariantRepository;
import com.plantmarket.catalog.repository.ReservationRepository;
import com.plantmarket.catalog.repository.StockBatchRepository;
import com.plantmarket.catalog.web.dto.ReservationItemRequest;
import com.plantmarket.catalog.web.dto.ReservationRequest;
import com.plantmarket.catalog.web.dto.ReservationResponse;
import com.plantmarket.common.error.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

  private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
  private static final Duration RESERVATION_TTL = Duration.ofMinutes(15);

  private final ReservationRepository reservationRepository;
  private final ProductVariantRepository variantRepository;
  private final IndividualPlantRepository plantRepository;
  private final StockBatchRepository batchRepository;

  public ReservationService(
      ReservationRepository reservationRepository,
      ProductVariantRepository variantRepository,
      IndividualPlantRepository plantRepository,
      StockBatchRepository batchRepository) {
    this.reservationRepository = reservationRepository;
    this.variantRepository = variantRepository;
    this.plantRepository = plantRepository;
    this.batchRepository = batchRepository;
  }

  @Transactional
  public ReservationResponse reserve(ReservationRequest request) {
    Instant expiresAt = Instant.now().plus(RESERVATION_TTL);
    List<ReservationEntity> created = new ArrayList<>();
    for (ReservationItemRequest item : request.items()) {
      if (item.targetType() == ReservationTargetType.VARIANT) {
        created.add(reserveVariant(request.orderId(), item, expiresAt));
      } else {
        created.add(reservePlant(request.orderId(), item, expiresAt));
      }
    }
    return response(request.orderId(), created);
  }

  @Transactional
  public ReservationResponse release(UUID orderId) {
    List<ReservationEntity> active =
        reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
    for (ReservationEntity reservation : active) {
      releaseOne(reservation);
      reservation.release();
    }
    return response(orderId, active);
  }

  @Transactional
  public ReservationResponse confirm(UUID orderId) {
    List<ReservationEntity> active =
        reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
    if (active.isEmpty()) {
      throw ApiException.conflict("No active reservations for order " + orderId);
    }
    for (ReservationEntity reservation : active) {
      if (reservation.getExpiresAt().isBefore(Instant.now())) {
        throw ApiException.conflict("Reservation has expired for order " + orderId);
      }
      confirmOne(reservation);
      reservation.confirm();
    }
    return response(orderId, active);
  }

  @Scheduled(fixedDelayString = "${app.reservations.expiry-scan-ms:60000}")
  @Transactional
  public void expireReservations() {
    List<ReservationEntity> expired =
        reservationRepository.findByStatusAndExpiresAtBefore(
            ReservationStatus.ACTIVE, Instant.now());
    for (ReservationEntity reservation : expired) {
      releaseOne(reservation);
      reservation.expire();
    }
  }

  private ReservationEntity reserveVariant(
      UUID orderId, ReservationItemRequest item, Instant expiresAt) {
    ProductVariantEntity variant =
        variantRepository
            .findByIdForUpdate(item.targetId())
            .orElseThrow(() -> ApiException.notFound("Variant not found: " + item.targetId()));
    if (variant.getProduct().getStatus() != ProductStatus.PUBLISHED) {
      throw ApiException.conflict("Product is not published: " + variant.getProduct().getSku());
    }
    int available =
        batchRepository.availableQuantity(variant.getId(), StockBatchStatus.AVAILABLE)
            - variant.getReservedQuantity();
    if (item.quantity() > available) {
      throw ApiException.conflict(
          "Not enough stock for variant " + variant.getSku() + ": available " + available);
    }
    variant.reserve(item.quantity());
    return reservationRepository.save(
        new ReservationEntity(
            orderId, ReservationTargetType.VARIANT, variant.getId(), item.quantity(), expiresAt));
  }

  private ReservationEntity reservePlant(
      UUID orderId, ReservationItemRequest item, Instant expiresAt) {
    if (item.quantity() != 1) {
      throw ApiException.badRequest("Individual plant quantity must be 1");
    }
    IndividualPlantEntity plant =
        plantRepository
            .findByIdForUpdate(item.targetId())
            .orElseThrow(
                () -> ApiException.notFound("Individual plant not found: " + item.targetId()));
    if (plant.getStatus() != IndividualPlantStatus.AVAILABLE) {
      throw ApiException.conflict("Individual plant is not available: " + plant.getCode());
    }
    plant.transitionTo(IndividualPlantStatus.RESERVED);
    return reservationRepository.save(
        new ReservationEntity(
            orderId,
            ReservationTargetType.INDIVIDUAL_PLANT,
            plant.getId(),
            item.quantity(),
            expiresAt));
  }

  private void releaseOne(ReservationEntity reservation) {
    if (reservation.getTargetType() == ReservationTargetType.VARIANT) {
      variantRepository
          .findByIdForUpdate(reservation.getTargetId())
          .ifPresentOrElse(
              variant -> variant.release(reservation.getQuantity()),
              () ->
                  log.warn(
                      "Reservation {} references missing variant {}",
                      reservation.getId(),
                      reservation.getTargetId()));
      return;
    }
    plantRepository
        .findByIdForUpdate(reservation.getTargetId())
        .ifPresentOrElse(
            plant -> {
              if (plant.getStatus() == IndividualPlantStatus.RESERVED) {
                plant.transitionTo(IndividualPlantStatus.AVAILABLE);
              }
            },
            () ->
                log.warn(
                    "Reservation {} references missing plant {}",
                    reservation.getId(),
                    reservation.getTargetId()));
  }

  private void confirmOne(ReservationEntity reservation) {
    if (reservation.getTargetType() == ReservationTargetType.INDIVIDUAL_PLANT) {
      IndividualPlantEntity plant =
          plantRepository
              .findByIdForUpdate(reservation.getTargetId())
              .orElseThrow(() -> ApiException.notFound("Individual plant not found"));
      if (plant.getStatus() != IndividualPlantStatus.RESERVED) {
        throw ApiException.conflict(
            "Individual plant is not reserved: "
                + plant.getCode()
                + " ("
                + plant.getStatus()
                + ")");
      }
      plant.transitionTo(IndividualPlantStatus.SOLD);
      return;
    }

    ProductVariantEntity variant =
        variantRepository
            .findByIdForUpdate(reservation.getTargetId())
            .orElseThrow(() -> ApiException.notFound("Variant not found"));
    int remaining = reservation.getQuantity();
    List<StockBatchEntity> batches =
        batchRepository.findAvailableForUpdate(variant.getId(), StockBatchStatus.AVAILABLE);
    for (StockBatchEntity batch : batches) {
      if (remaining == 0) {
        break;
      }
      int take = Math.min(remaining, batch.getRemainingQuantity());
      batch.adjustRemaining(batch.getRemainingQuantity() - take);
      remaining -= take;
    }
    if (remaining > 0) {
      throw ApiException.conflict(
          "Reserved stock is no longer available for variant " + variant.getSku());
    }
    variant.release(reservation.getQuantity());
  }

  private ReservationResponse response(UUID orderId, List<ReservationEntity> reservations) {
    List<ReservationResponse.ReservationLine> lines =
        reservations.stream()
            .map(reservation ->
                new ReservationResponse.ReservationLine(
                    reservation.getId(),
                    reservation.getTargetType(),
                    reservation.getTargetId(),
                    reservation.getQuantity(),
                    reservation.getStatus()))
            .toList();
    Instant expiresAt = reservations.isEmpty() ? null : reservations.getFirst().getExpiresAt();
    return new ReservationResponse(orderId, lines, expiresAt);
  }
}
