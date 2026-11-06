package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.ProductVariantEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

  List<ProductVariantEntity> findByProductIdOrderByCreatedAtAsc(UUID productId);

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, UUID id);

  boolean existsByProductId(UUID productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select variant from ProductVariantEntity variant where variant.id = :id")
  Optional<ProductVariantEntity> findByIdForUpdate(@Param("id") UUID id);
}
