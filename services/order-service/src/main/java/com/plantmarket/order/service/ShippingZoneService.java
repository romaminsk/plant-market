package com.plantmarket.order.service;

import com.plantmarket.common.error.ApiException;
import com.plantmarket.order.domain.ShippingZoneEntity;
import com.plantmarket.order.repository.ShippingZoneRepository;
import com.plantmarket.order.web.dto.ShippingZoneRequest;
import com.plantmarket.order.web.dto.ShippingZoneResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingZoneService {

  private final ShippingZoneRepository repository;

  public ShippingZoneService(ShippingZoneRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<ShippingZoneResponse> list() {
    return repository.findByActiveTrueOrderByCityAsc().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ShippingZoneResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public ShippingZoneResponse create(ShippingZoneRequest request) {
    validateDays(request);
    ShippingZoneEntity zone =
        new ShippingZoneEntity(
            request.name().trim(),
            request.city().trim(),
            request.priceMinor(),
            request.freeFromMinor(),
            request.minDays(),
            request.maxDays(),
            request.winterRequiresThermal(),
            request.thermalPackMinor());
    return toResponse(repository.save(zone));
  }

  @Transactional
  public ShippingZoneResponse update(UUID id, ShippingZoneRequest request) {
    validateDays(request);
    ShippingZoneEntity zone = find(id);
    zone.update(
        request.name().trim(),
        request.city().trim(),
        request.priceMinor(),
        request.freeFromMinor(),
        request.minDays(),
        request.maxDays(),
        request.winterRequiresThermal(),
        request.thermalPackMinor(),
        request.active() == null ? zone.isActive() : request.active());
    return toResponse(zone);
  }

  private ShippingZoneEntity find(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> ApiException.notFound("Shipping zone not found: " + id));
  }

  private void validateDays(ShippingZoneRequest request) {
    if (request.maxDays() < request.minDays()) {
      throw ApiException.badRequest("maxDays must be >= minDays");
    }
  }

  private ShippingZoneResponse toResponse(ShippingZoneEntity zone) {
    return new ShippingZoneResponse(
        zone.getId(),
        zone.getName(),
        zone.getCity(),
        zone.getPriceMinor(),
        zone.getFreeFromMinor(),
        zone.getMinDays(),
        zone.getMaxDays(),
        zone.isWinterRequiresThermal(),
        zone.getThermalPackMinor(),
        zone.isActive(),
        zone.getCreatedAt(),
        zone.getUpdatedAt());
  }
}
