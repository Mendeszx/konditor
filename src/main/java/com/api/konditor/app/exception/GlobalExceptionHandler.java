package com.api.konditor.app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratador global de exceções da aplicação.
 *
 * <p>Todas as respostas seguem o padrão RFC 9457 (Problem Details for HTTP APIs).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata erros de autenticação vindos do domínio da aplicação.
     */
    @ExceptionHandler(AuthException.class)
    public ProblemDetail handleAuthException(AuthException ex) {
        log.warn("[EXCEPTION] Falha de autenticação: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        detail.setType(URI.create("https://konditor.api/errors/authentication-failed"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /**
     * Trata erros de autenticação (token inválido, credenciais erradas, etc.).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        log.warn("[EXCEPTION] Falha de autenticação: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        detail.setType(URI.create("https://konditor.api/errors/authentication-failed"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /**
     * Trata erros de validação de campos do request body.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "inválido" : fe.getDefaultMessage()
                ));
        log.warn("[EXCEPTION] Erro de validação: {}", erros);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validação falhou");
        detail.setType(URI.create("https://konditor.api/errors/validation-error"));
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("fieldErrors", erros);
        return detail;
    }

    /**
     * Trata erros de onboarding (regras de negócio, idempotência, dados de seed ausentes).
     */
    @ExceptionHandler(OnboardingException.class)
    public ProblemDetail handleOnboardingException(OnboardingException ex) {
        log.warn("[EXCEPTION] Falha no onboarding: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(422), ex.getMessage());
        detail.setType(URI.create("https://konditor.api/errors/onboarding-failed"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /**
     * Trata erros de regras de negócio do dashboard (ex: usuário sem workspace).
     */
    @ExceptionHandler(DashboardException.class)
    public ProblemDetail handleDashboardException(DashboardException ex) {
        log.warn("[EXCEPTION] Falha no dashboard: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(422), ex.getMessage());
        detail.setType(URI.create("https://konditor.api/errors/dashboard-failed"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /**
     * Trata erros de regras de negócio de receitas
     * (ex: nome duplicado, ingrediente não encontrado, status inválido).
     */
    @ExceptionHandler(ReceitaException.class)
    public ProblemDetail handleReceitaException(ReceitaException ex) {
        log.warn("[EXCEPTION] Falha na operação de receita: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(422), ex.getMessage());
        detail.setType(URI.create("https://konditor.api/errors/receita-failed"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    /**
     * Trata qualquer exceção não mapeada — retorna 500 e loga o stack trace completo.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("[EXCEPTION] Erro inesperado não tratado", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado");
        detail.setType(URI.create("https://konditor.api/errors/internal-error"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
