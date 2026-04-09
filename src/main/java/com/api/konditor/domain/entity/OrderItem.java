package com.api.konditor.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa um item de pedido. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

  private UUID id;
  private Order order;
  private Product product;
  private BigDecimal quantity;
  private BigDecimal unitPriceAtTime;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;
  private User createdBy;
  private User updatedBy;
}
