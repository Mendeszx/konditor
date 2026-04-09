package com.api.konditor.app.exception;

/**
 * Exceção de domínio para falhas no fluxo de autenticação.
 *
 * <p>Utilizada no lugar de {@code BadCredentialsException} do Spring Security, mantendo o domínio
 * desacoplado de frameworks externos. Tratada pelo {@link GlobalExceptionHandler} com resposta 401.
 */
public class AuthException extends RuntimeException {

  public AuthException(String message) {
    super(message);
  }

  public AuthException(String message, Throwable cause) {
    super(message, cause);
  }
}
