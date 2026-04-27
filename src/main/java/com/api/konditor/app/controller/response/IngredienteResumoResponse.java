package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados agregados para os painéis de resumo da tela de Gestão de Ingredientes.
 *
 * <p>Retornado pelo endpoint {@code GET /ingredientes/estoque/resumo}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IngredienteResumoResponse {

  /** Total de ingredientes cadastrados e ativos no workspace. */
  private long totalIngredientes;
}
