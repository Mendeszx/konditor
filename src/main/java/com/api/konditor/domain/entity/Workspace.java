package com.api.konditor.domain.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa um workspace (tenant). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {

  private UUID id;
  private String name;
  private User owner;
  private UUID planId;
  private String currency;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
