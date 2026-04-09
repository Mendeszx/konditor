package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.WorkspaceMemberJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositório Spring Data JPA para {@link WorkspaceMemberJpaEntity}. */
public interface WorkspaceMemberJpaRepository
    extends JpaRepository<WorkspaceMemberJpaEntity, UUID> {

  List<WorkspaceMemberJpaEntity> findAllByWorkspaceId(UUID workspaceId);

  List<WorkspaceMemberJpaEntity> findAllByUser_Id(UUID userId);

  Optional<WorkspaceMemberJpaEntity> findByWorkspaceIdAndUser_Id(UUID workspaceId, UUID userId);

  @Query(
      "SELECT COUNT(m) > 0 FROM WorkspaceMemberJpaEntity m WHERE m.workspace.id = :workspaceId AND"
          + " m.user.id = :userId AND m.role.name = :roleName")
  boolean existsByWorkspaceIdAndUserIdAndRoleName(UUID workspaceId, UUID userId, String roleName);

  List<WorkspaceMemberJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
}
