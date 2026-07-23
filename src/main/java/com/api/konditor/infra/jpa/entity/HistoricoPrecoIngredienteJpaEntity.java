package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code historico_preco_ingrediente}. */
@Entity
@Table(name = "historico_preco_ingrediente", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoPrecoIngredienteJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingrediente_id", nullable = false)
  private IngredienteJpaEntity ingredient;

  @Column(name = "preco_antigo", nullable = false, precision = 19, scale = 4)
  private BigDecimal oldPrice;

  @Column(name = "preco_novo", nullable = false, precision = 19, scale = 4)
  private BigDecimal newPrice;

  @Column(name = "alterado_em", nullable = false, updatable = false)
  private Instant changedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "alterado_por")
  private UsuarioJpaEntity changedBy;

  @PrePersist
  void prePersist() {
    this.changedAt = Instant.now();
  }
}
