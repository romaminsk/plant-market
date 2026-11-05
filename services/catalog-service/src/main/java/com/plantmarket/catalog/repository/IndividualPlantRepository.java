package com.plantmarket.catalog.repository;

import com.plantmarket.catalog.domain.IndividualPlantEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IndividualPlantRepository
    extends JpaRepository<IndividualPlantEntity, UUID>,
        JpaSpecificationExecutor<IndividualPlantEntity> {

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, UUID id);
}
