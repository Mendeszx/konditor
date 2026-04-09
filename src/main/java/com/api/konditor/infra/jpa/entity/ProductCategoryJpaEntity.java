package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * Entidade JPA que mapeia a tabela {@code product_categories}.
 *
 * <p>Categorias <strong>globais</strong> de produto (ex: Bolo, Brigadeiro, Salgado). Compartilhadas
 * entre todos os workspaces — não pertencem a nenhum workspace específico.
 */
@Entity
@Table(name = "product_categories", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  /** Cor em hexadecimal para exibição na UI (ex: #FF5733). */
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
