package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.ProductEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {

  boolean existsByCategoryId(UUID categoryId);

  boolean existsBySku(String sku);

  boolean existsBySlug(String slug);

  Optional<ProductEntity> findBySlug(String slug);

  boolean existsBySkuAndIdNot(String sku, UUID id);

  boolean existsBySlugAndIdNot(String slug, UUID id);
}
