package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code espacos_trabalho}. */
@Entity
@Table(name = "espacos_trabalho", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspacoTrabalhoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false)
  private String nome;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "proprietario_id", nullable = false)
  private UsuarioJpaEntity proprietario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plano_id", nullable = false)
  private DetalhesPlanoJpaEntity plano;

  @Column(nullable = false)
  private String moeda;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em")
  private Instant atualizadoEm;

  @Column(name = "excluido_em")
  private Instant excluidoEm;

  @Column(name = "criado_por")
  private UUID criadoPor;

  @Column(name = "atualizado_por")
  private UUID atualizadoPor;

  @PrePersist
  void prePersist() {
    this.criadoEm = Instant.now();
    if (this.moeda == null) {
      this.moeda = "BRL";
    }
  }

  @PreUpdate
  void preUpdate() {
    this.atualizadoEm = Instant.now();
  }
}
