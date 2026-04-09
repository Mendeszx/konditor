package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados da receita com a melhor margem de lucro do workspace.
 *
 * <p>Retornado como parte de {@link DashboardStatsResponse}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MelhorMargemResponse {

  /** Nome da receita com maior margem de lucro. */
  private String nome;

  /** Margem de lucro em percentual, arredondada para inteiro (ex: 81). */
  private int margem;
}
