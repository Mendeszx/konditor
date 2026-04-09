package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Entidade JPA que mapeia a tabela {@code roles}.
 *
 * <p>Armazena os papéis disponíveis dentro de um workspace com suas permissões granulares e nível
 * hierárquico.
 */
@Entity
@Table(name = "roles", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleJpaEntity {

  /** Identificador técnico do papel (ex: owner, admin, member). */
  @Id
  @Column(nullable = false, unique = true)
  private String name;

  /** Nome exibido ao usuário (ex: Proprietário, Administrador, Membro). */
  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(columnDefinition = "text")
  private String description;

  /** Nível hierárquico: owner=100, admin=50, member=10. Quanto maior, mais permissões possui. */
  @Column(name = "hierarchy_level", nullable = false)
  private Integer hierarchyLevel;

  // -------------------------------------------------------------------------
  // Gerenciamento do workspace
  // -------------------------------------------------------------------------

  /** Pode renomear ou excluir o workspace. */
  @Column(name = "can_manage_workspace", nullable = false)
  private boolean canManageWorkspace;

  /** Pode convidar, remover e alterar o papel de membros. */
  @Column(name = "can_manage_members", nullable = false)
  private boolean canManageMembers;

  /** Pode alterar o plano ou assinatura do workspace. */
  @Column(name = "can_manage_plan", nullable = false)
  private boolean canManagePlan;

  // -------------------------------------------------------------------------
  // Ingredientes
  // -------------------------------------------------------------------------

  @Column(name = "can_create_ingredients", nullable = false)
  private boolean canCreateIngredients;

  @Column(name = "can_edit_ingredients", nullable = false)
  private boolean canEditIngredients;

  @Column(name = "can_delete_ingredients", nullable = false)
  private boolean canDeleteIngredients;

  // -------------------------------------------------------------------------
  // Produtos / Receitas
  // -------------------------------------------------------------------------

  @Column(name = "can_create_products", nullable = false)
  private boolean canCreateProducts;

  @Column(name = "can_edit_products", nullable = false)
  private boolean canEditProducts;

  @Column(name = "can_delete_products", nullable = false)
  private boolean canDeleteProducts;

  // -------------------------------------------------------------------------
  // Pedidos
  // -------------------------------------------------------------------------

  @Column(name = "can_create_orders", nullable = false)
  private boolean canCreateOrders;

  @Column(name = "can_edit_orders", nullable = false)
  private boolean canEditOrders;

  @Column(name = "can_delete_orders", nullable = false)
  private boolean canDeleteOrders;

  // -------------------------------------------------------------------------
  // Relatórios e custos
  // -------------------------------------------------------------------------

  /** Pode visualizar relatórios de vendas e produção. */
  @Column(name = "can_view_reports", nullable = false)
  private boolean canViewReports;

  /** Pode visualizar custos de ingredientes e margem dos produtos. */
  @Column(name = "can_view_costs", nullable = false)
  private boolean canViewCosts;

  // -------------------------------------------------------------------------
  // Auditoria
  // -------------------------------------------------------------------------

  /** Pode visualizar o histórico de auditoria do workspace. */
  @Column(name = "can_view_audit_log", nullable = false)
  private boolean canViewAuditLog;

  // -------------------------------------------------------------------------

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
  }
}
