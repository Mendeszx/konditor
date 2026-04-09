package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.PlanDetailsJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link PlanDetailsJpaEntity}. */
public interface PlanDetailsJpaRepository extends JpaRepository<PlanDetailsJpaEntity, UUID> {

  Optional<PlanDetailsJpaEntity> findByName(String name);
}
