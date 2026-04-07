package com.api.konditor.domain.useCase;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.response.DashboardStatsResponse;
import com.api.konditor.app.controller.response.ReceitaCardResponse;

import java.util.List;

/**
 * Caso de uso responsável por fornecer os dados do dashboard de receitas.
 *
 * <p>Todas as operações são scoped ao workspace do usuário autenticado,
 * garantindo isolamento total entre tenants.
 */
public interface DashboardUseCase {

    /**
     * Retorna as estatísticas gerais do workspace:
     * total de receitas, margem média e receita com maior margem.
     *
     * @param usuario principal autenticado extraído do JWT
     * @return estatísticas consolidadas do workspace
     */
    DashboardStatsResponse buscarEstatisticas(UsuarioAutenticado usuario);

    /**
     * Retorna a lista de receitas ativas do workspace formatadas para o grid do dashboard,
     * incluindo cálculo de margem e status por receita.
     *
     * @param usuario principal autenticado extraído do JWT
     * @return lista de cards de receita, ordenada por nome
     */
    List<ReceitaCardResponse> listarReceitas(UsuarioAutenticado usuario);
}

