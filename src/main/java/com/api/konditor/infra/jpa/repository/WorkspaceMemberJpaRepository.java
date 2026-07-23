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

  List<MembroEspacoTrabalhoJpaEntity> findAllByEspacoTrabalho_Id(UUID workspaceId);

  List<MembroEspacoTrabalhoJpaEntity> findAllByUsuario_Id(UUID userId);

  Optional<MembroEspacoTrabalhoJpaEntity> findByEspacoTrabalho_IdAndUsuario_Id(
      UUID workspaceId, UUID userId);

  @Query(
      "SELECT COUNT(m) > 0 FROM MembroEspacoTrabalhoJpaEntity m WHERE m.espacoTrabalho.id ="
          + " :workspaceId AND m.usuario.id = :userId AND m.papel.nome = :roleName")
  boolean existsByWorkspaceIdAndUserIdAndRoleName(UUID workspaceId, UUID userId, String roleName);

  List<MembroEspacoTrabalhoJpaEntity> findAllByEspacoTrabalho_IdAndExcluidoEmIsNull(
      UUID workspaceId);
}
