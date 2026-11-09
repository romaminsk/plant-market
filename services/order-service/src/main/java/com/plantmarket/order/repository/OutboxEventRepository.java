package com.plantmarket.order.repository;

import com.plantmarket.order.domain.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

  /** Locks a batch for one publisher; concurrent instances skip rows already claimed. */
  @Query(
      value =
          """
          select * from outbox_events
          where published_at is null and dead = false
            and (next_attempt_at is null or next_attempt_at <= :now)
          order by created_at asc
          limit :limit
          for update skip locked
          """,
      nativeQuery = true)
  List<OutboxEventEntity> findPendingForUpdate(
      @Param("now") Instant now, @Param("limit") int limit);

  List<OutboxEventEntity> findByDeadTrueOrderByCreatedAtDesc();
}
