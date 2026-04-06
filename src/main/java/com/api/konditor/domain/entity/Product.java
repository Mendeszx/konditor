package com.api.konditor.domain.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de domínio que representa um produto (receita).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    private UUID id;
    private Workspace workspace;
    private UUID categoryId;
    private String name;
    private String description;
    private BigDecimal sellingPrice;
    private BigDecimal yieldQuantity;
    private UUID yieldUnitId;
    private BigDecimal calculatedCost;
    private Integer prepTimeMinutes;
    private String notes;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private User createdBy;
    private User updatedBy;
}
