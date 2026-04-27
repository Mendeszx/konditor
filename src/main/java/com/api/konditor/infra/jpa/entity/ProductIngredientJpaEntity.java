package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code ingredientes_produto}. */
@Entity
@Table(
    name = "ingredientes_produto",
    schema = "konditor",
    uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "ingrediente_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductIngredientJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProductJpaEntity product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingrediente_id", nullable = false)
  private IngredientJpaEntity ingredient;

  /** Quantidade do ingrediente usada nesta receita. */
  @Column(name = "quantidade", nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;

  /** Unidade da quantidade acima (pode diferir da unidade base do ingrediente). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unidade_id", nullable = false)
  private UnitJpaEntity unit;

  @Column(name = "notas", columnDefinition = "text")
  private String notes;

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
