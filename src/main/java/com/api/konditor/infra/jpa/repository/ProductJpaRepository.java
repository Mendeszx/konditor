package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.RecipeStatus;
import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório Spring Data JPA para {@link ProductJpaEntity}. */
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

  List<ProductJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

  Optional<ProductJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  Optional<ProductJpaEntity> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);

  boolean existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID workspaceId, String name);

  /**
   * Busca todos os produtos publicados e ativos do workspace com categoria e unidade de rendimento
   * carregados em uma única query (evita N+1). Apenas receitas com {@code status = publicada}
   * aparecem no dashboard.
   *
   * @param workspaceId ID do workspace (tenant)
   * @param status status desejado (normalmente {@code RecipeStatus.publicada})
   * @return lista de produtos com category e yieldUnit inicializados
   */
  @Query(
      """
      SELECT p FROM ProductJpaEntity p
      LEFT JOIN FETCH p.category
      LEFT JOIN FETCH p.yieldUnit
      WHERE p.workspace.id = :workspaceId
        AND p.deletedAt IS NULL
        AND p.isActive = true
        AND p.status = :status
      ORDER BY p.name ASC
      """)
  List<ProductJpaEntity> findAllActiveByWorkspaceIdWithDetails(
      @Param("workspaceId") UUID workspaceId, @Param("status") RecipeStatus status);
}
