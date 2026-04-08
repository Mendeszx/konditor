package com.api.konditor.app.controller;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CalcularCustosRequest;
import com.api.konditor.app.controller.request.CriarReceitaRequest;
import com.api.konditor.app.controller.response.BuscaIngredienteResponse;
import com.api.konditor.app.controller.response.CategoriaReceitaResponse;
import com.api.konditor.app.controller.response.CustosCalculadosResponse;
import com.api.konditor.app.controller.response.ReceitaResponse;
import com.api.konditor.app.service.ReceitaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de gerenciamento de receitas e busca de ingredientes.
 *
 * <p>Rotas:
 * <ul>
 *   <li>{@code POST   /receitas}              — cria uma nova receita (rascunho por padrão)</li>
 *   <li>{@code GET    /receitas/{id}}          — retorna detalhes completos de uma receita</li>
 *   <li>{@code PUT    /receitas/{id}}          — atualiza uma receita (salvar rascunho)</li>
 *   <li>{@code POST   /receitas/{id}/publicar} — publica a receita (muda status para {@code publicada})</li>
 *   <li>{@code POST   /receitas/calcular}      — calcula custos em tempo real, sem persistir</li>
 *   <li>{@code GET    /ingredientes}           — autocomplete de ingredientes por nome</li>
 * </ul>
 *
 * <p>Todos os endpoints requerem JWT válido. O tenant é resolvido automaticamente
 * a partir do {@code workspaceId} contido no token.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReceitaController {

    private final ReceitaService receitaService;

    /**
     * Cria uma nova receita para o workspace autenticado.
     *
     * <p>Por padrão, a receita é criada como {@code rascunho}. Para criar já publicada,
     * envie {@code "status": "publicada"} no payload.
     * O custo total e o preço sugerido são calculados automaticamente pelo servidor.
     *
     * @return 201 Created com a receita criada
     */
    @PostMapping("/receitas")
    public ResponseEntity<ReceitaResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CriarReceitaRequest request
    ) {
        log.info("[RECEITA] POST /receitas — userId={} workspaceId={}", usuario.id(), usuario.workspaceId());
        ReceitaResponse response = receitaService.criar(request, usuario);
        log.info("[RECEITA] Receita criada id={} status={}", response.getId(), response.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retorna os detalhes completos de uma receita pelo ID,
     * incluindo lista de ingredientes com custos calculados.
     *
     * @return 200 OK com os dados da receita
     */
    @GetMapping("/receitas/{id}")
    public ResponseEntity<ReceitaResponse> buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id
    ) {
        log.info("[RECEITA] GET /receitas/{} — userId={}", id, usuario.id());
        return ResponseEntity.ok(receitaService.buscarPorId(id, usuario));
    }

    /**
     * Atualiza todos os campos de uma receita (salvar rascunho ou editar publicada).
     * Os ingredientes são completamente substituídos e os custos são recalculados.
     *
     * @return 200 OK com a receita atualizada
     */
    @PutMapping("/receitas/{id}")
    public ResponseEntity<ReceitaResponse> atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id,
            @Valid @RequestBody CriarReceitaRequest request
    ) {
        log.info("[RECEITA] PUT /receitas/{} — userId={}", id, usuario.id());
        ReceitaResponse response = receitaService.atualizar(id, request, usuario);
        log.info("[RECEITA] Receita id={} atualizada", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Publica uma receita, tornando-a visível no dashboard e listagens.
     * Muda o status de {@code rascunho} para {@code publicada}.
     *
     * @return 200 OK com a receita publicada
     */
    @PostMapping("/receitas/{id}/publicar")
    public ResponseEntity<ReceitaResponse> publicar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable UUID id
    ) {
        log.info("[RECEITA] POST /receitas/{}/publicar — userId={}", id, usuario.id());
        ReceitaResponse response = receitaService.publicar(id, usuario);
        log.info("[RECEITA] Receita id={} publicada", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Calcula custos e preço sugerido em tempo real para um conjunto de ingredientes,
     * sem persistir nenhum dado. Ideal para atualização dinâmica do painel lateral.
     *
     * <p>Os percentuais de mão de obra, custos fixos e margem desejada são opcionais
     * (padrão: 20 %, 15 % e 30 % respectivamente).
     *
     * @return 200 OK com o breakdown de custos e preço sugerido
     */
    @PostMapping("/receitas/calcular")
    public ResponseEntity<CustosCalculadosResponse> calcularCustos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody CalcularCustosRequest request
    ) {
        log.info("[RECEITA] POST /receitas/calcular — userId={} ingredientes={}",
                usuario.id(), request.getIngredientes().size());
        return ResponseEntity.ok(receitaService.calcularCustos(request, usuario));
    }

    /**
     * Retorna todas as categorias de receita globais, ordenadas por nome.
     * Usadas para preencher chips de filtro e o seletor de categoria ao criar/editar uma receita.
     *
     * @return 200 OK com a lista de categorias
     */
    @GetMapping("/receitas/categorias")
    public ResponseEntity<List<CategoriaReceitaResponse>> listarCategorias() {
        log.info("[RECEITA] GET /receitas/categorias");
        List<CategoriaReceitaResponse> response = receitaService.listarCategorias();
        log.info("[RECEITA] {} categorias retornadas", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Busca ingredientes do workspace por nome (autocomplete).
     * Retorna todos os ingredientes ativos quando {@code query} é omitido.
     *
     * <p>O ID retornado deve ser usado no campo {@code ingredienteId} ao criar/editar receitas.
     *
     * @param query prefixo ou trecho do nome do ingrediente (ex: {@code "framb"})
     * @return 200 OK com a lista de ingredientes correspondentes
     */
    @GetMapping("/ingredientes")
    public ResponseEntity<List<BuscaIngredienteResponse>> buscarIngredientes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) String query
    ) {
        log.debug("[INGREDIENTE] GET /ingredientes — query='{}' workspaceId={}", query, usuario.workspaceId());
        return ResponseEntity.ok(receitaService.buscarIngredientes(query, usuario));
    }
}

