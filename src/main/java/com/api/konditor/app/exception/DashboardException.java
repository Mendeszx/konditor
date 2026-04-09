package com.api.konditor.app.exception;

/**
 * Exceção de domínio para falhas no fluxo de dashboard.
 *
 * <p>Tratada pelo {@link GlobalExceptionHandler} com resposta 422 (Unprocessable Entity).
 */
public class DashboardException extends RuntimeException {

  public DashboardException(String message) {
    super(message);
  }

  public DashboardException(String message, Throwable cause) {
    super(message, cause);
  }
}
