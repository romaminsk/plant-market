package com.plantmarket.catalog.service;

import com.plantmarket.catalog.domain.IndividualPlantEntity;
import com.plantmarket.catalog.domain.IndividualPlantStatus;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.domain.ProductEntity;
import com.plantmarket.catalog.repository.IndividualPlantRepository;
import com.plantmarket.catalog.repository.ProductRepository;
import com.plantmarket.catalog.web.dto.IndividualPlantRequest;
import com.plantmarket.catalog.web.dto.IndividualPlantResponse;
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
public class IndividualPlantService {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("code", "priceMinor", "createdAt");
  private static final int MAX_PAGE_SIZE = 100;

  private final IndividualPlantRepository plantRepository;
  private final ProductRepository productRepository;
  private final RestockSubscriptionService restockService;

  public IndividualPlantService(
      IndividualPlantRepository plantRepository,
      ProductRepository productRepository,
      RestockSubscriptionService restockService) {
    this.plantRepository = plantRepository;
    this.productRepository = productRepository;
    this.restockService = restockService;
  }

  @Transactional(readOnly = true)
  public PageResponse<IndividualPlantResponse> list(
      int page,
      int size,
      String sort,
      UUID productId,
      IndividualPlantStatus status,
      LightLevel light,
      Boolean variegated,
      Long maxPriceMinor,
      String search) {
    validatePage(page, size);
    Page<IndividualPlantEntity> result =
        plantRepository.findAll(
            filter(productId, status, light, variegated, maxPriceMinor, search),
            PageRequest.of(page, size, parseSort(sort)));
    List<IndividualPlantResponse> items =
        result.getContent().stream().map(this::toResponse).toList();
    return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
  }

  @Transactional(readOnly = true)
  public IndividualPlantResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public IndividualPlantResponse create(UUID productId, IndividualPlantRequest request) {
    ProductEntity product = findProduct(productId);
    String code = request.code().trim();
    if (plantRepository.existsByCode(code)) {
      throw ApiException.conflict("Individual plant code already exists");
    }
    IndividualPlantEntity plant =
        new IndividualPlantEntity(
            product,
            code,
            normalize(request.description()),
            normalize(request.photoUrl()),
            request.priceMinor(),
            request.currency(),
            request.careLevel(),
            request.light(),
            request.petSafe());
    return toResponse(plantRepository.save(plant));
  }

  @Transactional
  public IndividualPlantResponse update(UUID id, IndividualPlantRequest request) {
    IndividualPlantEntity plant = find(id);
    String code = request.code().trim();
    if (plantRepository.existsByCodeAndIdNot(code, id)) {
      throw ApiException.conflict("Individual plant code already exists");
    }
    plant.update(
        code,
        normalize(request.description()),
        normalize(request.photoUrl()),
        request.priceMinor(),
        request.currency(),
        request.careLevel(),
        request.light(),
        request.petSafe());
    return toResponse(plant);
  }

  @Transactional
  public IndividualPlantResponse transition(UUID id, IndividualPlantStatus target) {
    IndividualPlantEntity plant = find(id);
    if (!plant.canTransitionTo(target)) {
      throw ApiException.conflict("Illegal transition from " + plant.getStatus() + " to " + target);
    }
    plant.transitionTo(target);
    if (target == IndividualPlantStatus.AVAILABLE) {
      restockService.notifyPlantAvailable(id);
    }
    return toResponse(plant);
  }

  private IndividualPlantEntity find(UUID id) {
    return plantRepository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Individual plant not found: " + id));
  }

  private ProductEntity findProduct(UUID id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Product not found: " + id));
  }

  private Specification<IndividualPlantEntity> filter(
      UUID productId,
      IndividualPlantStatus status,
      LightLevel light,
      Boolean variegated,
      Long maxPriceMinor,
      String search) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (productId != null) {
        predicates.add(builder.equal(root.get("product").get("id"), productId));
      }
      if (status != null) {
        predicates.add(builder.equal(root.get("status"), status));
      }
      if (light != null) {
        predicates.add(builder.equal(root.get("light"), light));
      }
      if (variegated != null) {
        predicates.add(builder.equal(root.get("product").get("variegated"), variegated));
      }
      if (maxPriceMinor != null) {
        predicates.add(builder.lessThanOrEqualTo(root.get("priceMinor"), maxPriceMinor));
      }
      if (search != null && !search.isBlank()) {
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(
            builder.or(
                builder.like(builder.lower(root.get("code").as(String.class)), pattern),
                builder.like(builder.lower(root.get("description").as(String.class)), pattern),
                builder.like(
                    builder.lower(root.get("product").get("name").as(String.class)), pattern)));
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

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private IndividualPlantResponse toResponse(IndividualPlantEntity plant) {
    return new IndividualPlantResponse(
        plant.getId(),
        plant.getProduct().getId(),
        plant.getProduct().getName(),
        plant.getProduct().isVariegated(),
        plant.getCode(),
        plant.getDescription(),
        plant.getPhotoUrl(),
        plant.getPriceMinor(),
        plant.getCurrency(),
        plant.getStatus(),
        plant.getCareLevel(),
        plant.getLight(),
        plant.isPetSafe(),
        plant.getCreatedAt(),
        plant.getUpdatedAt());
  }
}
