package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link EspacoTrabalhoJpaEntity}. */
public interface WorkspaceJpaRepository extends JpaRepository<EspacoTrabalhoJpaEntity, UUID> {

  List<EspacoTrabalhoJpaEntity> findAllByOwnerId(UUID ownerId);

  Optional<EspacoTrabalhoJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<EspacoTrabalhoJpaEntity> findAllByOwnerIdAndDeletedAtIsNull(UUID ownerId);
}
