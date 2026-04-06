package com.api.konditor.domain.useCase.impl;

import com.api.konditor.app.controller.response.DadosUsuarioResponse;
import com.api.konditor.app.controller.response.DadosWorkspaceResponse;
import com.api.konditor.app.controller.response.GoogleAuthResponse;
import com.api.konditor.app.controller.response.RenovarTokenResponse;
import com.api.konditor.app.exception.AuthException;
import com.api.konditor.app.service.JwtService;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.domain.enuns.SubscriptionStatus;
import com.api.konditor.domain.useCase.AuthUseCase;
import com.api.konditor.infra.googleprovider.GoogleIdentityProvider;
import com.api.konditor.infra.googleprovider.response.GoogleUserResponse;
import com.api.konditor.infra.jpa.entity.RefreshTokenJpaEntity;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import com.api.konditor.infra.jpa.entity.WorkspaceJpaEntity;
import com.api.konditor.infra.jpa.entity.WorkspaceMemberJpaEntity;
import com.api.konditor.infra.jpa.repository.RefreshTokenJpaRepository;
import com.api.konditor.infra.jpa.repository.SubscriptionJpaRepository;
import com.api.konditor.infra.jpa.repository.UserJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceMemberJpaRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementação do caso de uso de autenticação.
 *
 * <p>Concentra <strong>todas</strong> as regras de negócio relacionadas à autenticação:
 * ciclo de vida do refresh token, gerenciamento do cookie HttpOnly,
 * resolução de workspace e emissão de JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUseCaseImpl implements AuthUseCase {

    // ---- Cookie ----
    private static final String NOME_COOKIE    = "refresh_token";
    private static final String CAMINHO_COOKIE = "/auth/refresh";

    @Value("${security.cookie.secure:true}")
    private boolean cookieSeguro;

    // ---- Refresh token ----
    @Value("${security.jwt.refresh-expiration-seconds:2592000}")
    private long refreshExpiracaoEmSegundos;

    private final GoogleIdentityProvider googleIdentityProvider;
    private final UserJpaRepository userRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final WorkspaceMemberJpaRepository workspaceMemberRepository;
    private final SubscriptionJpaRepository subscriptionRepository;
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final JwtService jwtService;

    // =========================================================================
    // Casos de uso
    // =========================================================================

    @Override
    @Transactional
    public GoogleAuthResponse loginComGoogle(String idToken, HttpServletResponse response) {
        log.info("[AUTH] Iniciando autenticação via Google");

        GoogleUserResponse dadosGoogle = googleIdentityProvider.verificar(idToken);
        log.debug("[AUTH] Token Google validado para email={}", dadosGoogle.getEmail());

        UserJpaEntity usuario = upsertUsuario(dadosGoogle);
        log.info("[AUTH] Usuário autenticado id={} email={}", usuario.getId(), usuario.getEmail());

        WorkspaceContext ctx = resolverWorkspace(usuario);

        Role role = Role.valueOf(ctx.member().getRole().getName());

        String accessToken = jwtService.gerarToken(new JwtService.ContextoToken(
                usuario, ctx.workspace().getId().toString(), role, ctx.plan()));

        RefreshTokenJpaEntity refreshToken = criarRefreshToken(usuario);
        adicionarCookie(response, refreshToken);

        log.debug("[AUTH] Tokens emitidos para usuário id={} workspace={}", usuario.getId(), ctx.workspace().getId());

        return new GoogleAuthResponse(
                accessToken,
                "Bearer",
                jwtService.getExpiracaoEmSegundos(),
                new DadosUsuarioResponse(usuario.getId().toString(), usuario.getName(), usuario.getEmail()),
                new DadosWorkspaceResponse(ctx.workspace().getId().toString(), role, ctx.plan())
        );
    }

    @Override
    @Transactional
    public RenovarTokenResponse renovarToken(String refreshToken, HttpServletResponse response) {
        log.debug("[AUTH] Renovação de token solicitada");

        RefreshTokenJpaEntity tokenAntigo = validarRefreshToken(refreshToken);
        UserJpaEntity usuario = userRepository.findById(tokenAntigo.getUserId())
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));

        log.debug("[AUTH] Refresh token válido para usuário id={}", usuario.getId());

        revogarToken(tokenAntigo);

        WorkspaceContext ctx = resolverWorkspace(usuario);

        RefreshTokenJpaEntity novoToken = criarRefreshToken(usuario);
        adicionarCookie(response, novoToken);

        Role role = Role.valueOf(ctx.member().getRole().getName());
        String novoAccessToken = jwtService.gerarToken(new JwtService.ContextoToken(
                usuario, ctx.workspace().getId().toString(), role, ctx.plan()));

        log.info("[AUTH] Tokens renovados para usuário id={} workspace={}", usuario.getId(), ctx.workspace().getId());

        return new RenovarTokenResponse(novoAccessToken, "Bearer", jwtService.getExpiracaoEmSegundos());
    }

    @Override
    @Transactional
    public void logout(String userId, HttpServletResponse response) {
        log.info("[AUTH] Logout solicitado para usuário id={}", userId);

        refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId));
        limparCookie(response);

        log.info("[AUTH] Todas as sessões encerradas para usuário id={}", userId);
    }

    // =========================================================================
    // Regras de negócio — Cookie
    // =========================================================================

    /**
     * Escreve o refresh token em cookie HttpOnly com flags de segurança:
     * {@code Secure}, {@code HttpOnly}, {@code SameSite=Strict}, path restrito.
     */
    private void adicionarCookie(HttpServletResponse response, RefreshTokenJpaEntity refreshToken) {
        long maxAge = refreshToken.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        if (maxAge <= 0) {
            log.warn("[AUTH] Tentativa de emitir cookie com token já expirado — ignorado");
            return;
        }
        response.addHeader("Set-Cookie", montarHeaderSetCookie(refreshToken.getToken(), (int) maxAge));
    }

    /**
     * Remove o cookie de refresh token (logout) zerando o Max-Age.
     */
    private void limparCookie(HttpServletResponse response) {
        String header = NOME_COOKIE + "=; Path=" + CAMINHO_COOKIE
                + "; HttpOnly; Max-Age=0; SameSite=Strict"
                + (cookieSeguro ? "; Secure" : "");
        response.addHeader("Set-Cookie", header);
    }

    private String montarHeaderSetCookie(String valor, int maxAgeSegundos) {
        return NOME_COOKIE + "=" + valor
                + "; Path=" + CAMINHO_COOKIE
                + "; HttpOnly"
                + "; Max-Age=" + maxAgeSegundos
                + "; SameSite=Strict"
                + (cookieSeguro ? "; Secure" : "");
    }

    // =========================================================================
    // Regras de negócio — Refresh Token
    // =========================================================================

    /**
     * Cria e persiste um novo refresh token para o usuário.
     */
    private RefreshTokenJpaEntity criarRefreshToken(UserJpaEntity usuario) {
        RefreshTokenJpaEntity token = RefreshTokenJpaEntity.builder()
                .token(UUID.randomUUID().toString())
                .userId(usuario.getId())
                .expiresAt(Instant.now().plusSeconds(refreshExpiracaoEmSegundos))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * Valida o token informado.
     *
     * <p>Implementa detecção de roubo (reuse detection): se um token já revogado
     * for apresentado, todas as sessões do usuário são imediatamente encerradas.
     */
    private RefreshTokenJpaEntity validarRefreshToken(String valorToken) {
        RefreshTokenJpaEntity token = refreshTokenRepository.findByToken(valorToken)
                .orElseThrow(() -> new AuthException("Refresh token não encontrado"));

        if (token.isRevoked()) {
            log.warn("[SECURITY] Reutilização de refresh token detectada para userId={}. Revogando todas as sessões.",
                    token.getUserId());
            refreshTokenRepository.revokeAllByUserId(token.getUserId());
            throw new AuthException("Token reutilizado. Todas as sessões foram encerradas por segurança.");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthException("Refresh token expirado. Por favor, faça login novamente.");
        }

        return token;
    }

    /**
     * Revoga um único token (rotação após emissão do novo).
     * A entidade está gerenciada pelo JPA na transação — não é necessário chamar save().
     */
    private void revogarToken(RefreshTokenJpaEntity token) {
        token.setRevoked(true);
        log.debug("[AUTH] Refresh token revogado id={}", token.getId());
    }

    // =========================================================================
    // Regras de negócio — Usuário e Workspace
    // =========================================================================

    /**
     * Cria um novo usuário ou atualiza nome e email caso já exista pelo googleId.
     * A entidade gerenciada pelo JPA é atualizada automaticamente via dirty checking —
     * não é necessário chamar save() explicitamente no branch de update.
     */
    private UserJpaEntity upsertUsuario(GoogleUserResponse dados) {
        return userRepository.findByGoogleId(dados.getGoogleId())
                .map(existente -> {
                    existente.setEmail(dados.getEmail());
                    existente.setName(dados.getName());
                    // updatedAt gerenciado pelo @PreUpdate da entidade
                    return existente;
                })
                .orElseGet(() -> {
                    log.info("[AUTH] Novo usuário criado via Google email={}", dados.getEmail());
                    return userRepository.save(UserJpaEntity.builder()
                            .googleId(dados.getGoogleId())
                            .email(dados.getEmail())
                            .name(dados.getName())
                            .build());
                });
    }

    /**
     * Resolve o workspace ativo do usuário, seu papel e o plano da assinatura.
     * Ignora workspaces com soft-delete e usa o mais recente como workspace ativo.
     */
    private WorkspaceContext resolverWorkspace(UserJpaEntity usuario) {
        UUID userId = usuario.getId();

        List<WorkspaceJpaEntity> workspaces = workspaceRepository.findAllByOwnerIdAndDeletedAtIsNull(userId);
        if (workspaces.isEmpty()) {
            throw new AuthException("Usuário não possui nenhum workspace ativo.");
        }

        WorkspaceJpaEntity workspace = workspaces.get(0);

        WorkspaceMemberJpaEntity member = workspaceMemberRepository
                .findByWorkspaceIdAndUser_Id(workspace.getId(), userId)
                .orElseThrow(() -> new AuthException(
                        "Usuário não é membro do workspace id=" + workspace.getId()));

        Plan plan = subscriptionRepository
                .findByWorkspaceIdAndStatus(workspace.getId(), SubscriptionStatus.active)
                .map(sub -> Plan.valueOf(sub.getPlan().getName()))
                .orElse(Plan.free);

        return new WorkspaceContext(workspace, member, plan);
    }

    private record WorkspaceContext(WorkspaceJpaEntity workspace, WorkspaceMemberJpaEntity member, Plan plan) {}
}
