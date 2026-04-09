package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entidade JPA que mapeia a tabela {@code users}. Isolada na camada de infraestrutura — o domínio
 * nunca conhece esta classe.
 */
@Entity
@Table(name = "users", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column private String name;

  @Column(name = "google_id", unique = true)
  private String googleId;

  @Column private String locale;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
