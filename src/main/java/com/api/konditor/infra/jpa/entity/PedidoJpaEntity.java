package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code pedidos}. */
@Entity
@Table(name = "pedidos", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "espaco_trabalho_id", nullable = false)
  private EspacoTrabalhoJpaEntity workspace;

  @Column(name = "nome_cliente")
  private String clientName;

  @Column(name = "telefone_cliente")
  private String clientPhone;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "data_entrega")
  private LocalDate deliveryDate;

  @Column(name = "hora_entrega")
  private LocalTime deliveryTime;

  @Column(name = "eh_entrega", nullable = false)
  private boolean isDelivery;

  @Column(name = "endereco_entrega", columnDefinition = "text")
  private String deliveryAddress;

  @Column(name = "preco_total", precision = 19, scale = 4)
  private BigDecimal totalPrice;

  @Column(name = "desconto_centavos", nullable = false)
  private Integer discountCents;

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
  private UsuarioJpaEntity createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "atualizado_por")
  private UsuarioJpaEntity updatedBy;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
    if (this.discountCents == null) {
      this.discountCents = 0;
    }
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
