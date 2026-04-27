package com.api.konditor.app.controller.response;

import com.api.konditor.domain.enuns.RecipeStatus;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Card de uma receita exibida no grid do dashboard.
 *
 * <p>Retornado pelo endpoint {@code GET /dashboard/receitas}.
 *
 * <p>Exemplo de item:
 *
 * <pre>
 * {
 *   "id": "3fa85f64-...",
 *   "nome": "Ganache de Framboesa",
 *   "categoria": "Tortas",
 *   "quantidade": 24,
 *   "unidade": "mini tortas",
 *   "custoTotal": 63.26,
 *   "custoIngredientesPorUnidade": 0.93,
 *   "custoMaoDeObraPorUnidade": 1.56,
 *   "custosFixosPorUnidade": 0.14,
 *   "custoUnitario": 2.64,
 *   "precoUnitario": 6.50,
 *   "precoSugerido": 4.39,
 *   "margem": 59,
 *   "margemDesejada": 40,
 *   "margemStatus": "normal",
 *   "tempoPreparo": 90,
 *   "status": "publicada",
 *   "pesoPorUnidade": 15,
 *   "pesoPorUnidadeSimbolo": "g",
 *   "numeroPorcoesUnidades": 45.00,
 *   "custoPorGramaOuMl": 0.068500,
 *   "precoPorGramaOuMl": 0.097900,
 *   "custoPorPorcaoOuUnidade": 0.68,
 *   "precoPorPorcaoOuUnidade": 0.98
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
   * Quantidade produzida pela receita (rendimento). Representa {@code yieldQuantity} — ex: 24 (mini
   * tortas).
   */
  private BigDecimal quantidade;

  /**
   * Unidade do rendimento (ex: "mini tortas", "unidades", "kg"). {@code null} se unidade não
   * cadastrada.
   */
  private String unidade;

  /**
   * Custo total do lote completo = ingredientes + mão de obra + custos fixos. Corresponde ao {@code
   * calculatedCost} do produto.
   */
  private BigDecimal custoTotal;

  /**
   * Custo de ingredientes por unidade ({@code ingredientCost / yieldQuantity}). Útil para
   * visualizar o peso de cada componente no custo unitário.
   */
  private BigDecimal custoIngredientesPorUnidade;

  /**
   * Custo de mão de obra por unidade ({@code laborCost / yieldQuantity}). Zero quando não há tempo
   * de preparo ou valor/hora configurado.
   */
  private BigDecimal custoMaoDeObraPorUnidade;

  /**
   * Custos fixos por unidade ({@code fixedCosts / yieldQuantity}). Zero quando não há custos fixos
   * configurados.
   */
  private BigDecimal custosFixosPorUnidade;

  /**
   * Custo total por unidade ({@code unitCost = calculatedCost / yieldQuantity}). Soma dos três
   * componentes acima.
   */
  private BigDecimal custoUnitario;

  /** Preço de venda por unidade definido pelo usuário. Corresponde ao {@code sellingPrice}. */
  private BigDecimal precoUnitario;

  /**
   * Preço sugerido por unidade ({@code suggestedUnitPrice = suggestedPrice / yieldQuantity}).
   * {@code null} se não calculado. Serve como referência de comparação com o {@code precoUnitario}.
   */
  private BigDecimal precoSugerido;

  /**
   * Margem de lucro real em percentual, arredondada para inteiro (ex: 59). Calculada como {@code
   * ((precoUnitario - custoUnitario) / precoUnitario) × 100}.
   */
  private int margem;

  /**
   * Margem de lucro desejada pelo usuário (%) definida ao salvar a receita. Permite comparar a
   * margem real com a meta estabelecida.
   */
  private int margemDesejada;

  /**
   * Status da margem de lucro em relação ao limiar mínimo e à meta do usuário:
   *
   * <ul>
   *   <li>{@code "baixa"} — margem abaixo de 30% (limiar crítico, exibir em vermelho)
   *   <li>{@code "abaixo_desejada"} — margem >= 30% mas abaixo da {@code margemDesejada} (exibir em
   *       amarelo)
   *   <li>{@code "normal"} — margem >= {@code margemDesejada} (exibir em verde)
   * </ul>
   */
  private String margemStatus;

  /**
   * Tempo estimado de preparo em minutos. {@code null} se não informado. Útil para exibição rápida
   * no card sem precisar abrir a receita.
   */
  private Integer tempoPreparo;

  /**
   * Status do ciclo de vida da receita: {@code publicada} ou {@code rascunho}. Presente para que o
   * frontend possa diferenciar visualmente os cards.
   */
  private RecipeStatus status;

  /**
   * Peso ou volume de cada unidade/porção (ex: {@code 15} para "15g por brigadeiro"). {@code null}
   * se não configurado.
   */
  private BigDecimal pesoPorUnidade;

  /**
   * Símbolo da unidade do {@code pesoPorUnidade} (ex: {@code "g"}, {@code "ml"}). {@code null} se
   * não configurado.
   */
  private String pesoPorUnidadeSimbolo;

  /**
   * Número de porções/unidades do lote = rendimento (base) ÷ pesoPorUnidade (base). {@code null}
   * quando {@code pesoPorUnidade} não estiver configurado.
   */
  private BigDecimal numeroPorcoesUnidades;

  /**
   * Custo total por grama ou mililitro. Disponível quando a unidade de rendimento é peso ({@code
   * weight}) ou volume ({@code volume}). {@code null} caso contrário.
   */
  private BigDecimal custoPorGramaOuMl;

  /**
   * Preço sugerido por grama ou mililitro. Disponível quando a unidade de rendimento é peso ({@code
   * weight}) ou volume ({@code volume}). {@code null} caso contrário.
   */
  private BigDecimal precoPorGramaOuMl;

  /**
   * Custo por porção/unidade individual. Disponível quando {@code numeroPorcoesUnidades} está
   * calculado. {@code null} caso contrário.
   */
  private BigDecimal custoPorPorcaoOuUnidade;

  /**
   * Preço sugerido por porção/unidade individual. Disponível quando {@code numeroPorcoesUnidades}
   * está calculado. {@code null} caso contrário.
   */
  private BigDecimal precoPorPorcaoOuUnidade;
}
