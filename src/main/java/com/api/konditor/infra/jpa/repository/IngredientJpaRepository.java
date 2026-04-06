package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.IngredientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link IngredientJpaEntity}.
 */
public interface IngredientJpaRepository extends JpaRepository<IngredientJpaEntity, UUID> {

    List<IngredientJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    Optional<IngredientJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID workspaceId, String name);
}
