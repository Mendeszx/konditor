package com.api.konditor.app.exception;

/**
 * Exceção de domínio para falhas no fluxo de onboarding.
 *
 * <p>Tratada pelo {@link GlobalExceptionHandler} com resposta 422 (Unprocessable Entity).
 */
public class OnboardingException extends RuntimeException {

    public OnboardingException(String message) {
        super(message);
    }

    public OnboardingException(String message, Throwable cause) {
        super(message, cause);
    }
}
