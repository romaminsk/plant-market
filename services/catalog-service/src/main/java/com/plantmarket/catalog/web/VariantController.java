package com.plantmarket.catalog.web;

import com.plantmarket.catalog.service.VariantService;
import com.plantmarket.catalog.web.dto.VariantRequest;
import com.plantmarket.catalog.web.dto.VariantResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VariantController {

  private final VariantService variantService;

  public VariantController(VariantService variantService) {
    this.variantService = variantService;
  }

  @GetMapping("/products/{productId}/variants")
  public List<VariantResponse> list(@PathVariable UUID productId) {
    return variantService.list(productId);
  }

  @PostMapping("/products/{productId}/variants")
  @ResponseStatus(HttpStatus.CREATED)
  public VariantResponse create(
      @PathVariable UUID productId, @Valid @RequestBody VariantRequest request) {
    return variantService.create(productId, request);
  }

  @GetMapping("/variants/{id}")
  public VariantResponse get(@PathVariable UUID id) {
    return variantService.get(id);
  }

  @PutMapping("/variants/{id}")
  public VariantResponse update(@PathVariable UUID id, @Valid @RequestBody VariantRequest request) {
    return variantService.update(id, request);
  }

  @DeleteMapping("/variants/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(@PathVariable UUID id) {
    variantService.archive(id);
  }
}
