package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resposta detalhada de um ingrediente.
 *
 * <p>Retornado pelos endpoints {@code POST /ingredientes/estoque}
 * e {@code PUT /ingredientes/estoque/{id}}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredienteResponse {

    private String id;

    /** Código interno (ex: CHC-001). {@code null} se não atribuído. */
    private String codigo;

    private String nome;

    private String descricao;

    /** Marca / fornecedor. {@code null} se não informado. */
    private String marca;

    /** ID da categoria. {@code null} se não categorizado. */
    private String categoriaId;

    /** Nome da categoria. {@code null} se não categorizado. */
    private String categoriaNome;

    /** ID da unidade de medida. */
    private String unidadeId;

    /** Símbolo da unidade de medida (ex: "kg", "L"). */
    private String unidadeSimbolo;

    /** Nome completo da unidade de medida. */
    private String unidadeNome;

    /** Custo por unidade base. */
    private BigDecimal precoPorUnidade;

    /** Quantidade atual em estoque. {@code null} se o estoque não é controlado. */
    private BigDecimal estoqueQuantidade;

    /** Quantidade mínima configurada para alerta de estoque crítico. */
    private BigDecimal estoqueAlertaMinimo;

    /** {@code true} quando {@code estoqueQuantidade < estoqueAlertaMinimo}. */
    private boolean estoqueCritico;

    /** Notas / observações adicionais. */
    private String notas;

    private Instant criadoEm;

    private Instant atualizadoEm;
}

