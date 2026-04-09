package com.api.konditor.app.controller;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CriarIngredienteRequest;
import com.api.konditor.app.controller.response.AlertaMercadoItemResponse;
import com.api.konditor.app.controller.response.CategoriaIngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteCardResponse;
import com.api.konditor.app.controller.response.IngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteResumoResponse;
import com.api.konditor.app.controller.response.PaginaResponse;
import com.api.konditor.app.service.IngredienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints da tela de Gestão de Ingredientes (Câmara Fria).
 *
 * <p>Rotas:
 * <ul>
 *   <li>{@code POST /ingredientes/estoque}               — cria um novo ingrediente</li>
 *   <li>{@code GET  /ingredientes/estoque}               — listagem paginada com filtro por categoria</li>
 *   <li>{@code PUT  /ingredientes/estoque/{id}}          — atualiza um ingrediente existente</li>
 *   <li>{@code GET  /ingredientes/estoque/resumo}        — painéis de resumo (total e estoque crítico)</li>
 *   <li>{@code GET  /ingredientes/estoque/alertas-mercado} — variações de preço recentes</li>
 *   <li>{@code GET  /ingredientes/categorias}            — chips de filtro por categoria</li>
 * </ul>
 *
 * <p>Todos os endpoints requerem JWT válido. O tenant é resolvido automaticamente
 * a partir do {@code workspaceId} contido no token.
 */
@Slf4j
@RestController
@RequestMapping("/ingredientes")
@RequiredArgsConstructor
public class IngredienteController {

    private final IngredienteService ingredienteService;

    /**
     * Cria um novo ingrediente no workspace autenticado.
     *
     * @return 201 Created com os detalhes completos do ingrediente criado
     */
    @PostMapping("/estoque")
    public ResponseEntity<IngredienteResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CriarIngredienteRequest request
    ) {
        log.info("[INGREDIENTE] POST /estoque — userId={} workspaceId={} nome={}",
                usuario.id(), usuario.workspaceId(), request.getNome());
        IngredienteResponse response = ingredienteService.criar(request, usuario);
        log.info("[INGREDIENTE] Ingrediente criado id={} — workspaceId={}", response.getId(), usuario.workspaceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retorna os detalhes completos de um ingrediente pelo ID.
     *
     * @param id ID do ingrediente
     * @return 200 OK com os dados completos do ingrediente
     */
    @GetMapping("/estoque/{id}")
    public ResponseEntity<IngredienteResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id
    ) {
        log.info("[INGREDIENTE] GET /estoque/{} — userId={} workspaceId={}", id, usuario.id(), usuario.workspaceId());
        IngredienteResponse response = ingredienteService.buscarPorId(id, usuario);
        log.info("[INGREDIENTE] Ingrediente id={} retornado — workspaceId={}", id, usuario.workspaceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna a listagem paginada de ingredientes do workspace.
     *
     * @param categoriaId ID da categoria para filtro (opcional)
     * @param pagina      índice da página, 0-based (padrão: 0)
     * @param tamanho     itens por página (padrão: 20)
     * @return 200 OK com a página de ingredientes
     */
    @GetMapping("/estoque")
    public ResponseEntity<PaginaResponse<IngredienteCardResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        log.info("[INGREDIENTE] GET /estoque — workspaceId={} categoriaId={} pagina={} tamanho={}",
                usuario.workspaceId(), categoriaId, pagina, tamanho);
        PaginaResponse<IngredienteCardResponse> response =
                ingredienteService.listar(usuario, categoriaId, pagina, tamanho);
        log.info("[INGREDIENTE] {} ingredientes retornados (total={}) — workspaceId={}",
                response.getConteudo().size(), response.getTotalElementos(), usuario.workspaceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza todos os campos de um ingrediente existente no workspace.
     *
     * @param id ID do ingrediente a atualizar
     * @return 200 OK com os detalhes completos do ingrediente atualizado
     */
    @PutMapping("/estoque/{id}")
    public ResponseEntity<IngredienteResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CriarIngredienteRequest request
    ) {
        log.info("[INGREDIENTE] PUT /estoque/{} — userId={} workspaceId={}", id, usuario.id(), usuario.workspaceId());
        IngredienteResponse response = ingredienteService.atualizar(id, request, usuario);
        log.info("[INGREDIENTE] Ingrediente id={} atualizado — workspaceId={}", id, usuario.workspaceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna os dados agregados para os painéis de resumo da tela:
     * total de ingredientes cadastrados e quantidade em estoque crítico.
     *
     * @return 200 OK com os dados de resumo
     */
    @GetMapping("/estoque/resumo")
    public ResponseEntity<IngredienteResumoResponse> resumo(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        log.info("[INGREDIENTE] GET /estoque/resumo — workspaceId={}", usuario.workspaceId());
        IngredienteResumoResponse response = ingredienteService.resumo(usuario);
        log.info("[INGREDIENTE] Resumo — total={} critico={}", response.getTotalIngredientes(), response.getEstoqueCritico());
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna as variações de preço recentes para o painel de Alerta de Mercado.
     *
     * <p>Os dados são parcialmente mockados até que a integração com fornecedores
     * externos esteja disponível.
     *
     * @return 200 OK com a lista de alertas de variação de preço
     */
    @GetMapping("/estoque/alertas-mercado")
    public ResponseEntity<List<AlertaMercadoItemResponse>> alertasMercado(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        log.info("[INGREDIENTE] GET /estoque/alertas-mercado — userId={}", usuario.id());
        return ResponseEntity.ok(ingredienteService.alertasMercado(usuario));
    }

    /**
     * Retorna todas as categorias de ingrediente do workspace,
     * usadas para preencher os chips de filtro da tela.
     *
     * @return 200 OK com a lista de categorias ordenada por nome
     */
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaIngredienteResponse>> listarCategorias(
            @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        log.info("[INGREDIENTE] GET /categorias — workspaceId={}", usuario.workspaceId());
        return ResponseEntity.ok(ingredienteService.listarCategorias(usuario));
    }
}

