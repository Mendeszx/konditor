package com.api.konditor.infra.jpa.entity;

import com.api.konditor.domain.enuns.AuditOperation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entidade JPA que mapeia a tabela {@code logs_auditoria}. */
@Entity
@Table(
    name = "logs_auditoria",
    schema = "konditor",
    indexes = {
      @Index(name = "idx_auditoria_entidade", columnList = "nome_entidade, id_entidade"),
      @Index(name = "idx_auditoria_espaco", columnList = "espaco_trabalho_id"),
      @Index(name = "idx_auditoria_realizado_em", columnList = "realizado_em")
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
  @JoinColumn(name = "espaco_trabalho_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  @Column(name = "nome_entidade", nullable = false)
  private String entityName;

  @Column(name = "id_entidade", nullable = false, columnDefinition = "uuid")
  private UUID entityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "operacao", nullable = false)
  private AuditOperation operation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dados_antes", columnDefinition = "jsonb")
  private String dataBefore;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dados_depois", columnDefinition = "jsonb")
  private String dataAfter;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "campos_alterados", columnDefinition = "jsonb")
  private String changedFields;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "realizado_por")
  private UserJpaEntity performedBy;

  @Column(name = "realizado_em", nullable = false, updatable = false)
  private Instant performedAt;

  @Column(name = "id_requisicao")
  private String requestId;

  @Column(name = "endereco_ip")
  private String ipAddress;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    this.createdAt = now;
    if (this.performedAt == null) {
      this.performedAt = now;
    }
  }
}
