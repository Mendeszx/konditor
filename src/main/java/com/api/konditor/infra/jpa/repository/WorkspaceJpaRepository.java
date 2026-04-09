package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.WorkspaceJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link WorkspaceJpaEntity}. */
public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, UUID> {

  List<WorkspaceJpaEntity> findAllByOwnerId(UUID ownerId);

  Optional<WorkspaceJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<WorkspaceJpaEntity> findAllByOwnerIdAndDeletedAtIsNull(UUID ownerId);
}
