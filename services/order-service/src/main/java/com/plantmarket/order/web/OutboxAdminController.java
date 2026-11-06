package com.plantmarket.order.web;

import com.plantmarket.order.service.OutboxAdminService;
import com.plantmarket.order.web.dto.OutboxEventResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/outbox")
public class OutboxAdminController {

  private final OutboxAdminService outboxAdminService;

  public OutboxAdminController(OutboxAdminService outboxAdminService) {
    this.outboxAdminService = outboxAdminService;
  }

  @GetMapping("/dead")
  public List<OutboxEventResponse> listDead() {
    return outboxAdminService.listDead();
  }

  @PostMapping("/{id}/redrive")
  public OutboxEventResponse redrive(@PathVariable UUID id) {
    return outboxAdminService.redrive(id);
  }
}
