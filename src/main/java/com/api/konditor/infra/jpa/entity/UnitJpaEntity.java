package com.api.konditor.infra.jpa.entity;

import com.api.konditor.domain.enuns.UnitType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code unidades}. */
@Entity
@Table(name = "unidades", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "nome", nullable = false)
  private String name;

  @Column(name = "simbolo", nullable = false, unique = true)
  private String symbol;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false)
  private UnitType type;

  @Column(name = "base")
  private Boolean isBase;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "atualizado_em")
  private Instant updatedAt;

  @Column(name = "excluido_em")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "criado_por")
  private UserJpaEntity createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "atualizado_por")
  private UserJpaEntity updatedBy;

  @PrePersist
  void prePersist() {
    this.createdAt = Instant.now();
    if (this.isBase == null) {
      this.isBase = false;
    }
  }

  @PreUpdate
  void preUpdate() {
    this.updatedAt = Instant.now();
  }
}
