package com.plantmarket.catalog.service;

import com.plantmarket.catalog.domain.IndividualPlantEntity;
import com.plantmarket.catalog.domain.IndividualPlantStatus;
import com.plantmarket.catalog.domain.ProductVariantEntity;
import com.plantmarket.catalog.domain.RestockSubscriptionEntity;
import com.plantmarket.catalog.domain.RestockSubscriptionStatus;
import com.plantmarket.catalog.domain.RestockTargetType;
import com.plantmarket.catalog.domain.StockBatchStatus;
import com.plantmarket.catalog.notification.RestockNotifier;
import com.plantmarket.catalog.repository.IndividualPlantRepository;
import com.plantmarket.catalog.repository.ProductVariantRepository;
import com.plantmarket.catalog.repository.RestockSubscriptionRepository;
import com.plantmarket.catalog.repository.StockBatchRepository;
import com.plantmarket.catalog.web.dto.RestockSubscriptionRequest;
import com.plantmarket.catalog.web.dto.RestockSubscriptionResponse;
import com.plantmarket.common.error.ApiException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestockSubscriptionService {

  private final RestockSubscriptionRepository subscriptionRepository;
  private final ProductVariantRepository variantRepository;
  private final IndividualPlantRepository plantRepository;
  private final StockBatchRepository batchRepository;
  private final RestockNotifier notifier;

  public RestockSubscriptionService(
      RestockSubscriptionRepository subscriptionRepository,
      ProductVariantRepository variantRepository,
      IndividualPlantRepository plantRepository,
      StockBatchRepository batchRepository,
      RestockNotifier notifier) {
    this.subscriptionRepository = subscriptionRepository;
    this.variantRepository = variantRepository;
    this.plantRepository = plantRepository;
    this.batchRepository = batchRepository;
    this.notifier = notifier;
  }

  @Transactional
  public SubscriptionResult subscribe(RestockSubscriptionRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    requireSoldOut(request.targetType(), request.targetId());
    var existing =
        subscriptionRepository.findByTargetTypeAndTargetIdAndEmail(
            request.targetType(), request.targetId(), email);
    if (existing.isPresent()) {
      RestockSubscriptionEntity subscription = existing.get();
      if (subscription.getStatus() == RestockSubscriptionStatus.NOTIFIED) {
        subscription.resetToPending();
      }
      return new SubscriptionResult(toResponse(subscription), false);
    }
    RestockSubscriptionEntity saved =
        subscriptionRepository.save(
            new RestockSubscriptionEntity(request.targetType(), request.targetId(), email));
    return new SubscriptionResult(toResponse(saved), true);
  }

  public record SubscriptionResult(RestockSubscriptionResponse subscription, boolean created) {}

  @Transactional(readOnly = true)
  public RestockSubscriptionResponse get(UUID id) {
    return subscriptionRepository
        .findById(id)
        .map(RestockSubscriptionService::toResponse)
        .orElseThrow(() -> ApiException.notFound("Restock subscription not found: " + id));
  }

  @Transactional
  public void notifyVariantAvailable(UUID variantId) {
    ProductVariantEntity variant = variantRepository.findById(variantId).orElse(null);
    if (variant == null) {
      return;
    }
    notifySubscribers(
        RestockTargetType.VARIANT,
        variantId,
        "вариант " + variant.getSku() + " " + variant.getName());
  }

  @Transactional
  public void notifyPlantAvailable(UUID plantId) {
    IndividualPlantEntity plant = plantRepository.findById(plantId).orElse(null);
    if (plant == null) {
      return;
    }
    notifySubscribers(RestockTargetType.INDIVIDUAL_PLANT, plantId, "растение " + plant.getCode());
  }

  private void notifySubscribers(RestockTargetType targetType, UUID targetId, String label) {
    List<RestockSubscriptionEntity> pending =
        subscriptionRepository.findByTargetTypeAndTargetIdAndStatus(
            targetType, targetId, RestockSubscriptionStatus.PENDING);
    for (RestockSubscriptionEntity subscription : pending) {
      if (notifier.notifyAvailable(subscription.getEmail(), label)) {
        subscription.markNotified();
      }
    }
  }

  private void requireSoldOut(RestockTargetType targetType, UUID targetId) {
    if (targetType == RestockTargetType.VARIANT) {
      ProductVariantEntity variant =
          variantRepository
              .findById(targetId)
              .orElseThrow(() -> ApiException.notFound("Variant not found: " + targetId));
      int available =
          batchRepository.availableQuantity(targetId, StockBatchStatus.AVAILABLE)
              - variant.getReservedQuantity();
      if (available > 0) {
        throw ApiException.conflict("Variant is in stock: " + variant.getSku());
      }
      return;
    }
    IndividualPlantEntity plant =
        plantRepository
            .findById(targetId)
            .orElseThrow(() -> ApiException.notFound("Individual plant not found: " + targetId));
    if (plant.getStatus() == IndividualPlantStatus.AVAILABLE) {
      throw ApiException.conflict("Individual plant is in stock: " + plant.getCode());
    }
  }

  private static RestockSubscriptionResponse toResponse(RestockSubscriptionEntity subscription) {
    return new RestockSubscriptionResponse(
        subscription.getId(),
        subscription.getTargetType(),
        subscription.getTargetId(),
        subscription.getStatus(),
        subscription.getCreatedAt(),
        subscription.getNotifiedAt());
  }
}
