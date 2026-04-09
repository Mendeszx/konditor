package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code order_items}. */
@Entity
@Table(name = "order_items", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderJpaEntity order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductJpaEntity product;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;

  /**
   * Snapshot do preço de venda no momento da criação do pedido. Protege o histórico financeiro de
   * mudanças futuras no preço do produto.
   */
  @Column(name = "unit_price_at_time", nullable = false, precision = 19, scale = 2)
  private BigDecimal unitPriceAtTime;

  /** Personalização do item (ex: "escrita: Feliz Aniversário Maria"). */
  @Column(columnDefinition = "text")
  private String notes;

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
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
