package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Resultado do cálculo de custos e precificação de uma receita.
 *
 * <p>Retornado por {@code POST /receitas/calcular}.
 *
 * <p>Fórmulas aplicadas:
 * <pre>
 *   custoIngredientes    = Σ (costPerUnit × quantidade × fatorConversão)
 *   custoMaoDeObra       = maoDeObraValorHora × (tempoPreparoMinutos / 60)
 *   custosFixos          = custosFixosValor  (se tipo="fixo")
 *                        | custoIngredientes × custosFixosValor / 100  (se tipo="percentual")
 *   custoTotal           = custoIngredientes + custoMaoDeObra + custosFixos
 *   precoSugerido        = custoTotal / (1 - margem / 100)
 *   ---
 *   custoTotalPorUnidade    = custoTotal    / rendimentoQuantidade
 *   precoSugeridoPorUnidade = precoSugerido / rendimentoQuantidade
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustosCalculadosResponse {

    /** Custo total dos ingredientes do lote. */
    private BigDecimal custoIngredientes;

    /**
     * Custo de mão de obra do lote = {@code maoDeObraValorHora × (tempoPreparoMinutos / 60)}.
     */
    private BigDecimal custoMaoDeObra;

    /**
     * Custos fixos aplicados ao lote
     * (valor absoluto ou percentual sobre ingredientes, conforme {@link #custosFixosTipo}).
     */
    private BigDecimal custosFixos;

    /** Custo total do lote = ingredientes + mão de obra + custos fixos. */
    private BigDecimal custoTotal;

    /** Preço de venda sugerido para o lote inteiro. */
    private BigDecimal precoSugerido;

    /** Margem de lucro real obtida com o preço sugerido (1 casa decimal). */
    private BigDecimal margem;

    /** Quantidade produzida pelo lote (denominador). */
    private BigDecimal rendimentoQuantidade;

    /** Custo total dividido pelo rendimento — quanto custa produzir uma unidade. */
    private BigDecimal custoTotalPorUnidade;

    /** Preço sugerido dividido pelo rendimento — preço de venda por unidade. */
    private BigDecimal precoSugeridoPorUnidade;

    /** Valor da hora informado. */
    private BigDecimal maoDeObraValorHora;

    /** Tempo de preparo informado (minutos, pode ter fração). */
    private BigDecimal tempoPreparoMinutos;

    /** Valor informado para custos fixos. */
    private BigDecimal custosFixosValor;

    /**
     * Tipo de custo fixo utilizado: {@code "percentual"} ou {@code "fixo"}.
     */
    private String custosFixosTipo;

    /** Margem desejada utilizada no cálculo. */
    private BigDecimal margemUtilizada;
}
