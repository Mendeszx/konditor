package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Um ingrediente dentro da resposta de uma receita.
 *
 * <p>Inclui o custo calculado para facilitar a exibição do breakdown de custos no painel lateral.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredienteReceitaResponse {

    /** ID do vínculo produto-ingrediente (product_ingredients.id). */
    private String id;

    private String ingredienteId;
    private String ingredienteNome;
    private String marca;

    /** Quantidade utilizada nesta receita, na unidade informada. */
    private BigDecimal quantidade;

    private String unidadeId;
    private String unidadeSimbolo;
    private String unidadeNome;

    /**
     * Custo calculado para esta linha: {@code costPerUnit × quantidade × fatorConversão}.
     * Escala: 4 casas decimais.
     */
    private BigDecimal custoCalculado;

    /**
     * Custo unitário base do ingrediente (sem conversão).
     * Útil para o frontend exibir o preço de referência no breakdown de custos.
     */
    private BigDecimal custoPorUnidade;

    /** Nota específica para este ingrediente nesta receita (ex: "peneirar antes de usar"). */
    private String notas;
}

