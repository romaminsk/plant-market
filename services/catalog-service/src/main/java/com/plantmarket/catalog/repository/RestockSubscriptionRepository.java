package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.RestockSubscriptionEntity;
import com.plantmarket.catalog.domain.RestockSubscriptionStatus;
import com.plantmarket.catalog.domain.RestockTargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestockSubscriptionRepository
    extends JpaRepository<RestockSubscriptionEntity, UUID> {

  Optional<RestockSubscriptionEntity> findByTargetTypeAndTargetIdAndEmail(
      RestockTargetType targetType, UUID targetId, String email);

  List<RestockSubscriptionEntity> findByTargetTypeAndTargetIdAndStatus(
      RestockTargetType targetType, UUID targetId, RestockSubscriptionStatus status);
}
