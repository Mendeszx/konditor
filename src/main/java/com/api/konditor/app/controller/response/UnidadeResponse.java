package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa uma unidade de medida disponível para uso em ingredientes e receitas.
 *
 * <p>Retornado pelo endpoint {@code GET /unidades}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadeResponse {

    /** UUID da unidade — use este valor nos campos {@code unidadeId} dos ingredientes. */
    private String id;

    /** Nome completo (ex: "Quilograma", "Litro", "Unidade"). */
    private String nome;

    /** Símbolo de exibição (ex: "kg", "L", "un"). */
    private String simbolo;

    /**
     * Tipo da unidade:
     * <ul>
     *   <li>{@code weight} — peso (kg, g, mg)</li>
     *   <li>{@code volume} — volume (L, mL)</li>
     *   <li>{@code unit}   — contagem (un, dz)</li>
     * </ul>
     */
    private String tipo;

    /** {@code true} se é a unidade base do seu tipo (ex: kg para peso). */
    private boolean isBase;
}

