package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.MembroEspacoTrabalhoJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositório Spring Data JPA para {@link MembroEspacoTrabalhoJpaEntity}. */
public interface WorkspaceMemberJpaRepository
    extends JpaRepository<MembroEspacoTrabalhoJpaEntity, UUID> {

  List<MembroEspacoTrabalhoJpaEntity> findAllByWorkspaceId(UUID workspaceId);

  List<MembroEspacoTrabalhoJpaEntity> findAllByUser_Id(UUID userId);

  Optional<MembroEspacoTrabalhoJpaEntity> findByWorkspaceIdAndUser_Id(
      UUID workspaceId, UUID userId);

  @Query(
      "SELECT COUNT(m) > 0 FROM WorkspaceMemberJpaEntity m WHERE m.workspace.id = :workspaceId AND"
          + " m.user.id = :userId AND m.role.name = :roleName")
  boolean existsByWorkspaceIdAndUserIdAndRoleName(UUID workspaceId, UUID userId, String roleName);

  List<MembroEspacoTrabalhoJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
}
