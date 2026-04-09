package com.api.konditor.infra.jpa.entity;

import com.api.konditor.domain.enuns.AuditOperation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entidade JPA que mapeia a tabela {@code audit_logs}. */
@Entity
@Table(
    name = "audit_logs",
    schema = "konditor",
    indexes = {
      @Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
      @Index(name = "idx_audit_workspace", columnList = "workspace_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  @Column(name = "entity_name", nullable = false)
  private String entityName;

  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuditOperation operation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data_before", columnDefinition = "jsonb")
  private String dataBefore;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data_after", columnDefinition = "jsonb")
  private String dataAfter;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "changed_fields", columnDefinition = "jsonb")
  private String changedFields;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "performed_by")
  private UserJpaEntity performedBy;

  @Column(name = "performed_at")
  private Instant performedAt;

  @Column(name = "request_id")
  private String requestId;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
    if (this.performedAt == null) {
      this.performedAt = Instant.now();
    }
  }
}
