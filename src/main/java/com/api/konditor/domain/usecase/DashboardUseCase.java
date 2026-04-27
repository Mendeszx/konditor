package com.api.konditor.domain.usecase;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.response.DashboardStatsResponse;
import com.api.konditor.app.controller.response.ReceitaCardResponse;
import com.api.konditor.domain.enuns.RecipeStatus;
import java.util.List;

/**
 * Caso de uso responsável por fornecer os dados do dashboard de receitas.
 *
 * <p>Todas as operações são scoped ao workspace do usuário autenticado, garantindo isolamento total
 * entre tenants.
 */
public interface DashboardUseCase {

  /**
   * Retorna as estatísticas gerais do workspace: total de receitas, margem média e receita com
   * maior margem.
   *
   * @param usuario principal autenticado extraído do JWT
   * @return estatísticas consolidadas do workspace
   */
  DashboardStatsResponse buscarEstatisticas(UsuarioAutenticado usuario);

  /**
   * Retorna a lista de receitas do workspace formatadas para o grid do dashboard.
   *
   * <p>Por padrão retorna apenas receitas {@code publicada}. Passando {@code status = rascunho}
   * permite visualizar os rascunhos em andamento.
   *
   * @param usuario principal autenticado extraído do JWT
   * @param status filtro de status — {@code publicada} (padrão) ou {@code rascunho}
   * @return lista de cards de receita ordenada por nome
   */
  List<ReceitaCardResponse> listarReceitas(UsuarioAutenticado usuario, RecipeStatus status);
}
