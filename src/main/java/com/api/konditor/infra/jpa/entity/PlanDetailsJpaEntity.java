package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entidade JPA que mapeia a tabela {@code plan_details}.
 *
 * <p>Armazena os dados completos de cada plano disponível na plataforma: preço, limites de uso e
 * funcionalidades inclusas.
 */
@Entity
@Table(name = "plan_details", schema = "konditor")
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
  @Column(nullable = false, unique = true)
  private String name;

  /** Nome exibido ao usuário (ex: Gratuito, Básico, Premium). */
  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(columnDefinition = "text")
  private String description;

  // -------------------------------------------------------------------------
  // Preço
  // -------------------------------------------------------------------------

  /** Valor em centavos (0 = gratuito). */
  @Column(name = "price_cents", nullable = false)
  private Integer priceCents;

  /** Ciclo de cobrança: monthly, yearly, once. */
  @Column(name = "billing_cycle", nullable = false)
  private String billingCycle;

  // -------------------------------------------------------------------------
  // Limites de uso
  // -------------------------------------------------------------------------

  @Column(name = "max_workspaces", nullable = false)
  private Integer maxWorkspaces;

  @Column(name = "max_members", nullable = false)
  private Integer maxMembers;

  @Column(name = "max_products")
  private Integer maxProducts;

  @Column(name = "max_ingredients")
  private Integer maxIngredients;

  /** Pedidos por mês; null = ilimitado. */
  @Column(name = "max_orders_per_month")
  private Integer maxOrdersPerMonth;

  // -------------------------------------------------------------------------
  // Funcionalidades inclusas
  // -------------------------------------------------------------------------

  @Column(name = "has_cost_calculation", nullable = false)
  private boolean hasCostCalculation;

  @Column(name = "has_order_management", nullable = false)
  private boolean hasOrderManagement;

  @Column(name = "has_reports", nullable = false)
  private boolean hasReports;

  @Column(name = "has_audit_log", nullable = false)
  private boolean hasAuditLog;

  @Column(name = "has_api_access", nullable = false)
  private boolean hasApiAccess;

  @Column(name = "has_priority_support", nullable = false)
  private boolean hasPrioritySupport;

  // -------------------------------------------------------------------------

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
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
