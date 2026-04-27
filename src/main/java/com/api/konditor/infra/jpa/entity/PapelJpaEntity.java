package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code papeis}. */
@Entity
@Table(name = "papeis", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PapelJpaEntity {

  /** Identificador técnico do papel (ex: owner, admin, member). */
  @Id
  @Column(nullable = false, unique = true)
  private String nome;

  /** Nome exibido ao usuário (ex: Proprietário, Administrador, Membro). */
  @Column(name = "nome_exibido", nullable = false)
  private String nomeExibido;

  @Column(columnDefinition = "text")
  private String descricao;

  /** Nível hierárquico: owner=100, admin=50, member=10. Quanto maior, mais permissões possui. */
  @Column(name = "nivel_hierarquia", nullable = false)
  private Integer nivelHierarquia;

  // -------------------------------------------------------------------------
  // Gerenciamento do workspace
  // -------------------------------------------------------------------------

  /** Pode renomear ou excluir o workspace. */
  @Column(name = "pode_gerenciar_espaco_trabalho", nullable = false)
  private boolean podeGerenciarEspacoTrabalho;

  /** Pode convidar, remover e alterar o papel de membros. */
  @Column(name = "pode_gerenciar_membros", nullable = false)
  private boolean podeGerenciarMembros;

  /** Pode alterar o plano ou assinatura do workspace. */
  @Column(name = "pode_gerenciar_plano", nullable = false)
  private boolean podeGerenciarPlano;

  // -------------------------------------------------------------------------
  // Ingredientes
  // -------------------------------------------------------------------------

  @Column(name = "pode_criar_ingredientes", nullable = false)
  private boolean podeCriarIngredientes;

  @Column(name = "pode_editar_ingredientes", nullable = false)
  private boolean podeEditarIngredientes;

  @Column(name = "pode_excluir_ingredientes", nullable = false)
  private boolean podeExcluirIngredientes;

  // -------------------------------------------------------------------------
  // Produtos / Receitas
  // -------------------------------------------------------------------------

  @Column(name = "pode_criar_produtos", nullable = false)
  private boolean podeCriarProdutos;

  @Column(name = "pode_editar_produtos", nullable = false)
  private boolean podeEditarProdutos;

  @Column(name = "pode_excluir_produtos", nullable = false)
  private boolean podeExcluirProdutos;

  // -------------------------------------------------------------------------
  // Pedidos
  // -------------------------------------------------------------------------

  @Column(name = "pode_criar_pedidos", nullable = false)
  private boolean podeCriarPedidos;

  @Column(name = "pode_editar_pedidos", nullable = false)
  private boolean podeEditarPedidos;

  @Column(name = "pode_excluir_pedidos", nullable = false)
  private boolean podeExcluirPedidos;

  // -------------------------------------------------------------------------
  // Relatórios e custos
  // -------------------------------------------------------------------------

  /** Pode visualizar relatórios de vendas e produção. */
  @Column(name = "pode_ver_relatorios", nullable = false)
  private boolean podeVerRelatorios;

  /** Pode visualizar custos de ingredientes e margem dos produtos. */
  @Column(name = "pode_ver_custos", nullable = false)
  private boolean podeVerCustos;

  // -------------------------------------------------------------------------
  // Auditoria
  // -------------------------------------------------------------------------

  /** Pode visualizar o histórico de auditoria do workspace. */
  @Column(name = "pode_ver_log_auditoria", nullable = false)
  private boolean podeVerLogAuditoria;

  // -------------------------------------------------------------------------

  @Column(name = "ativo", nullable = false)
  private boolean ativo;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @PrePersist
  void prePersist() {
    this.criadoEm = Instant.now();
  }
}
