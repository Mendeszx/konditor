package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.ConversaoUnidadeJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link ConversaoUnidadeJpaEntity}. */
public interface UnitConversionJpaRepository
    extends JpaRepository<ConversaoUnidadeJpaEntity, UUID> {

  Optional<ConversaoUnidadeJpaEntity> findByFromUnitIdAndToUnitId(UUID fromUnitId, UUID toUnitId);

  List<ConversaoUnidadeJpaEntity> findAllByFromUnitId(UUID fromUnitId);

  List<ConversaoUnidadeJpaEntity> findAllByToUnitId(UUID toUnitId);

  boolean existsByFromUnitIdAndToUnitId(UUID fromUnitId, UUID toUnitId);
}
