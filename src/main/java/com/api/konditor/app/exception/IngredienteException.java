package com.api.konditor.app.exception;

/**
 * Exceção de domínio para operações de gestão de ingredientes.
 *
 * <p>Lançada quando uma regra de negócio é violada durante listagem ou consulta de ingredientes.
 */
public class IngredienteException extends RuntimeException {

    public IngredienteException(String message) {
        super(message);
    }
}

