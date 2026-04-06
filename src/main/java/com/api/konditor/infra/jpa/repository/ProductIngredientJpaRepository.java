package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.ProductIngredientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link ProductIngredientJpaEntity}.
 */
public interface ProductIngredientJpaRepository extends JpaRepository<ProductIngredientJpaEntity, UUID> {

    List<ProductIngredientJpaEntity> findAllByProductIdAndDeletedAtIsNull(UUID productId);

    List<ProductIngredientJpaEntity> findAllByIngredientIdAndDeletedAtIsNull(UUID ingredientId);

    void deleteAllByProductId(UUID productId);
}
