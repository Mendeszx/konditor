package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code tokens_atualizacao}. */
@Entity
@Table(name = "tokens_atualizacao", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenAtualizacaoJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "token", nullable = false, unique = true, columnDefinition = "text")
  private String token;

  @Column(name = "usuario_id", nullable = false)
  private UUID userId;

  @Column(name = "expira_em", nullable = false)
  private Instant expiresAt;

  @Column(name = "revogado", nullable = false)
  private boolean revoked;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
    this.revoked = false;
  }
}
