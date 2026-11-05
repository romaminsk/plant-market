package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.StockBatchEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockBatchRepository extends JpaRepository<StockBatchEntity, UUID> {

  List<StockBatchEntity> findByProductVariantIdOrderByReceivedAtDesc(UUID variantId);
}
