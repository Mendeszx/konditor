package com.api.konditor.app.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload para criação e atualização de ingredientes.
 *
 * <p>Usado por {@code POST /ingredientes/estoque} e {@code PUT /ingredientes/estoque/{id}}.
 */
@Getter
@Setter
@NoArgsConstructor
public class CriarIngredienteRequest {

  @NotBlank(message = "Nome do ingrediente é obrigatório")
  private String nome;

  /** Código interno do ingrediente (ex: CHC-001). Opcional. */
  private String codigo;

  /** Descrição livre do ingrediente. Opcional. */
  private String descricao;

  /** Marca / fornecedor. Opcional. */
  private String marca;

  /** ID da categoria de ingrediente do workspace. Opcional. */
  private UUID categoriaId;

  /** ID da unidade de medida base (ex: UUID de "kg", "L"). Obrigatório. */
  @NotNull(message = "Unidade de medida é obrigatória")
  private UUID unidadeId;

  /** Custo por unidade base do ingrediente. Deve ser zero ou positivo. Obrigatório. */
  @NotNull(message = "Custo por unidade é obrigatório")
  @PositiveOrZero(message = "Custo por unidade deve ser zero ou positivo")
  private BigDecimal precoPorUnidade;

  /** Quantidade atual em estoque. Opcional (null = não controla estoque). */
  @PositiveOrZero(message = "Quantidade em estoque deve ser zero ou positiva")
  private BigDecimal estoqueQuantidade;

  /** Quantidade mínima para disparar alerta de estoque crítico. Opcional. */
  @PositiveOrZero(message = "Estoque mínimo de alerta deve ser zero ou positivo")
  private BigDecimal estoqueAlertaMinimo;

  /** Notas / observações adicionais. Opcional. */
  private String notas;
}
