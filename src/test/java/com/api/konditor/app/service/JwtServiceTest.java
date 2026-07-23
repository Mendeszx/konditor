package com.api.konditor.app.service;

import static org.assertj.core.api.Assertions.*;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários de {@link JwtService}: fail-fast do segredo (KON-19), emissão e validação de
 * tokens com as claims multi-tenant.
 */
class JwtServiceTest {

  private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!";
  private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-com-32+-caracteres";
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String WORKSPACE_ID = UUID.randomUUID().toString();

  private JwtService sut;
  private UsuarioJpaEntity usuario;

  @BeforeEach
  void setUp() {
    sut = new JwtService(SEGREDO, 3600);
    usuario =
        UsuarioJpaEntity.builder().id(USER_ID).email("chef@konditor.io").nome("Chef Teste").build();
  }

  @Nested
  @DisplayName("Fail-fast do segredo (KON-19)")
  class FailFastSegredo {

    @Test
    @DisplayName("Segredo nulo impede a inicialização")
    void segredoNulo_lancaExcecao() {
      assertThatThrownBy(() -> new JwtService(null, 3600))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("Segredo em branco (JWT_SECRET não definida) impede a inicialização")
    void segredoEmBranco_lancaExcecao() {
      assertThatThrownBy(() -> new JwtService("", 3600))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("Segredo com menos de 32 caracteres impede a inicialização")
    void segredoCurto_lancaExcecao() {
      assertThatThrownBy(() -> new JwtService("curto-demais", 3600))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("32 caracteres");
    }
  }

  @Nested
  @DisplayName("Emissão e validação de tokens")
  class EmissaoEValidacao {

    @Test
    @DisplayName("Token completo carrega todas as claims do tenant")
    void gerarToken_roundTrip_preservaClaims() {
      String token =
          sut.gerarToken(
              new JwtService.ContextoToken(usuario, WORKSPACE_ID, Role.owner, Plan.free));

      Claims claims = sut.validarEExtrairClaims(token);

      assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
      assertThat(claims.get("email", String.class)).isEqualTo("chef@konditor.io");
      assertThat(claims.get("name", String.class)).isEqualTo("Chef Teste");
      assertThat(claims.get("workspaceId", String.class)).isEqualTo(WORKSPACE_ID);
      assertThat(claims.get("workspaceRole", String.class)).isEqualTo("owner");
      assertThat(claims.get("plan", String.class)).isEqualTo("free");
    }

    @Test
    @DisplayName("Token de onboarding não carrega contexto de workspace")
    void gerarTokenOnboarding_semClaimsDeWorkspace() {
      String token = sut.gerarTokenOnboarding(usuario);

      Claims claims = sut.validarEExtrairClaims(token);

      assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
      assertThat(claims.get("onboarding", Boolean.class)).isTrue();
      assertThat(claims.get("workspaceId")).isNull();
      assertThat(claims.get("workspaceRole")).isNull();
    }

    @Test
    @DisplayName("Token assinado com outro segredo é rejeitado")
    void tokenComOutraAssinatura_lancaJwtException() {
      String tokenForjado =
          new JwtService(OUTRO_SEGREDO, 3600)
              .gerarToken(
                  new JwtService.ContextoToken(usuario, WORKSPACE_ID, Role.owner, Plan.free));

      assertThatThrownBy(() -> sut.validarEExtrairClaims(tokenForjado))
          .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token expirado é rejeitado")
    void tokenExpirado_lancaJwtException() {
      String tokenExpirado =
          new JwtService(SEGREDO, -60)
              .gerarToken(
                  new JwtService.ContextoToken(usuario, WORKSPACE_ID, Role.owner, Plan.free));

      assertThatThrownBy(() -> sut.validarEExtrairClaims(tokenExpirado))
          .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token malformado é rejeitado")
    void tokenMalformado_lancaJwtException() {
      assertThatThrownBy(() -> sut.validarEExtrairClaims("nao-e-um-jwt"))
          .isInstanceOf(JwtException.class);
    }
  }
}
