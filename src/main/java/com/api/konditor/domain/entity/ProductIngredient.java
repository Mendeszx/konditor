package com.api.konditor.domain.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de domínio que representa um ingrediente de um produto (linha da receita).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductIngredient {

    private UUID id;
    private Product product;
    private Ingredient ingredient;
    private BigDecimal quantity;
    private Unit unit;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private User createdBy;
    private User updatedBy;
}
