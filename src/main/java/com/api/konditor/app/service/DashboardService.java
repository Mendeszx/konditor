package com.api.konditor.app.service;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.response.DashboardStatsResponse;
import com.api.konditor.app.controller.response.ReceitaCardResponse;
import com.api.konditor.domain.enuns.RecipeStatus;
import com.api.konditor.domain.useCase.DashboardUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço de aplicação — orquestrador do dashboard.
 *
 * <p>Não contém regras de negócio. Delega toda a lógica ao {@link DashboardUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

  private final DashboardUseCase dashboardUseCase;

  public DashboardStatsResponse buscarEstatisticas(UsuarioAutenticado usuario) {
    log.debug(
        "[DASHBOARD-SERVICE] Delegando busca de estatísticas para use case. workspaceId={}",
        usuario.workspaceId());
    return dashboardUseCase.buscarEstatisticas(usuario);
  }

  public List<ReceitaCardResponse> listarReceitas(UsuarioAutenticado usuario, RecipeStatus status) {
    log.debug(
        "[DASHBOARD-SERVICE] Delegando listagem de receitas status={} para use case."
            + " workspaceId={}",
        status,
        usuario.workspaceId());
    return dashboardUseCase.listarReceitas(usuario, status);
  }
}
