package com.api.konditor.app.service;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CalcularCustosRequest;
import com.api.konditor.app.controller.request.CriarReceitaRequest;
import com.api.konditor.app.controller.response.BuscaIngredienteResponse;
import com.api.konditor.app.controller.response.CustosCalculadosResponse;
import com.api.konditor.app.controller.response.ReceitaResponse;
import com.api.konditor.domain.useCase.ReceitaUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de aplicação — orquestrador de receitas.
 *
 * <p>Não contém regras de negócio. Delega toda a lógica ao {@link ReceitaUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceitaService {

    private final ReceitaUseCase receitaUseCase;

    public ReceitaResponse criar(CriarReceitaRequest request, UsuarioAutenticado usuario) {
        log.debug("[RECEITA-SERVICE] Delegando criação de receita ao use case. workspaceId={}", usuario.workspaceId());
        return receitaUseCase.criar(request, usuario);
    }

    public ReceitaResponse atualizar(UUID id, CriarReceitaRequest request, UsuarioAutenticado usuario) {
        log.debug("[RECEITA-SERVICE] Delegando atualização id={} ao use case.", id);
        return receitaUseCase.atualizar(id, request, usuario);
    }

    public ReceitaResponse publicar(UUID id, UsuarioAutenticado usuario) {
        log.debug("[RECEITA-SERVICE] Delegando publicação id={} ao use case.", id);
        return receitaUseCase.publicar(id, usuario);
    }

    public ReceitaResponse buscarPorId(UUID id, UsuarioAutenticado usuario) {
        log.debug("[RECEITA-SERVICE] Delegando busca id={} ao use case.", id);
        return receitaUseCase.buscarPorId(id, usuario);
    }

    public CustosCalculadosResponse calcularCustos(CalcularCustosRequest request, UsuarioAutenticado usuario) {
        log.debug("[RECEITA-SERVICE] Delegando cálculo de custos ao use case. workspaceId={}", usuario.workspaceId());
        return receitaUseCase.calcularCustos(request, usuario);
    }

    public List<BuscaIngredienteResponse> buscarIngredientes(String query, UsuarioAutenticado usuario) {
        log.debug("[RECEITA-SERVICE] Delegando busca de ingredientes query='{}' workspaceId={}", query, usuario.workspaceId());
        return receitaUseCase.buscarIngredientes(query, usuario);
    }
}

