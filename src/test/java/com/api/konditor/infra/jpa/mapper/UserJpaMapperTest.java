package com.api.konditor.infra.jpa.mapper;

import static org.assertj.core.api.Assertions.*;

import com.api.konditor.domain.entity.User;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link UserJpaMapper} com a implementação real do MapStruct. Cobre os nomes divergentes
 * pt/en (ex.: {@code name}↔{@code nome}, {@code googleId}↔{@code idGoogle}) — regressão do KON-56.
 */
class UserJpaMapperTest {

  private final UserJpaMapper sut = new UserJpaMapperImpl();

  @Test
  @DisplayName("toJpa copia name→nome, googleId→idGoogle, locale→idioma e auditoria")
  void toJpa_copiaTodosOsCampos() {
    UUID id = UUID.randomUUID();
    Instant agora = Instant.now();

    User domain =
        User.builder()
            .id(id)
            .email("chef@konditor.io")
            .name("Chef Teste")
            .googleId("google-123")
            .locale("pt-BR")
            .createdAt(agora)
            .build();

    UsuarioJpaEntity jpa = sut.toJpa(domain);

    assertThat(jpa.getId()).isEqualTo(id);
    assertThat(jpa.getEmail()).isEqualTo("chef@konditor.io");
    assertThat(jpa.getNome()).isEqualTo("Chef Teste");
    assertThat(jpa.getIdGoogle()).isEqualTo("google-123");
    assertThat(jpa.getIdioma()).isEqualTo("pt-BR");
    assertThat(jpa.getCriadoEm()).isEqualTo(agora);
  }

  @Test
  @DisplayName("toDomain copia nome→name, idGoogle→googleId, idioma→locale")
  void toDomain_copiaTodosOsCampos() {
    UsuarioJpaEntity jpa =
        UsuarioJpaEntity.builder()
            .id(UUID.randomUUID())
            .email("ana@konditor.io")
            .nome("Ana")
            .idGoogle("google-999")
            .idioma("en-US")
            .build();

    User domain = sut.toDomain(jpa);

    assertThat(domain.getName()).isEqualTo("Ana");
    assertThat(domain.getGoogleId()).isEqualTo("google-999");
    assertThat(domain.getLocale()).isEqualTo("en-US");
    assertThat(domain.getEmail()).isEqualTo("ana@konditor.io");
  }
}
