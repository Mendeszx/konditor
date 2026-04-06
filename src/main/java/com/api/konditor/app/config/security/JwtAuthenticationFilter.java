package com.api.konditor.app.config.security;

import com.api.konditor.app.service.JwtService;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que intercepta toda requisição HTTP para validar o Bearer JWT.
 *
 * <p>Não realiza consulta ao banco — o principal é um {@link UsuarioAutenticado}
 * montado diretamente a partir das claims do token, incluindo o contexto do tenant
 * (workspaceId, role e plano).
 *
 * <p>Em caso de token inválido, malformado ou com claims ausentes, a cadeia é
 * interrompida e a resposta 401 é enviada imediatamente pelo
 * {@link RestAuthenticationEntryPoint}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(PREFIXO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(PREFIXO_BEARER.length());

        try {
            Claims claims = jwtService.validarEExtrairClaims(token);

            String userId = claims.getSubject();
            if (userId == null) {
                throw new JwtException("Token sem subject (sub)");
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var principal = new UsuarioAutenticado(
                        userId,
                        claims.get("email", String.class),
                        claims.get("name", String.class),
                        claims.get("workspaceId", String.class),
                        Role.valueOf(claims.get("workspaceRole", String.class)),
                        Plan.valueOf(claims.get("plan", String.class))
                );

                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("[SECURITY] JWT válido — id={} workspace={} rota={}",
                        userId, principal.workspaceId(), request.getRequestURI());
            }

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[SECURITY] JWT rejeitado — rota={} motivo={}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.core.AuthenticationException(e.getMessage(), e) {});
        }
    }
}

