package com.api.konditor.infra.jpa.entity;

import com.api.konditor.domain.enuns.RecipeStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code produtos}. */
@Entity
@Table(
    name = "produtos",
    schema = "konditor",
    uniqueConstraints = @UniqueConstraint(columnNames = {"espaco_trabalho_id", "nome"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "espaco_trabalho_id", nullable = false)
  private EspacoTrabalhoJpaEntity workspace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "categoria_id")
  private CategoriaProdutoJpaEntity category;

  @Column(name = "nome", nullable = false)
  private String name;

  @Column(name = "descricao", columnDefinition = "text")
  private String description;

  // -------------------------------------------------------------------------
  // Preço / rendimento
  // -------------------------------------------------------------------------

  @Column(name = "preco_venda", nullable = false, precision = 19, scale = 4)
  private BigDecimal sellingPrice;

  @Column(name = "quantidade_rendimento", nullable = false, precision = 19, scale = 4)
  private BigDecimal yieldQuantity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unidade_rendimento_id")
  private UnidadeJpaEntity yieldUnit;

  @Column(name = "custo_calculado", precision = 19, scale = 4)
  private BigDecimal calculatedCost;

  @Column(name = "tempo_preparo_minutos")
  private Integer prepTimeMinutes;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private RecipeStatus status;

  @Column(name = "preco_sugerido", precision = 19, scale = 4)
  private BigDecimal suggestedPrice;

  @Column(name = "notas", columnDefinition = "text")
  private String notes;

  @Column(name = "ativo", nullable = false)
  private boolean isActive;

  // -------------------------------------------------------------------------
  // V2 — peso/volume por porção
  // -------------------------------------------------------------------------

  @Column(name = "peso_por_unidade", precision = 19, scale = 4)
  private BigDecimal unitWeight;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "unidade_peso_por_unidade_id")
  private UnidadeJpaEntity unitWeightUnit;

  // -------------------------------------------------------------------------
  // V3 — mão de obra, custos fixos, margem
  // -------------------------------------------------------------------------

  @Column(name = "custo_mao_de_obra_por_hora", precision = 19, scale = 2)
  private BigDecimal laborCostPerHour;

  @Column(name = "custo_mao_de_obra", precision = 19, scale = 4)
  private BigDecimal laborCost;

  @Column(name = "valor_custos_fixos", precision = 19, scale = 2)
  private BigDecimal fixedCostsValue;

  @Column(name = "tipo_custos_fixos", length = 20)
  private String fixedCostsType;

  @Column(name = "custos_fixos", precision = 19, scale = 4)
  private BigDecimal fixedCosts;

  @Column(name = "margem_desejada", precision = 5, scale = 2)
  private BigDecimal desiredMargin;

  // -------------------------------------------------------------------------
  // V4 — métricas calculadas
  // -------------------------------------------------------------------------

  @Column(name = "custo_ingredientes", precision = 19, scale = 4)
  private BigDecimal ingredientCost;

  @Column(name = "custo_unitario", precision = 19, scale = 4)
  private BigDecimal unitCost;

  @Column(name = "preco_sugerido_unitario", precision = 19, scale = 4)
  private BigDecimal suggestedUnitPrice;

  @Column(name = "quantidade_porcoes", precision = 19, scale = 4)
  private BigDecimal portionCount;

  @Column(name = "custo_por_grama_ml", precision = 19, scale = 6)
  private BigDecimal costPerGramMl;

  @Column(name = "preco_por_grama_ml", precision = 19, scale = 6)
  private BigDecimal pricePerGramMl;

  @Column(name = "custo_por_porcao", precision = 19, scale = 4)
  private BigDecimal costPerPortion;

  @Column(name = "preco_por_porcao", precision = 19, scale = 4)
  private BigDecimal pricePerPortion;

  // -------------------------------------------------------------------------
  // Auditoria
  // -------------------------------------------------------------------------

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
      this.status = RecipeStatus.publicada;
    }
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
