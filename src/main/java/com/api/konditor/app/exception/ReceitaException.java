package com.api.konditor.app.exception;

/**
 * Exceção de domínio para operações de receitas/produtos.
 *
 * <p>Lançada quando uma regra de negócio é violada durante criação,
 * atualização, publicação ou cálculo de custos de receitas.
 */
public class ReceitaException extends RuntimeException {

    public ReceitaException(String message) {
        super(message);
    }
}

