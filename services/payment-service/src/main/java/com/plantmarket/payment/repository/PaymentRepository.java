package com.plantmarket.payment.repository;

import com.plantmarket.payment.domain.PaymentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

  Optional<PaymentEntity> findByOrderId(UUID orderId);

  Optional<PaymentEntity> findByProviderRef(String providerRef);
}
