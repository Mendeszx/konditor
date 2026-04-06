package com.api.konditor.app.service;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Serviço responsável por emitir e validar tokens JWT da aplicação.
 *
 * <p>Claims do token (multi-tenant):
 * <ul>
 *   <li>{@code sub}           — ID único do usuário</li>
 *   <li>{@code email}         — endereço de e-mail</li>
 *   <li>{@code name}          — nome completo</li>
 *   <li>{@code workspaceId}   — ID do workspace (tenant) ativo</li>
 *   <li>{@code workspaceRole} — papel do usuário no workspace (owner/admin/member)</li>
 *   <li>{@code plan}          — plano do workspace (free/basic/premium)</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiracaoEmSegundos;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-seconds:900}") long expiracaoEmSegundos
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("security.jwt.secret deve ter no mínimo 32 caracteres");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoEmSegundos = expiracaoEmSegundos;
    }

    /**
     * Contexto necessário para emitir o token: dados do usuário + workspace ativo.
     *
     * @param usuario       entidade JPA do usuário
     * @param workspaceId   ID do workspace (tenant) ativo
     * @param workspaceRole papel do usuário no workspace
     * @param plan          plano do workspace
     */
    public record ContextoToken(
            UserJpaEntity usuario,
            String workspaceId,
            Role workspaceRole,
            Plan plan
    ) {}

    /**
     * Gera um JWT assinado com as claims do usuário e do workspace ativo.
     *
     * @param contexto dados do usuário e do tenant para compor o token
     * @return token JWT compacto assinado
     */
    public String gerarToken(ContextoToken contexto) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(contexto.usuario().getId()))
                .claim("email", contexto.usuario().getEmail())
                .claim("name", contexto.usuario().getName())
                .claim("workspaceId", contexto.workspaceId())
                .claim("workspaceRole", contexto.workspaceRole().name())
                .claim("plan", contexto.plan().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoEmSegundos)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Valida e extrai as claims de um JWT.
     *
     * @param token JWT compacto recebido no header Authorization
     * @return claims extraídas do token
     * @throws JwtException se o token for inválido ou expirado
     */
    public Claims validarEExtrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Retorna a validade do access token em segundos. */
    public long getExpiracaoEmSegundos() {
        return expiracaoEmSegundos;
    }
}
