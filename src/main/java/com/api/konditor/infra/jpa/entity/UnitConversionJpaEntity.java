package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code conversoes_unidade}. */
@Entity
@Table(
    name = "conversoes_unidade",
    schema = "konditor",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"unidade_origem_id", "unidade_destino_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitConversionJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unidade_origem_id", nullable = false)
  private UnitJpaEntity fromUnit;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unidade_destino_id", nullable = false)
  private UnitJpaEntity toUnit;

  @Column(name = "fator", nullable = false, precision = 19, scale = 6)
  private BigDecimal factor;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "atualizado_em")
  private Instant updatedAt;

  @Column(name = "excluido_em")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "criado_por")
  private UserJpaEntity createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "atualizado_por")
  private UserJpaEntity updatedBy;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
