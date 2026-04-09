package com.api.konditor.domain.entity;

import com.api.konditor.domain.enuns.AuditOperation;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa um registro de auditoria. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  private UUID id;
  private Workspace workspace;
  private String entityName;
  private UUID entityId;
  private AuditOperation operation;
  private String dataBefore;
  private String dataAfter;
  private String changedFields;
  private User performedBy;
  private Instant performedAt;
  private String requestId;
  private String ipAddress;
  private Instant createdAt;
}
