package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.UnitType;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link UnitJpaEntity}.
 */
public interface UnitJpaRepository extends JpaRepository<UnitJpaEntity, UUID> {

    Optional<UnitJpaEntity> findBySymbol(String symbol);

    List<UnitJpaEntity> findAllByType(UnitType type);

    List<UnitJpaEntity> findAllByIsBaseTrue();

    Optional<UnitJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
