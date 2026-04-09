package com.api.konditor.infra.jpa.entity;

import com.api.konditor.domain.enuns.RecipeStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code products}. */
@Entity
@Table(
    name = "products",
    schema = "konditor",
    uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private ProductCategoryJpaEntity category;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "selling_price", nullable = false, precision = 19, scale = 2)
  private BigDecimal sellingPrice;

  /** Quantas unidades (ou kg, ml…) este produto rende pela receita. */
  @Column(name = "yield_quantity", nullable = false, precision = 19, scale = 4)
  private BigDecimal yieldQuantity;

  /** Unidade do rendimento (ex: 12 unidades, 1 kg). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "yield_unit_id")
  private UnitJpaEntity yieldUnit;

  /** Custo total calculado com base na receita (cache recalculado ao salvar ingredientes). */
  @Column(name = "calculated_cost", precision = 19, scale = 4)
  private BigDecimal calculatedCost;

  /** Tempo estimado de preparo em minutos. */
  @Column(name = "prep_time_minutes")
  private Integer prepTimeMinutes;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  /**
   * Status do ciclo de vida da receita. {@code rascunho} = em edição (não aparece no dashboard).
   * {@code publicada} = finalizada e visível em todas as listagens.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RecipeStatus status;

  /**
   * Preço de venda sugerido calculado pelo servidor com base no custo total e na margem padrão do
   * workspace. Serve de referência para o usuário.
   */
  @Column(name = "suggested_price", precision = 19, scale = 2)
  private BigDecimal suggestedPrice;

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
    this.isActive = true;
    if (this.status == null) {
      this.status = RecipeStatus.publicada;
    }
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
