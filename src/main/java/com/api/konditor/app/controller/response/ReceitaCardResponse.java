package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Card de uma receita exibida no grid do dashboard.
 *
 * <p>Retornado pelo endpoint {@code GET /dashboard/receitas}.
 *
 * <p>Exemplo de item:
 * <pre>
 * {
 *   "id": "3fa85f64-...",
 *   "nome": "Ganache de Framboesa",
 *   "categoria": "Tortas",
 *   "quantidade": 24,
 *   "unidade": "mini tortas",
 *   "custoTotal": 38.40,
 *   "precoUnitario": 8.50,
 *   "margem": 72,
 *   "margemStatus": "normal",
 *   "linkAnalise": "custos.html?id=3fa85f64-..."
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaCardResponse {

    /** ID único da receita. */
    private String id;

    /** Nome da receita. */
    private String nome;

    /** Nome da categoria (ex: "Tortas", "Brigadeiros"). {@code null} se não categorizada. */
    private String categoria;

    /**
     * Quantidade produzida pela receita (rendimento).
     * Representa {@code yieldQuantity} — ex: 24 (mini tortas).
     */
    private BigDecimal quantidade;

    /**
     * Unidade do rendimento (ex: "mini tortas", "unidades", "kg").
     * {@code null} se unidade não cadastrada.
     */
    private String unidade;

    /**
     * Custo total calculado para produzir o lote completo da receita.
     * Corresponde ao {@code calculatedCost} do produto.
     */
    private BigDecimal custoTotal;

    /**
     * Preço de venda por unidade produzida.
     * Corresponde ao {@code sellingPrice} do produto.
     */
    private BigDecimal precoUnitario;

    /**
     * Margem de lucro em percentual, arredondada para inteiro (ex: 72).
     * Calculada como {@code ((precoUnitario - custoUnitario) / precoUnitario) × 100}.
     */
    private int margem;

    /**
     * Status da margem de lucro.
     * {@code "baixa"} quando a margem está abaixo do limiar configurado;
     * {@code "normal"} caso contrário.
     */
    private String margemStatus;

    /**
     * Link para a página de análise detalhada desta receita no frontend.
     * Formato: {@code custos.html?id={id}}.
     */
    private String linkAnalise;
}

