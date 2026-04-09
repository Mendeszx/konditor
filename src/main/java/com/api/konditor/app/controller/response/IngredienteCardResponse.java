package com.api.konditor.app.controller.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Card de um ingrediente exibido no grid da tela de Gestão de Ingredientes.
 *
 * <p>Retornado pelo endpoint {@code GET /ingredientes/estoque}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredienteCardResponse {

  private String id;

  /** Código interno do ingrediente (ex: CHC-001). {@code null} se não atribuído. */
  private String codigo;

  private String nome;

  /** Nome da categoria (ex: "Chocolate", "Laticínio"). {@code null} se não categorizado. */
  private String categoria;

  /** ID da categoria. {@code null} se não categorizado. */
  private String categoriaId;

  private String descricao;

  /** Símbolo da unidade de medida base (ex: "kg", "L"). */
  private String unidade;

  /** Custo por unidade base. */
  private BigDecimal preco;

  /**
   * Variação percentual do preço em relação ao registro anterior. Negativo = redução; positivo =
   * aumento. {@code null} se não há histórico.
   */
  private BigDecimal variacaoPreco;

  /** Quantidade atual em estoque. {@code null} se o estoque não é controlado. */
  private BigDecimal estoque;

  /** {@code true} quando {@code estoque < stockAlertMin}. */
  private boolean estoqueCritico;
}
