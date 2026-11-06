package com.plantmarket.order.web;

import com.plantmarket.order.service.CareReminderService;
import com.plantmarket.order.web.dto.CareReminderRequest;
import com.plantmarket.order.web.dto.CareReminderResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CareReminderController {

  private final CareReminderService reminderService;

  public CareReminderController(CareReminderService reminderService) {
    this.reminderService = reminderService;
  }

  @PostMapping("/orders/{orderId}/care-reminders")
  @ResponseStatus(HttpStatus.CREATED)
  public List<CareReminderResponse> create(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID orderId,
      @Valid @RequestBody CareReminderRequest request) {
    return reminderService.create(customerId(jwt), orderId, request);
  }

  @GetMapping("/orders/{orderId}/care-reminders")
  public List<CareReminderResponse> listForOrder(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
    return reminderService.list(customerId(jwt), orderId);
  }

  @GetMapping("/care-reminders")
  public List<CareReminderResponse> list(@AuthenticationPrincipal Jwt jwt) {
    return reminderService.listForCustomer(customerId(jwt));
  }

  @DeleteMapping("/care-reminders/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    reminderService.deactivate(customerId(jwt), id);
  }

  private UUID customerId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
