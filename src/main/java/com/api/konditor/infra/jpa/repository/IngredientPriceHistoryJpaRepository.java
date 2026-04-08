package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.IngredientPriceHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IngredientPriceHistoryJpaRepository extends JpaRepository<IngredientPriceHistoryJpaEntity, UUID> {

    /**
     * Retorna a entrada mais recente de histórico para cada ingrediente da lista,
     * usada para calcular a variação de preço por ingrediente sem causar N+1.
     */
    @Query("""
            SELECT ph FROM IngredientPriceHistoryJpaEntity ph
            WHERE ph.ingredient.id IN :ingredientIds
              AND ph.changedAt = (
                  SELECT MAX(ph2.changedAt)
                  FROM IngredientPriceHistoryJpaEntity ph2
                  WHERE ph2.ingredient.id = ph.ingredient.id
              )
            """)
    List<IngredientPriceHistoryJpaEntity> findMostRecentByIngredientIds(
            @Param("ingredientIds") List<UUID> ingredientIds);
}

