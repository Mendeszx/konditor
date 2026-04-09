package com.api.konditor.domain.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade de domínio que representa um refresh token. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

  private UUID id;
  private String token;
  private UUID userId;
  private Instant expiresAt;
  private boolean revoked;
  private Instant createdAt;
}
