package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.UnitConversionJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link UnitConversionJpaEntity}. */
public interface UnitConversionJpaRepository extends JpaRepository<UnitConversionJpaEntity, UUID> {

  Optional<UnitConversionJpaEntity> findByFromUnitIdAndToUnitId(UUID fromUnitId, UUID toUnitId);

  List<UnitConversionJpaEntity> findAllByFromUnitId(UUID fromUnitId);

  List<UnitConversionJpaEntity> findAllByToUnitId(UUID toUnitId);

  boolean existsByFromUnitIdAndToUnitId(UUID fromUnitId, UUID toUnitId);
}
