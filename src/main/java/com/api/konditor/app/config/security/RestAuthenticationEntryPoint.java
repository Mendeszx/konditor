package com.api.konditor.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Retorna uma resposta JSON 401 padronizada quando uma rota protegida
 * é acessada sem um Bearer token válido.
 *
 * <p>Substitui o comportamento padrão do Spring Security que retornaria
 * um redirect para uma página de login HTML.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> corpo = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 401,
                "error", "Não autorizado",
                "message", "Autenticação necessária. Forneça um Bearer token válido.",
                "path", request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
