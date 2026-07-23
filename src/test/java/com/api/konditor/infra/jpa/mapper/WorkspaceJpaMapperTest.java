package com.api.konditor.infra.jpa.mapper;

import static org.assertj.core.api.Assertions.*;

import com.api.konditor.domain.entity.User;
import com.api.konditor.domain.entity.Workspace;
import com.api.konditor.infra.jpa.entity.DetalhesPlanoJpaEntity;
import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Testes do {@link WorkspaceJpaMapper} usando a implementação real gerada pelo MapStruct (sem
 * mocks). Garante que os campos domínio↔JPA (nomes divergentes pt/en) são efetivamente copiados —
 * regressão do KON-56, em que {@code name} não chegava a {@code nome} e o onboarding gravava
 * workspace com nome null.
 */
class WorkspaceJpaMapperTest {

  // Instancia a impl gerada e injeta manualmente o mapper aninhado (componentModel = SPRING não
  // é autowired fora de um contexto Spring).
  private final WorkspaceJpaMapper sut = criarMapper();

  private static WorkspaceJpaMapper criarMapper() {
    WorkspaceJpaMapperImpl impl = new WorkspaceJpaMapperImpl();
    ReflectionTestUtils.setField(impl, "userJpaMapper", new UserJpaMapperImpl());
    return impl;
  }

  @Test
  @DisplayName("toJpa copia name→nome, currency→moeda, owner→proprietario e auditoria")
  void toJpa_copiaTodosOsCampos() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID createdBy = UUID.randomUUID();
    Instant agora = Instant.now();

    Workspace domain =
        Workspace.builder()
            .id(id)
            .name("Mi amor")
            .currency("BRL")
            .owner(User.builder().id(ownerId).name("Chef").build())
            .createdAt(agora)
            .createdBy(createdBy)
            .build();

    EspacoTrabalhoJpaEntity jpa = sut.toJpa(domain);

    assertThat(jpa.getId()).isEqualTo(id);
    assertThat(jpa.getNome()).isEqualTo("Mi amor");
    assertThat(jpa.getMoeda()).isEqualTo("BRL");
    assertThat(jpa.getProprietario()).isNotNull();
    assertThat(jpa.getProprietario().getId()).isEqualTo(ownerId);
    assertThat(jpa.getProprietario().getNome()).isEqualTo("Chef");
    assertThat(jpa.getCriadoEm()).isEqualTo(agora);
    assertThat(jpa.getCriadoPor()).isEqualTo(createdBy);
    // plano é resolvido pelo use case após o mapeamento
    assertThat(jpa.getPlano()).isNull();
  }

  @Test
  @DisplayName("toDomain copia nome→name, moeda→currency e plano.id→planId")
  void toDomain_copiaTodosOsCampos() {
    UUID id = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    EspacoTrabalhoJpaEntity jpa =
        EspacoTrabalhoJpaEntity.builder()
            .id(id)
            .nome("Doceria da Chef")
            .moeda("USD")
            .proprietario(UsuarioJpaEntity.builder().id(UUID.randomUUID()).nome("Ana").build())
            .plano(DetalhesPlanoJpaEntity.builder().id(planId).name("free").build())
            .build();

    Workspace domain = sut.toDomain(jpa);

    assertThat(domain.getId()).isEqualTo(id);
    assertThat(domain.getName()).isEqualTo("Doceria da Chef");
    assertThat(domain.getCurrency()).isEqualTo("USD");
    assertThat(domain.getPlanId()).isEqualTo(planId);
    assertThat(domain.getOwner()).isNotNull();
    assertThat(domain.getOwner().getName()).isEqualTo("Ana");
  }

  @Test
  @DisplayName("Round-trip domínio → JPA → domínio preserva os campos escalares")
  void roundTrip_preservaCampos() {
    Workspace original =
        Workspace.builder().id(UUID.randomUUID()).name("Atelier X").currency("EUR").build();

    Workspace resultado = sut.toDomain(sut.toJpa(original));

    assertThat(resultado.getName()).isEqualTo("Atelier X");
    assertThat(resultado.getCurrency()).isEqualTo("EUR");
    assertThat(resultado.getId()).isEqualTo(original.getId());
  }
}
