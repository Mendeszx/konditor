package com.api.konditor.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa uma conversão entre unidades de medida. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitConversion {

  private UUID id;
  private Unit fromUnit;
  private Unit toUnit;
  private BigDecimal factor;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
