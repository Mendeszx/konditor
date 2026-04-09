package com.api.konditor.domain.useCase;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CriarIngredienteRequest;
import com.api.konditor.app.controller.response.AlertaMercadoItemResponse;
import com.api.konditor.app.controller.response.CategoriaIngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteCardResponse;
import com.api.konditor.app.controller.response.IngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteResumoResponse;
import com.api.konditor.app.controller.response.PaginaResponse;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso responsável pela tela de Gestão de Ingredientes (câmara fria).
 *
 * <p>Todas as operações são scoped ao workspace do usuário autenticado.
 */
public interface IngredienteUseCase {

    /**
     * Retorna a listagem paginada de ingredientes do workspace,
     * com suporte a filtro opcional por categoria.
     *
     * @param usuario    principal autenticado
     * @param categoriaId ID de categoria para filtro, ou {@code null} para retornar todos
     * @param pagina     índice da página (0-based)
     * @param tamanho    quantidade de itens por página
     */
    PaginaResponse<IngredienteCardResponse> listar(
            UsuarioAutenticado usuario, UUID categoriaId, int pagina, int tamanho);

    /**
     * Retorna os dados agregados para os painéis de resumo:
     * total de ingredientes cadastrados e quantidade em estoque crítico.
     */
    IngredienteResumoResponse resumo(UsuarioAutenticado usuario);

    /**
     * Retorna variações de preço recentes para o painel de Alerta de Mercado.
     *
     * <p>Dados parcialmente mockados enquanto o esquema de monitoramento de preços
     * de mercado externo não está disponível.
     */
    List<AlertaMercadoItemResponse> alertasMercado(UsuarioAutenticado usuario);

    /**
     * Retorna todas as categorias de ingrediente do workspace,
     * usadas para preencher os chips de filtro da tela.
     */
    List<CategoriaIngredienteResponse> listarCategorias(UsuarioAutenticado usuario);

    /**
     * Retorna os detalhes completos de um ingrediente pelo ID,
     * garantindo que ele pertence ao workspace do usuário autenticado.
     *
     * @param id      ID do ingrediente
     * @param usuario principal autenticado
     * @return detalhes completos do ingrediente
     */
    IngredienteResponse buscarPorId(UUID id, UsuarioAutenticado usuario);

    /**
     * Cria um novo ingrediente no workspace autenticado.
     * Valida unicidade do nome e referências a unidade e categoria.
     *
     * @param request dados do ingrediente a ser criado
     * @param usuario principal autenticado
     * @return detalhes completos do ingrediente criado
     */
    IngredienteResponse criar(CriarIngredienteRequest request, UsuarioAutenticado usuario);

    /**
     * Atualiza todos os campos de um ingrediente existente no workspace.
     * Valida unicidade do nome (excluindo o próprio registro) e referências.
     *
     * @param id      ID do ingrediente a atualizar
     * @param request novos dados do ingrediente
     * @param usuario principal autenticado
     * @return detalhes completos do ingrediente atualizado
     */
    IngredienteResponse atualizar(UUID id, CriarIngredienteRequest request, UsuarioAutenticado usuario);
}

