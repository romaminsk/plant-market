package com.plantmarket.catalog.web;

import com.plantmarket.catalog.service.RestockSubscriptionService;
import com.plantmarket.catalog.web.dto.RestockSubscriptionRequest;
import com.plantmarket.catalog.web.dto.RestockSubscriptionResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restock-subscriptions")
public class RestockSubscriptionController {

  private final RestockSubscriptionService subscriptionService;

  public RestockSubscriptionController(RestockSubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @PostMapping
  public ResponseEntity<RestockSubscriptionResponse> subscribe(
      @Valid @RequestBody RestockSubscriptionRequest request) {
    var result = subscriptionService.subscribe(request);
    return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(result.subscription());
  }

  @GetMapping("/{id}")
  public RestockSubscriptionResponse get(@PathVariable UUID id) {
    return subscriptionService.get(id);
  }
}
