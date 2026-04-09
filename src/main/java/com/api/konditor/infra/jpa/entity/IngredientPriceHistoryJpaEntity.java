package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "ingredient_price_history", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientPriceHistoryJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id", nullable = false)
  private IngredientJpaEntity ingredient;

  @Column(name = "old_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal oldPrice;

  @Column(name = "new_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal newPrice;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by")
  private UserJpaEntity changedBy;

  @PrePersist
  void prePersist() {
    this.changedAt = Instant.now();
  }
}
