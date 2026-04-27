package com.api.konditor.infra.jpa.entity;

import com.api.konditor.domain.enuns.SubscriptionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code assinaturas}. */
@Entity
@Table(name = "assinaturas", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssinaturaJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "espaco_trabalho_id", nullable = false)
  private EspacoTrabalhoJpaEntity workspace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plano_id", nullable = false)
  private DetalhesPlanoJpaEntity plan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SubscriptionStatus status;

  @Column(name = "iniciado_em")
  private Instant startedAt;

  @Column(name = "termina_em")
  private Instant endsAt;

  @Column(name = "trial_termina_em")
  private Instant trialEndsAt;

  /** ID da assinatura no gateway de pagamento externo (ex: Stripe, Pagar.me). */
  @Column(name = "id_assinatura_externo", unique = true)
  private String externalSubscriptionId;

  @Column(name = "ultimo_pagamento_em")
  private Instant lastPaymentAt;

  @Column(name = "proxima_cobranca_em")
  private Instant nextBillingAt;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "atualizado_em")
  private Instant updatedAt;

  @Column(name = "excluido_em")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "criado_por")
  private UsuarioJpaEntity createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "atualizado_por")
  private UsuarioJpaEntity updatedBy;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
    if (this.status == null) {
      this.status = SubscriptionStatus.active;
    }
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
