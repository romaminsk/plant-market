package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.StockBatchEntity;
import com.plantmarket.catalog.domain.StockBatchStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBatchRepository extends JpaRepository<StockBatchEntity, UUID> {

  List<StockBatchEntity> findByProductVariantIdOrderByReceivedAtDesc(UUID variantId);

  @Query(
      "select coalesce(sum(batch.remainingQuantity), 0) from StockBatchEntity batch "
          + "where batch.productVariant.id = :variantId and batch.status = :status")
  int availableQuantity(
      @Param("variantId") UUID variantId, @Param("status") StockBatchStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select batch from StockBatchEntity batch where batch.productVariant.id = :variantId "
          + "and batch.status = :status order by batch.receivedAt asc")
  List<StockBatchEntity> findAvailableForUpdate(
      @Param("variantId") UUID variantId, @Param("status") StockBatchStatus status);
}
