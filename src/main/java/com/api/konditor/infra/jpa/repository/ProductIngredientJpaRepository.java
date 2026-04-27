package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.IngredienteProdutoJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório Spring Data JPA para {@link IngredienteProdutoJpaEntity}. */
public interface ProductIngredientJpaRepository
    extends JpaRepository<IngredienteProdutoJpaEntity, UUID> {

  List<IngredienteProdutoJpaEntity> findAllByProductIdAndDeletedAtIsNull(UUID productId);

  List<IngredienteProdutoJpaEntity> findAllByIngredientIdAndDeletedAtIsNull(UUID ingredientId);

  void deleteAllByProductId(UUID productId);

  /**
   * Busca todos os ingredientes de uma receita com ingrediente, unidade do ingrediente e unidade da
   * receita já carregados (evita N+1).
   */
  @Query(
      """
      SELECT pi FROM ProductIngredientJpaEntity pi
      LEFT JOIN FETCH pi.ingredient i
      LEFT JOIN FETCH i.unit
      LEFT JOIN FETCH pi.unit
      WHERE pi.product.id = :productId
        AND pi.deletedAt IS NULL
      """)
  List<IngredienteProdutoJpaEntity> findAllByProductIdWithDetails(
      @Param("productId") UUID productId);

  /**
   * Busca todos os vínculos produto-ingrediente que referenciam um determinado ingrediente, com
   * produto, ingrediente (com sua unidade base) e unidade da linha carregados.
   *
   * <p>Usado para recalcular os custos de todas as receitas afetadas quando o custo de um
   * ingrediente é atualizado.
   */
  @Query(
      """
      SELECT pi FROM ProductIngredientJpaEntity pi
      JOIN FETCH pi.product p
      JOIN FETCH pi.ingredient i
      JOIN FETCH i.unit
      JOIN FETCH pi.unit
      WHERE i.id = :ingredientId
        AND pi.deletedAt IS NULL
        AND p.deletedAt IS NULL
      """)
  List<IngredienteProdutoJpaEntity> findAllByIngredientIdWithDetails(
      @Param("ingredientId") UUID ingredientId);
}
