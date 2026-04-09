package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categoria de receita retornada para os chips de filtro e seletores da tela de receitas.
 *
 * <p>Retornado pelo endpoint {@code GET /receitas/categorias}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaReceitaResponse {

  private String id;
  private String nome;

  /** Cor em hexadecimal para exibição na UI (ex: #F59E0B). {@code null} se não definida. */
  private String cor;
}
