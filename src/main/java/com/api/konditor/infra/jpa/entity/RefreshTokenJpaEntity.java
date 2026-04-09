package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code refresh_tokens}. Isolada na camada de infraestrutura. */
@Entity
@Table(
    name = "refresh_tokens",
    schema = "konditor",
    indexes = {
      @Index(name = "idx_refresh_token_value", columnList = "token"),
      @Index(name = "idx_refresh_token_user", columnList = "user_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
  }
}
