package com.api.konditor.domain.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entidade de domínio que representa um usuário. Sem acoplamento com JPA ou qualquer framework de
 * persistência.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  private UUID id;
  private String email;
  private String name;
  private String googleId;
  private String locale;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
