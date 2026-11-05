package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.ProductVariantEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

  List<ProductVariantEntity> findByProductIdOrderByCreatedAtAsc(UUID productId);

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, UUID id);

  boolean existsByProductId(UUID productId);
}
