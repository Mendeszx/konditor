package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code membros_espaco_trabalho}. */
@Entity
@Table(
    name = "membros_espaco_trabalho",
    schema = "konditor",
    uniqueConstraints = @UniqueConstraint(columnNames = {"espaco_trabalho_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMemberJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "espaco_trabalho_id", nullable = false)
  private WorkspaceJpaEntity espacoTrabalho;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", nullable = false)
  private UserJpaEntity usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "papel", referencedColumnName = "nome", nullable = false)
  private RoleJpaEntity papel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "convidado_por")
  private UserJpaEntity convidadoPor;

  @Column(name = "entrou_em")
  private Instant entrouEm;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em")
  private Instant atualizadoEm;

  @Column(name = "excluido_em")
  private Instant excluidoEm;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "criado_por")
  private UserJpaEntity criadoPor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "atualizado_por")
  private UserJpaEntity atualizadoPor;

  @PrePersist
  void prePersist() {
    this.criadoEm = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    this.atualizadoEm = Instant.now();
  }
}
