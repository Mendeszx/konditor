package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.UnitType;
import com.api.konditor.infra.jpa.entity.UnidadeJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link UnidadeJpaEntity}. */
public interface UnitJpaRepository extends JpaRepository<UnidadeJpaEntity, UUID> {

  Optional<UnidadeJpaEntity> findBySymbol(String symbol);

  List<UnidadeJpaEntity> findAllByType(UnitType type);

  List<UnidadeJpaEntity> findAllByIsBaseTrue();

  Optional<UnidadeJpaEntity> findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType type);

  Optional<UnidadeJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<UnidadeJpaEntity> findAllByDeletedAtIsNullOrderByNameAsc();

  List<UnidadeJpaEntity> findAllByTypeAndDeletedAtIsNullOrderByNameAsc(UnitType type);
}
