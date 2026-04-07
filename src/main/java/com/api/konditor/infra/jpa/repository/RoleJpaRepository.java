package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para {@link RoleJpaEntity}.
 */
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, String> {

    Optional<RoleJpaEntity> findByName(String name);
}
