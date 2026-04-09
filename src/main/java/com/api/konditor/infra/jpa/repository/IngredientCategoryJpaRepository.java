package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.IngredientCategoryJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para {@link IngredientCategoryJpaEntity}.
 *
 * <p>Categorias são globais — não filtradas por workspace.
 */
public interface IngredientCategoryJpaRepository
    extends JpaRepository<IngredientCategoryJpaEntity, UUID> {

  List<IngredientCategoryJpaEntity> findAllByDeletedAtIsNull(Sort sort);

  Optional<IngredientCategoryJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
