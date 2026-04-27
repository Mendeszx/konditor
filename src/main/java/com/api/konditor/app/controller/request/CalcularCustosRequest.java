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
 * Payload para o endpoint de cálculo de custos em tempo real.
 *
 * <p>Usado por {@code POST /receitas/calcular}.
 *
 * <p>Cálculo de mão de obra:
 *
 * <pre>
 *   custoMaoDeObra = maoDeObraValorHora × (tempoPreparoMinutos / 60)
 * </pre>
 *
 * <p>Cálculo de custos fixos:
 *
 * <ul>
 *   <li>{@code custosFixosTipo = "percentual"} — percentual aplicado sobre o custo de ingredientes
 *   <li>{@code custosFixosTipo = "fixo"} — valor direto em reais (ex: R$ 200,00 de aluguel rateado)
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class CalcularCustosRequest {

  /**
   * Ingredientes convencionais da receita. Pode ser vazia se {@code receitasComoIngredientes} for
   * preenchida.
   */
  @Valid private List<IngredienteReceitaRequest> ingredientes;

  /** Outras receitas usadas como ingrediente nesta receita. */
  @Valid private List<ReceitaComoIngredienteRequest> receitasComoIngredientes;

  /** Rendimento total produzido pela receita (usado para calcular custo/preço por unidade). */
  @NotNull(message = "Rendimento é obrigatório")
  @Positive(message = "Rendimento deve ser maior que zero")
  private BigDecimal rendimentoQuantidade;

  /**
   * Valor da hora de trabalho do confeiteiro (ex: R$ 25,00/hora). O custo de mão de obra para esta
   * receita será: {@code maoDeObraValorHora × (tempoPreparoMinutos / 60)}.
   */
  @NotNull(message = "Valor da hora de mão de obra é obrigatório")
  @PositiveOrZero(message = "Valor da hora deve ser zero ou positivo")
  private BigDecimal maoDeObraValorHora;

  /**
   * Tempo de preparo da receita em minutos (aceita frações, ex: 90.5 para 90 min e 30 s). Usado
   * para calcular o custo de mão de obra proporcional: {@code custoMaoDeObra = maoDeObraValorHora ×
   * (tempoPreparoMinutos / 60)}. Exemplos: 90 para 1 h 30 min, 45 para 45 min, 120 para 2 h.
   */
  @NotNull(message = "Tempo de preparo é obrigatório")
  @DecimalMin(value = "0.0", message = "Tempo de preparo deve ser zero ou positivo")
  private BigDecimal tempoPreparoMinutos;

  /**
   * Valor dos custos fixos. Interpretado como percentual ou valor absoluto conforme {@link
   * #custosFixosTipo}.
   */
  @NotNull(message = "Valor dos custos fixos é obrigatório")
  @PositiveOrZero(message = "Valor dos custos fixos deve ser zero ou positivo")
  private BigDecimal custosFixosValor;

  /**
   * Define como {@link #custosFixosValor} é interpretado:
   *
   * <ul>
   *   <li>{@code "percentual"} — ex: 15 significa 15% do custo de ingredientes
   *   <li>{@code "fixo"} — ex: 200 significa R$ 200,00 fixos
   * </ul>
   */
  @NotBlank(message = "Tipo dos custos fixos é obrigatório")
  @Pattern(
      regexp = "^(percentual|fixo)$",
      message = "Tipo dos custos fixos deve ser 'percentual' ou 'fixo'")
  private String custosFixosTipo;

  /** Margem de lucro desejada para o preço sugerido. Padrão: 30%. Intervalo válido: [0, 99]. */
  @DecimalMin(value = "0.0", message = "Margem deve ser >= 0")
  @DecimalMax(value = "99.0", message = "Margem deve ser < 100 (evita divisão por zero)")
  private BigDecimal margemDesejada;

  /**
   * ID da unidade de rendimento (ex: UUID de "g", "ml", "un"). Quando informado, permite ao sistema
   * calcular custo/preço por grama/ml (para peso e volume) além do custo/preço por unidade/porção.
   */
  private UUID rendimentoUnidadeId;

  /**
   * Peso ou volume de cada unidade/porção (opcional). Exemplo: 15 (g por brigadeiro), 50 (ml por
   * porção de mousse). Quando informado junto com {@code pesoPorUnidadeUnidadeId}, o sistema
   * calcula automaticamente o número de unidades/porções e os custos por unidade/porção.
   */
  @Positive(message = "Peso/volume por unidade deve ser maior que zero")
  private BigDecimal pesoPorUnidade;

  /**
   * Unidade do {@code pesoPorUnidade} (ex: UUID da unidade "g", "ml"). Deve ser compatível com o
   * tipo da unidade de rendimento.
   */
  private UUID pesoPorUnidadeUnidadeId;
}
