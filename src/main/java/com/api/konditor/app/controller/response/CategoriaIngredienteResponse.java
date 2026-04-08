package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categoria de ingrediente retornada para os chips de filtro da tela de estoque.
 *
 * <p>Retornado pelo endpoint {@code GET /ingredientes/categorias}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaIngredienteResponse {

    private String id;
    private String nome;

    /** Cor em hexadecimal para exibição na UI (ex: #8B4513). {@code null} se não definida. */
    private String cor;
}

