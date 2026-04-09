package com.api.konditor.domain.entity;

import com.api.konditor.domain.enuns.UnitType;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa uma unidade de medida. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unit {

  private UUID id;
  private String name;
  private String symbol;
  private UnitType type;
  private Boolean isBase;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;
  private User createdBy;
  private User updatedBy;
}
