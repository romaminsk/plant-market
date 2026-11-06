package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.IndividualPlantEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IndividualPlantRepository
    extends JpaRepository<IndividualPlantEntity, UUID>,
        JpaSpecificationExecutor<IndividualPlantEntity> {

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select plant from IndividualPlantEntity plant where plant.id = :id")
  Optional<IndividualPlantEntity> findByIdForUpdate(@Param("id") UUID id);
}
