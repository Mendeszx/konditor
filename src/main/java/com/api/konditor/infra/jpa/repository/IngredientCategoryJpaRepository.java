package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.CategoriaIngredienteJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para {@link CategoriaIngredienteJpaEntity}.
 *
 * <p>Categorias são globais — não filtradas por workspace.
 */
public interface IngredientCategoryJpaRepository
    extends JpaRepository<CategoriaIngredienteJpaEntity, UUID> {

  List<CategoriaIngredienteJpaEntity> findAllByDeletedAtIsNull(Sort sort);

  Optional<CategoriaIngredienteJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
