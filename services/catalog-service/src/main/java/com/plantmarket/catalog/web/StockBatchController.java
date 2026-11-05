package com.plantmarket.catalog.web;

import com.plantmarket.catalog.service.StockBatchService;
import com.plantmarket.catalog.web.dto.AdjustStockRequest;
import com.plantmarket.catalog.web.dto.StockBatchRequest;
import com.plantmarket.catalog.web.dto.StockBatchResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-batches")
public class StockBatchController {

  private final StockBatchService batchService;

  public StockBatchController(StockBatchService batchService) {
    this.batchService = batchService;
  }

  @GetMapping
  public List<StockBatchResponse> list(@RequestParam(required = false) UUID variantId) {
    return batchService.list(variantId);
  }

  @GetMapping("/{id}")
  public StockBatchResponse get(@PathVariable UUID id) {
    return batchService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StockBatchResponse create(@Valid @RequestBody StockBatchRequest request) {
    return batchService.create(request);
  }

  @PostMapping("/{id}/release")
  public StockBatchResponse release(@PathVariable UUID id) {
    return batchService.release(id);
  }

  @PostMapping("/{id}/adjust")
  public StockBatchResponse adjust(
      @PathVariable UUID id, @Valid @RequestBody AdjustStockRequest request) {
    return batchService.adjust(id, request.remainingQuantity());
  }
}
