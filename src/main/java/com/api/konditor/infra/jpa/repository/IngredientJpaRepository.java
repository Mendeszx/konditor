package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.IngredientJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link IngredientJpaEntity}.
 */
public interface IngredientJpaRepository extends JpaRepository<IngredientJpaEntity, UUID> {

    List<IngredientJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    Optional<IngredientJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<IngredientJpaEntity> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);

    boolean existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID workspaceId, String name);

    Optional<IngredientJpaEntity> findByWorkspaceIdAndCodeAndDeletedAtIsNull(UUID workspaceId, String code);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    /**
     * Conta ingredientes com estoque abaixo do mínimo configurado.
     */
    @Query("""
            SELECT COUNT(i) FROM IngredientJpaEntity i
            WHERE i.workspace.id = :workspaceId
              AND i.deletedAt IS NULL
              AND i.stockQuantity IS NOT NULL
              AND i.stockAlertMin IS NOT NULL
              AND i.stockQuantity < i.stockAlertMin
            """)
    long countEstoqueCritico(@Param("workspaceId") UUID workspaceId);

    /**
     * Listagem paginada de todos os ingredientes do workspace,
     * com unit e category carregados para evitar N+1.
     */
    @Query(value = """
            SELECT i FROM IngredientJpaEntity i
            LEFT JOIN FETCH i.unit
            LEFT JOIN FETCH i.category
            WHERE i.workspace.id = :workspaceId
              AND i.deletedAt IS NULL
            ORDER BY i.name ASC
            """,
            countQuery = """
            SELECT COUNT(i) FROM IngredientJpaEntity i
            WHERE i.workspace.id = :workspaceId
              AND i.deletedAt IS NULL
            """)
    Page<IngredientJpaEntity> findPageByWorkspaceId(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable);

    /**
     * Listagem paginada filtrada por categoria.
     */
    @Query(value = """
            SELECT i FROM IngredientJpaEntity i
            LEFT JOIN FETCH i.unit
            LEFT JOIN FETCH i.category
            WHERE i.workspace.id = :workspaceId
              AND i.category.id = :categoryId
              AND i.deletedAt IS NULL
            ORDER BY i.name ASC
            """,
            countQuery = """
            SELECT COUNT(i) FROM IngredientJpaEntity i
            WHERE i.workspace.id = :workspaceId
              AND i.category.id = :categoryId
              AND i.deletedAt IS NULL
            """)
    Page<IngredientJpaEntity> findPageByWorkspaceIdAndCategoryId(
            @Param("workspaceId") UUID workspaceId,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    /**
     * Busca ingredientes do workspace cujo nome contém {@code query} (case-insensitive),
     * com a unidade carregada via JOIN FETCH para evitar N+1.
     * Passando {@code query = ""} retorna todos os ingredientes.
     */
    @Query("""
            SELECT i FROM IngredientJpaEntity i
            LEFT JOIN FETCH i.unit
            WHERE i.workspace.id = :workspaceId
              AND i.deletedAt IS NULL
              AND LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY i.name ASC
            """)
    List<IngredientJpaEntity> searchByWorkspaceAndName(
            @Param("workspaceId") UUID workspaceId,
            @Param("query") String query
    );
}
