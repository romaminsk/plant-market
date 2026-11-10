package com.plantmarket.catalog.service;

import com.plantmarket.catalog.domain.CategoryEntity;
import com.plantmarket.catalog.repository.CategoryRepository;
import com.plantmarket.catalog.repository.ProductRepository;
import com.plantmarket.catalog.web.dto.CategoryRequest;
import com.plantmarket.catalog.web.dto.CategoryResponse;
import com.plantmarket.common.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  public CategoryService(
      CategoryRepository categoryRepository, ProductRepository productRepository) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> list() {
    return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
        .map(CategoryService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public CategoryResponse get(String idOrSlug) {
    return toResponse(find(idOrSlug));
  }

  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    String slug = request.slug().trim();
    if (categoryRepository.existsBySlug(slug)) {
      throw ApiException.conflict("Category slug already exists");
    }
    CategoryEntity category =
        new CategoryEntity(
            request.name().trim(), slug, normalizeDescription(request.description()));
    return toResponse(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse update(UUID id, CategoryRequest request) {
    CategoryEntity category = find(id);
    String slug = request.slug().trim();
    if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
      throw ApiException.conflict("Category slug already exists");
    }
    category.update(request.name().trim(), slug, normalizeDescription(request.description()));
    return toResponse(category);
  }

  @Transactional
  public void delete(UUID id) {
    CategoryEntity category = find(id);
    if (productRepository.existsByCategoryId(id)) {
      throw ApiException.conflict("Category contains products");
    }
    categoryRepository.delete(category);
  }

  private CategoryEntity find(UUID id) {
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Category not found: " + id));
  }

  private CategoryEntity find(String idOrSlug) {
    UUID id = parseUuid(idOrSlug);
    if (id != null) {
      return find(id);
    }
    return categoryRepository
        .findBySlug(idOrSlug)
        .orElseThrow(() -> ApiException.notFound("Category not found: " + idOrSlug));
  }

  private static UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private static String normalizeDescription(String description) {
    return description == null || description.isBlank() ? null : description.trim();
  }

  private static CategoryResponse toResponse(CategoryEntity category) {
    return new CategoryResponse(
        category.getId(),
        category.getName(),
        category.getSlug(),
        category.getDescription(),
        category.getCreatedAt(),
        category.getUpdatedAt());
  }
}
