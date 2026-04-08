package com.api.konditor.app.service;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CriarIngredienteRequest;
import com.api.konditor.app.controller.response.AlertaMercadoItemResponse;
import com.api.konditor.app.controller.response.CategoriaIngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteCardResponse;
import com.api.konditor.app.controller.response.IngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteResumoResponse;
import com.api.konditor.app.controller.response.PaginaResponse;
import com.api.konditor.domain.useCase.IngredienteUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de aplicação — orquestrador da gestão de ingredientes.
 *
 * <p>Não contém regras de negócio. Delega toda a lógica ao {@link IngredienteUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngredienteService {

    private final IngredienteUseCase ingredienteUseCase;

    public PaginaResponse<IngredienteCardResponse> listar(
            UsuarioAutenticado usuario, UUID categoriaId, int pagina, int tamanho) {
        log.debug("[INGREDIENTE-SERVICE] Delegando listagem ao use case. workspaceId={}", usuario.workspaceId());
        return ingredienteUseCase.listar(usuario, categoriaId, pagina, tamanho);
    }

    public IngredienteResumoResponse resumo(UsuarioAutenticado usuario) {
        log.debug("[INGREDIENTE-SERVICE] Delegando resumo ao use case. workspaceId={}", usuario.workspaceId());
        return ingredienteUseCase.resumo(usuario);
    }

    public List<AlertaMercadoItemResponse> alertasMercado(UsuarioAutenticado usuario) {
        log.debug("[INGREDIENTE-SERVICE] Delegando alertas de mercado ao use case. userId={}", usuario.id());
        return ingredienteUseCase.alertasMercado(usuario);
    }

    public List<CategoriaIngredienteResponse> listarCategorias(UsuarioAutenticado usuario) {
        log.debug("[INGREDIENTE-SERVICE] Delegando listagem de categorias ao use case. workspaceId={}", usuario.workspaceId());
        return ingredienteUseCase.listarCategorias(usuario);
    }

    public IngredienteResponse criar(CriarIngredienteRequest request, UsuarioAutenticado usuario) {
        log.debug("[INGREDIENTE-SERVICE] Delegando criação ao use case. workspaceId={}", usuario.workspaceId());
        return ingredienteUseCase.criar(request, usuario);
    }

    public IngredienteResponse atualizar(UUID id, CriarIngredienteRequest request, UsuarioAutenticado usuario) {
        log.debug("[INGREDIENTE-SERVICE] Delegando atualização ao use case. id={} workspaceId={}", id, usuario.workspaceId());
        return ingredienteUseCase.atualizar(id, request, usuario);
    }
}

