package com.api.konditor.domain.usecase.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.api.konditor.app.controller.response.GoogleAuthResponse;
import com.api.konditor.app.controller.response.RenovarTokenResponse;
import com.api.konditor.app.exception.AuthException;
import com.api.konditor.app.service.JwtService;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.googleprovider.GoogleIdentityProvider;
import com.api.konditor.infra.googleprovider.response.GoogleUserResponse;
import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import com.api.konditor.infra.jpa.entity.MembroEspacoTrabalhoJpaEntity;
import com.api.konditor.infra.jpa.entity.PapelJpaEntity;
import com.api.konditor.infra.jpa.entity.TokenAtualizacaoJpaEntity;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import com.api.konditor.infra.jpa.repository.RefreshTokenJpaRepository;
import com.api.konditor.infra.jpa.repository.SubscriptionJpaRepository;
import com.api.konditor.infra.jpa.repository.UserJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceMemberJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Testes unitários de {@link AuthUseCaseImpl}: login via Google (usuário novo → onboarding;
 * existente → workspace), rotação/reuso de refresh token e logout.
 */
@ExtendWith(MockitoExtension.class)
class AuthUseCaseImplTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID WORKSPACE_ID = UUID.randomUUID();
  private static final String GOOGLE_ID = "google-id-123";

  @Mock private GoogleIdentityProvider googleIdentityProvider;
  @Mock private UserJpaRepository userRepository;
  @Mock private WorkspaceJpaRepository workspaceRepository;
  @Mock private WorkspaceMemberJpaRepository workspaceMemberRepository;
  @Mock private SubscriptionJpaRepository subscriptionRepository;
  @Mock private RefreshTokenJpaRepository refreshTokenRepository;
  @Mock private JwtService jwtService;

  @InjectMocks private AuthUseCaseImpl sut;

  private MockHttpServletResponse httpResponse;
  private UsuarioJpaEntity usuario;
  private EspacoTrabalhoJpaEntity workspace;
  private MembroEspacoTrabalhoJpaEntity membro;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(sut, "refreshExpiracaoEmSegundos", 3600L);
    ReflectionTestUtils.setField(sut, "cookieSeguro", false);

    httpResponse = new MockHttpServletResponse();
    usuario =
        UsuarioJpaEntity.builder()
            .id(USER_ID)
            .idGoogle(GOOGLE_ID)
            .email("chef@konditor.io")
            .nome("Chef Teste")
            .build();
    workspace = EspacoTrabalhoJpaEntity.builder().id(WORKSPACE_ID).nome("Doceria").build();
    membro =
        MembroEspacoTrabalhoJpaEntity.builder()
            .id(UUID.randomUUID())
            .papel(PapelJpaEntity.builder().nome("owner").build())
            .build();
  }

  private void mockSalvarRefreshToken() {
    when(refreshTokenRepository.save(any(TokenAtualizacaoJpaEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  private TokenAtualizacaoJpaEntity refreshToken(boolean revogado, Instant expiraEm) {
    return TokenAtualizacaoJpaEntity.builder()
        .id(UUID.randomUUID())
        .token("refresh-antigo")
        .userId(USER_ID)
        .revoked(revogado)
        .expiresAt(expiraEm)
        .build();
  }

  @Nested
  @DisplayName("loginComGoogle")
  class LoginComGoogle {

    @Test
    @DisplayName("Usuário novo sem workspace recebe token de onboarding e workspace null")
    void usuarioNovo_recebeTokenDeOnboarding() {
      when(googleIdentityProvider.verificar("id-token"))
          .thenReturn(new GoogleUserResponse(GOOGLE_ID, "chef@konditor.io", "Chef Teste"));
      when(userRepository.findByIdGoogle(GOOGLE_ID)).thenReturn(Optional.empty());
      when(userRepository.save(any(UsuarioJpaEntity.class))).thenReturn(usuario);
      when(workspaceRepository.findAllByProprietario_IdAndExcluidoEmIsNull(USER_ID))
          .thenReturn(List.of());
      when(jwtService.gerarTokenOnboarding(usuario)).thenReturn("token-onboarding");
      mockSalvarRefreshToken();

      GoogleAuthResponse response = sut.loginComGoogle("id-token", httpResponse);

      assertThat(response.getAccessToken()).isEqualTo("token-onboarding");
      assertThat(response.getWorkspace()).isNull();
      assertThat(response.getUsuario().getEmail()).isEqualTo("chef@konditor.io");
      verify(jwtService, never()).gerarToken(any());

      String cookie = httpResponse.getHeader("Set-Cookie");
      assertThat(cookie)
          .contains("refresh_token=")
          .contains("HttpOnly")
          .contains("Path=/auth/refresh")
          .contains("SameSite=Strict");
    }

    @Test
    @DisplayName("Usuário existente com workspace recebe access token com contexto do tenant")
    void usuarioComWorkspace_recebeTokenCompleto() {
      when(googleIdentityProvider.verificar("id-token"))
          .thenReturn(new GoogleUserResponse(GOOGLE_ID, "novo@konditor.io", "Nome Novo"));
      when(userRepository.findByIdGoogle(GOOGLE_ID)).thenReturn(Optional.of(usuario));
      when(workspaceRepository.findAllByProprietario_IdAndExcluidoEmIsNull(USER_ID))
          .thenReturn(List.of(workspace));
      when(workspaceMemberRepository.findByEspacoTrabalho_IdAndUsuario_Id(WORKSPACE_ID, USER_ID))
          .thenReturn(Optional.of(membro));
      when(subscriptionRepository.findByWorkspaceIdAndStatus(eq(WORKSPACE_ID), any()))
          .thenReturn(Optional.empty());
      when(jwtService.gerarToken(any(JwtService.ContextoToken.class))).thenReturn("access-token");
      mockSalvarRefreshToken();

      GoogleAuthResponse response = sut.loginComGoogle("id-token", httpResponse);

      assertThat(response.getAccessToken()).isEqualTo("access-token");
      assertThat(response.getWorkspace()).isNotNull();
      assertThat(response.getWorkspace().getRole()).isEqualTo(Role.owner);
      assertThat(response.getWorkspace().getPlano()).isEqualTo(Plan.free);
      // Dados do Google atualizam o cadastro (dirty checking)
      assertThat(usuario.getEmail()).isEqualTo("novo@konditor.io");
      assertThat(usuario.getNome()).isEqualTo("Nome Novo");

      verify(jwtService)
          .gerarToken(
              argThat(
                  ctx ->
                      ctx.workspaceId().equals(WORKSPACE_ID.toString())
                          && ctx.workspaceRole() == Role.owner));
    }
  }

  @Nested
  @DisplayName("renovarToken")
  class RenovarToken {

    @Test
    @DisplayName("Token válido é rotacionado: antigo revogado, novo emitido")
    void tokenValido_rotaciona() {
      TokenAtualizacaoJpaEntity antigo = refreshToken(false, Instant.now().plusSeconds(1000));
      when(refreshTokenRepository.findByToken("refresh-antigo")).thenReturn(Optional.of(antigo));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuario));
      when(workspaceRepository.findAllByProprietario_IdAndExcluidoEmIsNull(USER_ID))
          .thenReturn(List.of(workspace));
      when(workspaceMemberRepository.findByEspacoTrabalho_IdAndUsuario_Id(WORKSPACE_ID, USER_ID))
          .thenReturn(Optional.of(membro));
      when(subscriptionRepository.findByWorkspaceIdAndStatus(eq(WORKSPACE_ID), any()))
          .thenReturn(Optional.empty());
      when(jwtService.gerarToken(any())).thenReturn("access-novo");
      mockSalvarRefreshToken();

      RenovarTokenResponse response = sut.renovarToken("refresh-antigo", httpResponse);

      assertThat(response.getAccessToken()).isEqualTo("access-novo");
      assertThat(antigo.isRevoked()).isTrue();
      assertThat(httpResponse.getHeader("Set-Cookie")).contains("refresh_token=");
    }

    @Test
    @DisplayName("Reuso de token revogado encerra todas as sessões do usuário (theft detection)")
    void tokenRevogado_revogaTodasAsSessoes() {
      TokenAtualizacaoJpaEntity revogado = refreshToken(true, Instant.now().plusSeconds(1000));
      when(refreshTokenRepository.findByToken("refresh-antigo")).thenReturn(Optional.of(revogado));

      assertThatThrownBy(() -> sut.renovarToken("refresh-antigo", httpResponse))
          .isInstanceOf(AuthException.class)
          .hasMessageContaining("reutilizado");

      verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
      verify(jwtService, never()).gerarToken(any());
    }

    @Test
    @DisplayName("Token expirado é rejeitado")
    void tokenExpirado_lancaExcecao() {
      TokenAtualizacaoJpaEntity expirado = refreshToken(false, Instant.now().minusSeconds(10));
      when(refreshTokenRepository.findByToken("refresh-antigo")).thenReturn(Optional.of(expirado));

      assertThatThrownBy(() -> sut.renovarToken("refresh-antigo", httpResponse))
          .isInstanceOf(AuthException.class)
          .hasMessageContaining("expirado");
    }

    @Test
    @DisplayName("Token desconhecido é rejeitado")
    void tokenDesconhecido_lancaExcecao() {
      when(refreshTokenRepository.findByToken("inexistente")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> sut.renovarToken("inexistente", httpResponse))
          .isInstanceOf(AuthException.class)
          .hasMessageContaining("não encontrado");
    }
  }

  @Nested
  @DisplayName("logout")
  class Logout {

    @Test
    @DisplayName("Revoga todos os refresh tokens e limpa o cookie")
    void logout_revogaTokensELimpaCookie() {
      sut.logout(USER_ID.toString(), httpResponse);

      verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
      String cookie = httpResponse.getHeader("Set-Cookie");
      assertThat(cookie).contains("refresh_token=;").contains("Max-Age=0");
    }
  }
}
