package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.DetalhesPlanoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link DetalhesPlanoJpaEntity}. */
public interface PlanDetailsJpaRepository extends JpaRepository<DetalhesPlanoJpaEntity, UUID> {

  Optional<DetalhesPlanoJpaEntity> findByName(String name);
}
