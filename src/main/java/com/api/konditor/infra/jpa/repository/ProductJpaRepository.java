package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Busca todos os produtos ativos do workspace com categoria e unidade de rendimento
     * carregados em uma única query (evita N+1).
     *
     * @param workspaceId ID do workspace (tenant)
     * @return lista de produtos ativos com category e yieldUnit inicializados
     */
    @Query("""
            SELECT p FROM ProductJpaEntity p
            LEFT JOIN FETCH p.category
            LEFT JOIN FETCH p.yieldUnit
            WHERE p.workspace.id = :workspaceId
              AND p.deletedAt IS NULL
              AND p.isActive = true
            ORDER BY p.name ASC
            """)
    List<ProductJpaEntity> findAllActiveByWorkspaceIdWithDetails(@Param("workspaceId") UUID workspaceId);
}
