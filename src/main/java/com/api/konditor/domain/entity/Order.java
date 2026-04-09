package com.api.konditor.domain.entity;

import com.api.konditor.domain.enuns.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa um pedido. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

  private UUID id;
  private Workspace workspace;
  private String clientName;
  private String clientPhone;
  private OrderStatus status;
  private LocalDate deliveryDate;
  private LocalTime deliveryTime;
  private Boolean isDelivery;
  private String deliveryAddress;
  private BigDecimal totalPrice;
  private Integer discountCents;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;
  private User createdBy;
  private User updatedBy;
}
