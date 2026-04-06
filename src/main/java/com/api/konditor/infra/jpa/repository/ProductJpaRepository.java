package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link ProductJpaEntity}.
 */
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    List<ProductJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    Optional<ProductJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID workspaceId, String name);
}
