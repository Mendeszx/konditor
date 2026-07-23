package com.api.konditor.infra.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/** Entidade JPA que mapeia a tabela {@code usuarios}. */
@Entity
@Table(name = "usuarios", schema = "konditor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column private String nome;

  @Column(name = "id_google", unique = true)
  private String idGoogle;

  @Column private String idioma;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em")
  private Instant atualizadoEm;

  @Column(name = "excluido_em")
  private Instant excluidoEm;

  @Column(name = "criado_por")
  private UUID criadoPor;

  @Column(name = "atualizado_por")
  private UUID atualizadoPor;

  @PrePersist
  void prePersist() {
    this.criadoEm = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    this.atualizadoEm = Instant.now();
  }
}
