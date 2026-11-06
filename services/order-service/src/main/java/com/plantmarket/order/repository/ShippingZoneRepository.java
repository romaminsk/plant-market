package com.plantmarket.order.repository;

import com.plantmarket.order.domain.ShippingZoneEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingZoneRepository extends JpaRepository<ShippingZoneEntity, UUID> {

  List<ShippingZoneEntity> findByActiveTrueOrderByCityAsc();
}
