package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entidade JPA que mapeia a tabela {@code ingredient_categories}.
 *
 * <p>Categorias <strong>globais</strong> de ingrediente (ex: Chocolate, Laticínio, Farinha).
 * Compartilhadas entre todos os workspaces — não pertencem a nenhum workspace específico.
 */
@Entity
@Table(name = "ingredient_categories", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientCategoryJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column private String color;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private UserJpaEntity createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private UserJpaEntity updatedBy;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
