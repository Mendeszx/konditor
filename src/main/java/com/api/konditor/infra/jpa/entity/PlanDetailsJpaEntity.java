package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entidade JPA que mapeia a tabela {@code detalhes_plano}.
 *
 * <p>Armazena os dados completos de cada plano disponível na plataforma: preço, limites de uso e
 * funcionalidades inclusas.
 */
@Entity
@Table(name = "detalhes_plano", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDetailsJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  /** Identificador técnico do plano (ex: free, basic, premium). */
  @Column(name = "nome", nullable = false, unique = true)
  private String name;

  /** Nome exibido ao usuário (ex: Gratuito, Básico, Premium). */
  @Column(name = "nome_exibido", nullable = false)
  private String displayName;

  @Column(name = "descricao", columnDefinition = "text")
  private String description;

  // -------------------------------------------------------------------------
  // Preço
  // -------------------------------------------------------------------------

  /** Valor em centavos (0 = gratuito). */
  @Column(name = "preco_centavos", nullable = false)
  private Integer priceCents;

  /** Ciclo de cobrança: monthly, yearly, once. */
  @Column(name = "ciclo_cobranca", nullable = false)
  private String billingCycle;

  // -------------------------------------------------------------------------
  // Limites de uso
  // -------------------------------------------------------------------------

  @Column(name = "max_espacos_trabalho")
  private Integer maxWorkspaces;

  @Column(name = "max_membros")
  private Integer maxMembers;

  @Column(name = "max_produtos")
  private Integer maxProducts;

  @Column(name = "max_ingredientes")
  private Integer maxIngredients;

  /** Pedidos por mês; null = ilimitado. */
  @Column(name = "max_pedidos_por_mes")
  private Integer maxOrdersPerMonth;

  // -------------------------------------------------------------------------
  // Funcionalidades inclusas
  // -------------------------------------------------------------------------

  @Column(name = "tem_calculo_custos", nullable = false)
  private boolean hasCostCalculation;

  @Column(name = "tem_gerenciamento_pedidos", nullable = false)
  private boolean hasOrderManagement;

  @Column(name = "tem_relatorios", nullable = false)
  private boolean hasReports;

  @Column(name = "tem_log_auditoria", nullable = false)
  private boolean hasAuditLog;

  @Column(name = "tem_acesso_api", nullable = false)
  private boolean hasApiAccess;

  @Column(name = "tem_suporte_prioritario", nullable = false)
  private boolean hasPrioritySupport;

  // -------------------------------------------------------------------------

  @Column(name = "ativo", nullable = false)
  private boolean isActive;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "atualizado_em")
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
    if (this.billingCycle == null) {
      this.billingCycle = "monthly";
    }
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
