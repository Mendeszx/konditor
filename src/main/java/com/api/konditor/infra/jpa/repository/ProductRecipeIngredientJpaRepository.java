package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.ProductRecipeIngredientJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório Spring Data JPA para {@link ProductRecipeIngredientJpaEntity}. */
public interface ProductRecipeIngredientJpaRepository
    extends JpaRepository<ProductRecipeIngredientJpaEntity, UUID> {

  void deleteAllByProductId(UUID productId);

  /**
   * Busca todas as receitas-como-ingrediente de um produto com a sub-receita e sua unidade de
   * rendimento já carregadas (evita N+1).
   */
  @Query(
      """
      SELECT ri FROM ProductRecipeIngredientJpaEntity ri
      LEFT JOIN FETCH ri.subReceita sr
      LEFT JOIN FETCH sr.yieldUnit
      WHERE ri.product.id = :productId
        AND ri.deletedAt IS NULL
      """)
  List<ProductRecipeIngredientJpaEntity> findAllByProductIdWithDetails(
      @Param("productId") UUID productId);
}
