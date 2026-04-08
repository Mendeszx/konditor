package com.api.konditor.domain.useCase;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CalcularCustosRequest;
import com.api.konditor.app.controller.request.CriarReceitaRequest;
import com.api.konditor.app.controller.response.BuscaIngredienteResponse;
import com.api.konditor.app.controller.response.CustosCalculadosResponse;
import com.api.konditor.app.controller.response.ReceitaResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso responsável pelo CRUD de receitas e cálculo de custos.
 *
 * <p>Todas as operações são scoped ao workspace do usuário autenticado,
 * garantindo isolamento total entre tenants.
 */
public interface ReceitaUseCase {

    /**
     * Cria uma nova receita para o workspace autenticado.
     * Calcula e persiste o custo total e o preço sugerido com base nos ingredientes informados.
     */
    ReceitaResponse criar(CriarReceitaRequest request, UsuarioAutenticado usuario);

    /**
     * Atualiza todos os campos de uma receita existente.
     * Recalcula custo e preço sugerido a partir dos novos ingredientes.
     */
    ReceitaResponse atualizar(UUID id, CriarReceitaRequest request, UsuarioAutenticado usuario);

    /**
     * Publica uma receita, tornando-a visível no dashboard e demais listagens.
     * Muda o status de {@code rascunho} para {@code publicada}.
     */
    ReceitaResponse publicar(UUID id, UsuarioAutenticado usuario);

    /**
     * Retorna os detalhes completos de uma receita pelo ID.
     */
    ReceitaResponse buscarPorId(UUID id, UsuarioAutenticado usuario);

    /**
     * Calcula custos e preço sugerido em tempo real para um conjunto de ingredientes,
     * sem persistir nada. Ideal para atualização dinâmica do painel lateral no front-end.
     *
     * <p>Permite sobrescrever os percentuais padrão de mão de obra, custos fixos e margem.
     */
    CustosCalculadosResponse calcularCustos(CalcularCustosRequest request, UsuarioAutenticado usuario);

    /**
     * Busca ingredientes do workspace por nome (autocomplete).
     * Retorna todos os ingredientes quando {@code query} é nulo ou vazio.
     */
    List<BuscaIngredienteResponse> buscarIngredientes(String query, UsuarioAutenticado usuario);
}

