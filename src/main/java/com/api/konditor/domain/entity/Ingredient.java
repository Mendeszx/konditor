package com.api.konditor.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa um ingrediente. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

  private UUID id;
  private Workspace workspace;
  private String name;
  private String brand;
  private Unit unit;
  private BigDecimal costPerUnit;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;
  private User createdBy;
  private User updatedBy;
}
