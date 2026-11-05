package com.plantmarket.catalog.service;

import com.plantmarket.catalog.domain.ProductEntity;
import com.plantmarket.catalog.domain.ProductVariantEntity;
import com.plantmarket.catalog.repository.ProductRepository;
import com.plantmarket.catalog.repository.ProductVariantRepository;
import com.plantmarket.catalog.web.dto.VariantRequest;
import com.plantmarket.catalog.web.dto.VariantResponse;
import com.plantmarket.common.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VariantService {

  private final ProductVariantRepository variantRepository;
  private final ProductRepository productRepository;

  public VariantService(
      ProductVariantRepository variantRepository, ProductRepository productRepository) {
    this.variantRepository = variantRepository;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public List<VariantResponse> list(UUID productId) {
    findProduct(productId);
    return variantRepository.findByProductIdOrderByCreatedAtAsc(productId).stream()
        .map(VariantService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public VariantResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public VariantResponse create(UUID productId, VariantRequest request) {
    ProductEntity product = findProduct(productId);
    String sku = request.sku().trim();
    if (variantRepository.existsBySku(sku)) {
      throw ApiException.conflict("Variant SKU already exists");
    }
    ProductVariantEntity variant =
        new ProductVariantEntity(
            product,
            sku,
            request.name().trim(),
            normalize(request.size()),
            normalize(request.potSize()),
            normalize(request.form()),
            request.priceMinor(),
            request.currency(),
            request.status());
    return toResponse(variantRepository.save(variant));
  }

  @Transactional
  public VariantResponse update(UUID id, VariantRequest request) {
    ProductVariantEntity variant = find(id);
    String sku = request.sku().trim();
    if (variantRepository.existsBySkuAndIdNot(sku, id)) {
      throw ApiException.conflict("Variant SKU already exists");
    }
    variant.update(
        sku,
        request.name().trim(),
        normalize(request.size()),
        normalize(request.potSize()),
        normalize(request.form()),
        request.priceMinor(),
        request.currency(),
        request.status());
    return toResponse(variant);
  }

  @Transactional
  public void archive(UUID id) {
    find(id).archive();
  }

  private ProductEntity findProduct(UUID id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Product not found: " + id));
  }

  private ProductVariantEntity find(UUID id) {
    return variantRepository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Variant not found: " + id));
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static VariantResponse toResponse(ProductVariantEntity variant) {
    return new VariantResponse(
        variant.getId(),
        variant.getProduct().getId(),
        variant.getSku(),
        variant.getName(),
        variant.getSize(),
        variant.getPotSize(),
        variant.getForm(),
        variant.getPriceMinor(),
        variant.getCurrency(),
        variant.getStatus(),
        variant.getCreatedAt(),
        variant.getUpdatedAt());
  }
}
