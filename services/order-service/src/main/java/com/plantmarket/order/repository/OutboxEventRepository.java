package com.plantmarket.order.repository;

import com.plantmarket.order.domain.OutboxEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, java.util.UUID> {

  List<OutboxEventEntity> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
