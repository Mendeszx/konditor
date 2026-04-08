package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Item retornado pelo endpoint de autocomplete de ingredientes.
 *
 * <p>Retornado por {@code GET /ingredientes?query=...}.
 * Permite que o front-end exiba sugestões ao usuário enquanto digita
 * e obtenha o ID correto para usar no payload de criação de receitas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuscaIngredienteResponse {

    /** ID do ingrediente — deve ser usado no campo {@code ingredienteId} ao criar receitas. */
    private String id;

    private String nome;
    private String marca;

    private String unidadeId;
    private String unidadeSimbolo;
    private String unidadeNome;

    /** Custo por unidade base cadastrado no workspace. */
    private BigDecimal custoPorUnidade;
}

