package com.api.konditor.app.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload para criação e atualização de receitas.
 *
 * <p>Usado por {@code POST /receitas} e {@code PUT /receitas/{id}}.
 *
 * <p>Os campos de custo ({@code custoIngredientes}, {@code custoMaoDeObra}, {@code custosFixos},
 * {@code custoCalculado}, {@code precoSugerido}) devem ser preenchidos com o output de {@code POST
 * /receitas/calcular}. O servidor os persiste diretamente, sem recalcular.
 */
@Getter
@Setter
@NoArgsConstructor
public class CriarReceitaRequest {

  @NotBlank(message = "Nome da receita é obrigatório")
  private String nome;

  /** Descrição detalhada da receita (opcional). */
  private String descricao;

  /** Quantidade produzida pela receita (ex: 12 para "12 unidades"). */
  @NotNull(message = "Rendimento é obrigatório")
  @Positive(message = "Rendimento deve ser maior que zero")
  private BigDecimal rendimentoQuantidade;

  /** ID da unidade de rendimento (ex: UUID da unidade "unidades", "kg"). */
  @NotNull(message = "Unidade de rendimento é obrigatória")
  private UUID rendimentoUnidadeId;

  /** ID da categoria do produto no workspace. */
  private UUID categoriaId;

  /** Tempo de preparo em minutos. */
  @PositiveOrZero(message = "Tempo de preparo deve ser zero ou positivo")
  private Integer tempoPreparoMinutos;

  /** Lista de ingredientes da receita. Pode ser vazia. */
  @Valid private List<IngredienteReceitaRequest> ingredientes;

  /** Outras receitas usadas como ingrediente nesta receita. Pode ser vazia. */
  @Valid private List<ReceitaComoIngredienteRequest> receitasComoIngredientes;

  /** Notas do processo de preparo / dicas. */
  private String notas;

  /** Preço de venda final definido pelo usuário. */
  @NotNull(message = "Preço final é obrigatório")
  @PositiveOrZero(message = "Preço final deve ser zero ou positivo")
  private BigDecimal precoFinal;

  /**
   * Status desejado: {@code "rascunho"} (padrão) ou {@code "publicada"}. Ignorado em {@code PUT} se
   * não informado (mantém o status atual).
   */
  private String status;

  /**
   * Peso ou volume de cada unidade/porção (opcional). Exemplo: 15 (g por brigadeiro), 50 (ml por
   * porção de mousse).
   */
  @Positive(message = "Peso/volume por unidade deve ser maior que zero")
  private BigDecimal pesoPorUnidade;

  /** Unidade do {@code pesoPorUnidade} (ex: UUID da unidade "g", "ml"). */
  private UUID pesoPorUnidadeUnidadeId;

  // -------------------------------------------------------------------------
  // Custos pré-calculados — preenchidos com o output de POST /receitas/calcular
  // O servidor persiste esses valores diretamente, sem recalcular.
  // -------------------------------------------------------------------------

  /** Custo total dos ingredientes do lote (Σ costPerUnit × quantidade × fator). */
  @PositiveOrZero(message = "Custo de ingredientes deve ser zero ou positivo")
  private BigDecimal custoIngredientes;

  /** Custo de mão de obra do lote = {@code maoDeObraValorHora × (tempoPreparoMinutos / 60)}. */
  @PositiveOrZero(message = "Custo de mão de obra deve ser zero ou positivo")
  private BigDecimal custoMaoDeObra;

  /** Custos fixos aplicados ao lote (percentual ou absoluto). */
  @PositiveOrZero(message = "Custos fixos devem ser zero ou positivos")
  private BigDecimal custosFixos;

  /**
   * Custo total do lote = ingredientes + mão de obra + custos fixos. Persistido em {@code
   * calculated_cost}.
   */
  @PositiveOrZero(message = "Custo total deve ser zero ou positivo")
  private BigDecimal custoCalculado;

  /**
   * Preço sugerido calculado para o lote inteiro ({@code custoCalculado / (1 - margem/100)}).
   * Persistido em {@code suggested_price}.
   */
  @PositiveOrZero(message = "Preço sugerido deve ser zero ou positivo")
  private BigDecimal precoSugerido;

  // Parâmetros de referência — armazenados para exibição e auditoria

  /** Valor da hora de mão de obra utilizado no cálculo (R$/h). */
  @PositiveOrZero(message = "Valor da hora de mão de obra deve ser zero ou positivo")
  private BigDecimal maoDeObraValorHora;

  /** Valor dos custos fixos utilizado (percentual ou absoluto). */
  @PositiveOrZero(message = "Valor dos custos fixos deve ser zero ou positivo")
  private BigDecimal custosFixosValor;

  /** Como {@link #custosFixosValor} foi interpretado: {@code "percentual"} ou {@code "fixo"}. */
  @Pattern(
      regexp = "^(percentual|fixo)$",
      message = "Tipo dos custos fixos deve ser 'percentual' ou 'fixo'")
  private String custosFixosTipo;

  /** Margem de lucro desejada (%) utilizada para calcular o {@code precoSugerido}. */
  @DecimalMin(value = "0.0", message = "Margem deve ser >= 0")
  @DecimalMax(value = "99.0", message = "Margem deve ser < 100")
  private BigDecimal margemDesejada;

  // -------------------------------------------------------------------------
  // Valores calculados aprimorados — preenchidos com o output de POST /receitas/calcular
  // -------------------------------------------------------------------------

  /** Custo total do lote ÷ rendimento — custo para produzir uma unidade. */
  @PositiveOrZero(message = "Custo por unidade deve ser zero ou positivo")
  private BigDecimal custoTotalPorUnidade;

  /** Preço sugerido do lote ÷ rendimento — preço de venda por unidade. */
  @PositiveOrZero(message = "Preço sugerido por unidade deve ser zero ou positivo")
  private BigDecimal precoSugeridoPorUnidade;

  /**
   * Número de porções/unidades = rendimento (base) ÷ pesoPorUnidade (base). {@code null} quando
   * {@code pesoPorUnidade} não for informado.
   */
  @PositiveOrZero(message = "Número de porções deve ser zero ou positivo")
  private BigDecimal numeroPorcoesUnidades;

  /** Custo por grama ou mililitro. Disponível quando a unidade de rendimento é peso ou volume. */
  @PositiveOrZero(message = "Custo por g/ml deve ser zero ou positivo")
  private BigDecimal custoPorGramaOuMl;

  /**
   * Preço sugerido por grama ou mililitro. Disponível quando a unidade de rendimento é peso ou
   * volume.
   */
  @PositiveOrZero(message = "Preço sugerido por g/ml deve ser zero ou positivo")
  private BigDecimal precoPorGramaOuMl;

  /** Custo por porção/unidade individual. Disponível quando {@code numeroPorcoesUnidades} > 0. */
  @PositiveOrZero(message = "Custo por porção deve ser zero ou positivo")
  private BigDecimal custoPorPorcaoOuUnidade;

  /**
   * Preço sugerido por porção/unidade individual. Disponível quando {@code numeroPorcoesUnidades} >
   * 0.
   */
  @PositiveOrZero(message = "Preço sugerido por porção deve ser zero ou positivo")
  private BigDecimal precoPorPorcaoOuUnidade;
}
