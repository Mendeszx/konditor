package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entidade JPA que mapeia a tabela {@code product_ingredients} (receita).
 *
 * <p>Cada linha representa um ingrediente e sua quantidade usada na receita do produto. O custo é
 * derivado de {@code ingredient.costPerUnit * quantity} — não armazenado aqui.
 */
@Entity
@Table(
    name = "product_ingredients",
    schema = "konditor",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "ingredient_id"}))
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
  @JoinColumn(name = "product_id", nullable = false)
  private ProductJpaEntity product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id", nullable = false)
  private IngredientJpaEntity ingredient;

  /** Quantidade do ingrediente usada nesta receita. */
  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;

  /** Unidade da quantidade acima (pode diferir da unidade base do ingrediente). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unit_id", nullable = false)
  private UnitJpaEntity unit;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private UserJpaEntity createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
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
