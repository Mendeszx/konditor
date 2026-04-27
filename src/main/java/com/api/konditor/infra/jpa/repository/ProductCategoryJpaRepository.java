package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.CategoriaProdutoJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data JPA para {@link CategoriaProdutoJpaEntity}.
 *
 * <p>Categorias são globais — não filtradas por workspace.
 */
public interface ProductCategoryJpaRepository
    extends JpaRepository<CategoriaProdutoJpaEntity, UUID> {

  List<CategoriaProdutoJpaEntity> findAllByDeletedAtIsNullOrderByNameAsc();

  Optional<CategoriaProdutoJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
