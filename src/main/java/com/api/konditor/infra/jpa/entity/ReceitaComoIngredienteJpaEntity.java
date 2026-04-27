package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code receitas_como_ingrediente}. */
@Entity
@Table(
    name = "receitas_como_ingrediente",
    schema = "konditor",
    uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "receita_ingrediente_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceitaComoIngredienteJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoJpaEntity product;

  /** Sub-receita usada como ingrediente. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receita_ingrediente_id", nullable = false)
  private ProdutoJpaEntity subReceita;

  /** Quantidade de unidades da sub-receita utilizada (na unidade de rendimento dela). */
  @Column(name = "quantidade", nullable = false, precision = 19, scale = 4)
  private BigDecimal quantidade;

  @Column(name = "notas", columnDefinition = "text")
  private String notas;

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
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
