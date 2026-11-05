package com.plantmarket.catalog.service;

import com.plantmarket.catalog.domain.CategoryEntity;
import com.plantmarket.catalog.domain.ProductEntity;
import com.plantmarket.catalog.domain.ProductStatus;
import com.plantmarket.catalog.repository.CategoryRepository;
import com.plantmarket.catalog.repository.ProductRepository;
import com.plantmarket.catalog.web.dto.ProductRequest;
import com.plantmarket.catalog.web.dto.ProductResponse;
import com.plantmarket.common.api.PageResponse;
import com.plantmarket.common.error.ApiException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "priceMinor", "createdAt");
  private static final int MAX_PAGE_SIZE = 100;

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  public ProductService(
      ProductRepository productRepository, CategoryRepository categoryRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
  }

  @Transactional(readOnly = true)
  public PageResponse<ProductResponse> list(
      int page, int size, String sort, UUID categoryId, String search, ProductStatus status) {
    validatePage(page, size);
    Page<ProductEntity> result =
        productRepository.findAll(
            filter(categoryId, search, status), PageRequest.of(page, size, parseSort(sort)));
    List<ProductResponse> items = result.getContent().stream().map(this::toResponse).toList();
    return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public ProductResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public ProductResponse create(ProductRequest request) {
    String sku = request.sku().trim();
    String slug = request.slug().trim();
    if (productRepository.existsBySku(sku)) {
      throw ApiException.conflict("Product SKU already exists");
    }
    if (productRepository.existsBySlug(slug)) {
      throw ApiException.conflict("Product slug already exists");
    }
    CategoryEntity category = findCategory(request.categoryId());
    ProductEntity product =
        new ProductEntity(
            sku,
            request.name().trim(),
            slug,
            normalizeDescription(request.description()),
            category,
            request.priceMinor(),
            request.currency(),
            request.status(),
            request.careLevel(),
            request.light(),
            request.petSafe());
    return toResponse(productRepository.save(product));
  }

  @Transactional
  public ProductResponse update(UUID id, ProductRequest request) {
    ProductEntity product = find(id);
    String sku = request.sku().trim();
    String slug = request.slug().trim();
    if (productRepository.existsBySkuAndIdNot(sku, id)) {
      throw ApiException.conflict("Product SKU already exists");
    }
    if (productRepository.existsBySlugAndIdNot(slug, id)) {
      throw ApiException.conflict("Product slug already exists");
    }
    CategoryEntity category = findCategory(request.categoryId());
    product.update(
        sku,
        request.name().trim(),
        slug,
        normalizeDescription(request.description()),
        category,
        request.priceMinor(),
        request.currency(),
        request.status(),
        request.careLevel(),
        request.light(),
        request.petSafe());
    return toResponse(product);
  }

  @Transactional
  public void archive(UUID id) {
    ProductEntity product = find(id);
    product.archive();
  }

  private ProductEntity find(UUID id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Product not found: " + id));
  }

  private CategoryEntity findCategory(UUID id) {
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> ApiException.badRequest("Category not found: " + id));
  }

  private Specification<ProductEntity> filter(
      UUID categoryId, String search, ProductStatus status) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (categoryId != null) {
        predicates.add(builder.equal(root.get("category").get("id"), categoryId));
      }
      if (status != null) {
        predicates.add(builder.equal(root.get("status"), status));
      }
      if (search != null && !search.isBlank()) {
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(
            builder.or(
                builder.like(builder.lower(root.get("name").as(String.class)), pattern),
                builder.like(builder.lower(root.get("slug").as(String.class)), pattern)));
      }
      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private Sort parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return Sort.by(Sort.Direction.DESC, "createdAt");
    }
    String[] parts = sort.split(",", 2);
    String field = parts[0].trim();
    if (!ALLOWED_SORT_FIELDS.contains(field)) {
      throw ApiException.badRequest("Unsupported sort field: " + field);
    }
    Sort.Direction direction = Sort.Direction.ASC;
    if (parts.length == 2) {
      if ("desc".equalsIgnoreCase(parts[1].trim())) {
        direction = Sort.Direction.DESC;
      } else if (!"asc".equalsIgnoreCase(parts[1].trim())) {
        throw ApiException.badRequest("Unsupported sort direction: " + parts[1].trim());
      }
    }
    return Sort.by(direction, field);
  }

  private void validatePage(int page, int size) {
    if (page < 0) {
      throw ApiException.badRequest("page must be >= 0");
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw ApiException.badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
    }
  }

  private static String normalizeDescription(String description) {
    return description == null || description.isBlank() ? null : description.trim();
  }

  private ProductResponse toResponse(ProductEntity product) {
    return new ProductResponse(
        product.getId(),
        product.getSku(),
        product.getName(),
        product.getSlug(),
        product.getDescription(),
        product.getCategory().getId(),
        product.getPriceMinor(),
        product.getCurrency(),
        product.getStatus(),
        product.getCareLevel(),
        product.getLight(),
        product.isPetSafe(),
        product.getCreatedAt(),
        product.getUpdatedAt());
  }
}
