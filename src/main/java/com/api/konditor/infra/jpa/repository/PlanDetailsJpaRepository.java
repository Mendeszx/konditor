package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.PlanDetailsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link PlanDetailsJpaEntity}.
 */
public interface PlanDetailsJpaRepository extends JpaRepository<PlanDetailsJpaEntity, UUID> {

    Optional<PlanDetailsJpaEntity> findByName(String name);
}
