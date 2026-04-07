package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Estatísticas gerais do dashboard para o workspace autenticado.
 *
 * <p>Retornado pelo endpoint {@code GET /dashboard/estatisticas}.
 *
 * <p>Exemplo de resposta:
 * <pre>
 * {
 *   "totalReceitas": 42,
 *   "margemMedia": 68.4,
 *   "melhorMargem": {
 *     "nome": "Macaron Baunilha",
 *     "margem": 81
 *   }
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    /** Número total de receitas ativas cadastradas no workspace. */
    private int totalReceitas;

    /**
     * Média aritmética das margens de lucro de todas as receitas ativas,
     * arredondada para 1 casa decimal (ex: 68.4).
     */
    private BigDecimal margemMedia;

    /**
     * Receita com a maior margem de lucro.
     * {@code null} se o workspace não possuir receitas ativas.
     */
    private MelhorMargemResponse melhorMargem;
}

