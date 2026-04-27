package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code ingredientes}. */
@Entity
@Table(
    name = "ingredientes",
    schema = "konditor",
    uniqueConstraints = @UniqueConstraint(columnNames = {"espaco_trabalho_id", "nome"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "espaco_trabalho_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  @Column(name = "nome", nullable = false)
  private String name;

  @Column(name = "codigo")
  private String code;

  @Column(name = "descricao", columnDefinition = "text")
  private String description;

  @Column(name = "marca")
  private String brand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "categoria_id")
  private IngredientCategoryJpaEntity category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unidade_id", nullable = false)
  private UnitJpaEntity unit;

  @Column(name = "custo_por_unidade", nullable = false, precision = 19, scale = 4)
  private BigDecimal costPerUnit;

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
    if (this.costPerUnit == null) {
      this.costPerUnit = BigDecimal.ZERO;
    }
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
