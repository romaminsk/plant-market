package com.plantmarket.order.web;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.order.domain.ClaimStatus;
import com.plantmarket.order.service.ClaimService;
import com.plantmarket.order.web.dto.ClaimDecisionRequest;
import com.plantmarket.order.web.dto.ClaimRequest;
import com.plantmarket.order.web.dto.ClaimResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ClaimController {

  private final ClaimService claimService;

  public ClaimController(ClaimService claimService) {
    this.claimService = claimService;
  }

  @PostMapping("/orders/{orderId}/claims")
  @ResponseStatus(HttpStatus.CREATED)
  public ClaimResponse open(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID orderId,
      @Valid @RequestBody ClaimRequest request) {
    return claimService.open(customerId(jwt), orderId, request);
  }

  @GetMapping("/orders/{orderId}/claims")
  public List<ClaimResponse> listForOrder(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
    return claimService.listForOrder(customerId(jwt), orderId);
  }

  @GetMapping("/claims")
  public List<ClaimResponse> list(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) ClaimStatus status) {
    requireStaff(jwt);
    return claimService.list(status);
  }

  @GetMapping("/claims/{id}")
  public ClaimResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    requireStaff(jwt);
    return claimService.get(id);
  }

  @PostMapping("/claims/{id}/decision")
  public ClaimResponse decide(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody ClaimDecisionRequest request) {
    requireStaff(jwt);
    return claimService.decide(id, request);
  }

  private UUID customerId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }

  private static void requireStaff(Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    if (roles == null
        || roles.stream().noneMatch(Set.of("ADMIN", "OPERATOR", "MANAGER")::contains)) {
      throw ApiException.forbidden("Staff role required");
    }
  }
}
