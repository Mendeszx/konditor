package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.UnitType;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link UnitJpaEntity}. */
public interface UnitJpaRepository extends JpaRepository<UnitJpaEntity, UUID> {

  Optional<UnitJpaEntity> findBySymbol(String symbol);

  List<UnitJpaEntity> findAllByType(UnitType type);

  List<UnitJpaEntity> findAllByIsBaseTrue();

  Optional<UnitJpaEntity> findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType type);

  Optional<UnitJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<UnitJpaEntity> findAllByDeletedAtIsNullOrderByNameAsc();

  List<UnitJpaEntity> findAllByTypeAndDeletedAtIsNullOrderByNameAsc(UnitType type);
}
