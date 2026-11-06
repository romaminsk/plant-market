package com.plantmarket.catalog.web;

import com.plantmarket.catalog.domain.IndividualPlantStatus;
import com.plantmarket.catalog.domain.LightLevel;
import com.plantmarket.catalog.service.IndividualPlantService;
import com.plantmarket.catalog.web.dto.IndividualPlantRequest;
import com.plantmarket.catalog.web.dto.IndividualPlantResponse;
import com.plantmarket.catalog.web.dto.PlantTransitionRequest;
import com.plantmarket.catalog.web.dto.UpdatePlantPhotosRequest;
import com.plantmarket.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class IndividualPlantController {

  private final IndividualPlantService plantService;

  public IndividualPlantController(IndividualPlantService plantService) {
    this.plantService = plantService;
  }

  @GetMapping("/individual-plants")
  public PageResponse<IndividualPlantResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort,
      @RequestParam(required = false) UUID productId,
      @RequestParam(required = false) IndividualPlantStatus status,
      @RequestParam(required = false) LightLevel light,
      @RequestParam(required = false) Boolean variegated,
      @RequestParam(required = false) Long maxPriceMinor,
      @RequestParam(required = false) String search) {
    return plantService.list(
        page, size, sort, productId, status, light, variegated, maxPriceMinor, search);
  }

  @GetMapping("/individual-plants/{id}")
  public IndividualPlantResponse get(@PathVariable UUID id) {
    return plantService.get(id);
  }

  @PostMapping("/products/{productId}/individual-plants")
  @ResponseStatus(HttpStatus.CREATED)
  public IndividualPlantResponse create(
      @PathVariable UUID productId, @Valid @RequestBody IndividualPlantRequest request) {
    return plantService.create(productId, request);
  }

  @PutMapping("/individual-plants/{id}")
  public IndividualPlantResponse update(
      @PathVariable UUID id, @Valid @RequestBody IndividualPlantRequest request) {
    return plantService.update(id, request);
  }

  @PutMapping("/individual-plants/{id}/photos")
  public IndividualPlantResponse updatePhotos(
      @PathVariable UUID id, @Valid @RequestBody UpdatePlantPhotosRequest request) {
    return plantService.updatePhotos(id, request);
  }

  @PostMapping("/individual-plants/{id}/transitions")
  public IndividualPlantResponse transition(
      @PathVariable UUID id, @Valid @RequestBody PlantTransitionRequest request) {
    return plantService.transition(id, request.status());
  }
}
