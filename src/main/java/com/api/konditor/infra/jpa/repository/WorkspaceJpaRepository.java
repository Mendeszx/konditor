package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link EspacoTrabalhoJpaEntity}. */
public interface WorkspaceJpaRepository extends JpaRepository<EspacoTrabalhoJpaEntity, UUID> {

  List<EspacoTrabalhoJpaEntity> findAllByProprietario_Id(UUID ownerId);

  Optional<EspacoTrabalhoJpaEntity> findByIdAndExcluidoEmIsNull(UUID id);

  List<EspacoTrabalhoJpaEntity> findAllByProprietario_IdAndExcluidoEmIsNull(UUID ownerId);
}
