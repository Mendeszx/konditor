package com.api.konditor.app.controller.response;

import com.api.konditor.domain.enuns.RecipeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representação completa de uma receita, retornada pelos endpoints de CRUD.
 *
 * <p>Retornado por:
 *
 * <ul>
 *   <li>{@code POST /receitas}
 *   <li>{@code GET /receitas/{id}}
 *   <li>{@code PUT /receitas/{id}}
 *   <li>{@code POST /receitas/{id}/publicar}
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceitaResponse {

  private String id;
  private String nome;
  private String descricao;

  private String categoriaId;
  private String categoriaNome;

  /** Quantidade produzida pelo lote (ex: 12). */
  private BigDecimal rendimentoQuantidade;

  private String rendimentoUnidadeId;
  private String rendimentoUnidadeSimbolo;
  private String rendimentoUnidadeNome;

  /** Tempo estimado de preparo em minutos. */
  private Integer tempoPreparoMinutos;

  /** Ingredientes com quantidade, unidade e custo calculado por linha. */
  private List<IngredienteReceitaResponse> ingredientes;

  /** Outras receitas usadas como ingrediente nesta receita. */
  private List<ReceitaComoIngredienteResponse> receitasComoIngredientes;

  /** Notas do processo de preparo / dicas. */
  private String notas;

  /** Preço de venda final definido pelo usuário. */
  private BigDecimal precoFinal;

  /** Preço sugerido calculado pelo servidor com base no custo total e na margem desejada. */
  private BigDecimal precoSugerido;

  /** Custo total de ingredientes (soma de todos os ingredientes × fator de conversão). */
  private BigDecimal custoIngredientes;

  /** Custo de mão de obra = {@code maoDeObraValorHora × (tempoPreparoMinutos / 60)}. */
  private BigDecimal custoMaoDeObra;

  /** Custos fixos aplicados ao lote (percentual sobre ingredientes ou valor absoluto). */
  private BigDecimal custosFixos;

  /** Custo total do lote = ingredientes + mão de obra + custos fixos. */
  private BigDecimal custoCalculado;

  /**
   * Margem real (%) com base no {@code precoFinal} e no custo unitário real.
   *
   * <p>Fórmula: {@code ((precoFinal - custoTotalPorUnidade) / precoFinal) × 100}, onde {@code
   * custoTotalPorUnidade = custoCalculado / rendimentoQuantidade}.
   *
   * <p>{@code null} quando {@code precoFinal} é zero.
   */
  private BigDecimal margem;

  /** Valor da hora de mão de obra utilizado no cálculo. */
  private BigDecimal maoDeObraValorHora;

  /** Valor de custos fixos utilizado no cálculo. */
  private BigDecimal custosFixosValor;

  /** Tipo de custo fixo utilizado: {@code "percentual"} ou {@code "fixo"}. */
  private String custosFixosTipo;

  /** Margem desejada utilizada para calcular o {@code precoSugerido}. */
  private BigDecimal margemDesejada;

  /** Status do ciclo de vida: {@code rascunho} ou {@code publicada}. */
  private RecipeStatus status;

  private boolean ativo;
  private Instant criadoEm;
  private Instant atualizadoEm;

  /** Peso ou volume de cada unidade/porção (opcional). Exemplo: 15g por brigadeiro. */
  private BigDecimal pesoPorUnidade;

  /** ID da unidade do {@code pesoPorUnidade}. */
  private String pesoPorUnidadeUnidadeId;

  /** Símbolo da unidade do {@code pesoPorUnidade} (ex: "g", "ml"). */
  private String pesoPorUnidadeUnidadeSimbolo;

  /**
   * Número de unidades/porções calculado = rendimentoQuantidade (base) / pesoPorUnidade (base).
   * {@code null} quando {@code pesoPorUnidade} não for informado.
   */
  private BigDecimal numeroPorcoesUnidades;

  /**
   * Custo total por grama ou mililitro. Disponível quando a unidade de rendimento é de peso ou
   * volume.
   */
  private BigDecimal custoPorGramaOuMl;

  /**
   * Preço sugerido por grama ou mililitro. Disponível quando a unidade de rendimento é de peso ou
   * volume.
   */
  private BigDecimal precoPorGramaOuMl;

  /** Custo por unidade/porção. Disponível quando {@code pesoPorUnidade} é informado. */
  private BigDecimal custoPorPorcaoOuUnidade;

  /** Preço sugerido por unidade/porção. Disponível quando {@code pesoPorUnidade} é informado. */
  private BigDecimal precoPorPorcaoOuUnidade;
}
