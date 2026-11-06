package com.plantmarket.order.repository;

import com.plantmarket.order.domain.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

  @Query(
      "select event from OutboxEventEntity event "
          + "where event.publishedAt is null and event.dead = false "
          + "and (event.nextAttemptAt is null or event.nextAttemptAt <= :now) "
          + "order by event.createdAt asc")
  List<OutboxEventEntity> findPending(@Param("now") Instant now, Pageable pageable);

  List<OutboxEventEntity> findByDeadTrueOrderByCreatedAtDesc();
}
