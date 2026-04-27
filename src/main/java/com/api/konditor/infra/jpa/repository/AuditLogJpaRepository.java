package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.AuditOperation;
import com.api.konditor.infra.jpa.entity.LogAuditoriaJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link LogAuditoriaJpaEntity}. */
public interface AuditLogJpaRepository extends JpaRepository<LogAuditoriaJpaEntity, UUID> {

  Page<LogAuditoriaJpaEntity> findAllByWorkspaceId(UUID workspaceId, Pageable pageable);

  List<LogAuditoriaJpaEntity> findAllByEntityNameAndEntityId(String entityName, UUID entityId);

  List<LogAuditoriaJpaEntity> findAllByWorkspaceIdAndOperation(
      UUID workspaceId, AuditOperation operation);

  List<LogAuditoriaJpaEntity> findAllByPerformedById(UUID performedById);
}
