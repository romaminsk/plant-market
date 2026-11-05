package com.plantmarket.auth.repository;

import com.plantmarket.auth.domain.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {}
