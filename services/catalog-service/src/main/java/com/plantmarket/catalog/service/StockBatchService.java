package com.plantmarket.catalog.service;

import com.plantmarket.catalog.domain.ProductVariantEntity;
import com.plantmarket.catalog.domain.StockBatchEntity;
import com.plantmarket.catalog.domain.StockBatchStatus;
import com.plantmarket.catalog.repository.ProductVariantRepository;
import com.plantmarket.catalog.repository.StockBatchRepository;
import com.plantmarket.catalog.web.dto.StockBatchRequest;
import com.plantmarket.catalog.web.dto.StockBatchResponse;
import com.plantmarket.common.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockBatchService {

  private final StockBatchRepository batchRepository;
  private final ProductVariantRepository variantRepository;
  private final RestockSubscriptionService restockService;

  public StockBatchService(
      StockBatchRepository batchRepository,
      ProductVariantRepository variantRepository,
      RestockSubscriptionService restockService) {
    this.batchRepository = batchRepository;
    this.variantRepository = variantRepository;
    this.restockService = restockService;
  }

  @Transactional(readOnly = true)
  public List<StockBatchResponse> list(UUID variantId) {
    List<StockBatchEntity> batches =
        variantId == null
            ? batchRepository.findAll(Sort.by(Sort.Direction.DESC, "receivedAt"))
            : batchRepository.findByProductVariantIdOrderByReceivedAtDesc(variantId);
    return batches.stream().map(StockBatchService::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public StockBatchResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public StockBatchResponse create(StockBatchRequest request) {
    ProductVariantEntity variant =
        variantRepository
            .findById(request.productVariantId())
            .orElseThrow(
                () -> ApiException.badRequest("Variant not found: " + request.productVariantId()));
    Instant receivedAt = request.receivedAt() == null ? Instant.now() : request.receivedAt();
    if (!request.quarantineUntil().isAfter(receivedAt)) {
      throw ApiException.badRequest("quarantineUntil must be after receivedAt");
    }
    StockBatchEntity batch =
        new StockBatchEntity(
            variant,
            request.supplier().trim(),
            request.quantity(),
            request.unitCostMinor(),
            request.currency(),
            receivedAt,
            request.quarantineUntil());
    return toResponse(batchRepository.save(batch));
  }

  @Transactional
  public StockBatchResponse release(UUID id) {
    StockBatchEntity batch = find(id);
    if (batch.getStatus() != StockBatchStatus.QUARANTINE) {
      throw ApiException.conflict("Only quarantine batches can be released");
    }
    batch.release();
    restockService.notifyVariantAvailable(batch.getProductVariant().getId());
    return toResponse(batch);
  }

  @Transactional
  public StockBatchResponse adjust(UUID id, int remainingQuantity) {
    StockBatchEntity batch = find(id);
    if (remainingQuantity > batch.getQuantity()) {
      throw ApiException.badRequest(
          "remainingQuantity cannot exceed batch quantity " + batch.getQuantity());
    }
    boolean increased = remainingQuantity > batch.getRemainingQuantity();
    batch.adjustRemaining(remainingQuantity);
    if (increased) {
      restockService.notifyVariantAvailable(batch.getProductVariant().getId());
    }
    return toResponse(batch);
  }

  private StockBatchEntity find(UUID id) {
    return batchRepository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Stock batch not found: " + id));
  }

  private static StockBatchResponse toResponse(StockBatchEntity batch) {
    return new StockBatchResponse(
        batch.getId(),
        batch.getProductVariant().getId(),
        batch.getSupplier(),
        batch.getQuantity(),
        batch.getRemainingQuantity(),
        batch.getUnitCostMinor(),
        batch.getCurrency(),
        batch.getReceivedAt(),
        batch.getQuarantineUntil(),
        batch.getStatus(),
        batch.getCreatedAt(),
        batch.getUpdatedAt());
  }
}
