package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.ProductCategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link ProductCategoryJpaEntity}.
 * <p>
 * Categorias são globais — não filtradas por workspace.
 */
public interface ProductCategoryJpaRepository extends JpaRepository<ProductCategoryJpaEntity, UUID> {

    List<ProductCategoryJpaEntity> findAllByDeletedAtIsNullOrderByNameAsc();

    Optional<ProductCategoryJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
