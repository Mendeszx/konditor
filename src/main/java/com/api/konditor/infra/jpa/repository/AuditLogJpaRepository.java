package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.AuditOperation;
import com.api.konditor.infra.jpa.entity.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link AuditLogJpaEntity}.
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    Page<AuditLogJpaEntity> findAllByWorkspaceId(UUID workspaceId, Pageable pageable);

    List<AuditLogJpaEntity> findAllByEntityNameAndEntityId(String entityName, UUID entityId);

    List<AuditLogJpaEntity> findAllByWorkspaceIdAndOperation(UUID workspaceId, AuditOperation operation);

    List<AuditLogJpaEntity> findAllByPerformedById(UUID performedById);
}
