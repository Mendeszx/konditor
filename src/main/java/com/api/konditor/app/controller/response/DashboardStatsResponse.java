package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estatísticas gerais do dashboard para o workspace autenticado.
 *
 * <p>Retornado pelo endpoint {@code GET /dashboard/estatisticas}.
 *
 * <pre>
 * {
 *   "totalReceitas": 42,
 *   "totalRascunhos": 5,
 *   "receitasComMargemBaixa": 3,
 *   "receitasAbaixoMargemDesejada": 8,
 *   "margemMedia": 68,
 *   "melhorMargem": {
 *     "id": "3fa85f64-...",
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

  /** Número total de receitas publicadas ativas no workspace. */
  private int totalReceitas;

  /** Número de receitas em status {@code rascunho} no workspace. */
  private int totalRascunhos;

  /**
   * Quantidade de receitas publicadas cuja margem real está abaixo de 30% (limiar crítico). Exibir
   * como alerta vermelho no dashboard.
   */
  private int receitasComMargemBaixa;

  /**
   * Quantidade de receitas publicadas onde a margem real ficou abaixo da {@code margemDesejada}
   * configurada pelo próprio usuário ao salvar a receita. Inclui as {@code receitasComMargemBaixa}.
   * Exibir como alerta amarelo/geral no dashboard.
   */
  private int receitasAbaixoMargemDesejada;

  /**
   * Média das margens de lucro de todas as receitas publicadas, arredondada para inteiro (ex: 68).
   * Usa o mesmo arredondamento HALF_UP das margens individuais exibidas nos cards.
   */
  private int margemMedia;

  /**
   * Receita com a maior margem de lucro. {@code null} se o workspace não possuir receitas ativas.
   */
  private MelhorMargemResponse melhorMargem;
}
