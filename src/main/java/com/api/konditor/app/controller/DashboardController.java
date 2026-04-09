package com.api.konditor.app.controller;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.response.DashboardStatsResponse;
import com.api.konditor.app.controller.response.ReceitaCardResponse;
import com.api.konditor.app.service.DashboardService;
import com.api.konditor.domain.enuns.RecipeStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do dashboard de receitas.
 *
 * <p>Rotas:
 *
 * <ul>
 *   <li>{@code GET /dashboard/estatisticas} — estatísticas gerais do workspace
 *   <li>{@code GET /dashboard/receitas} — grid de receitas com margens calculadas
 * </ul>
 *
 * <p>Todos os endpoints requerem JWT válido. O tenant é resolvido automaticamente a partir do
 * {@code workspaceId} contido no token — nenhum parâmetro adicional é necessário.
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * Retorna as estatísticas gerais do workspace autenticado: total de receitas, margem média e a
   * receita com maior margem.
   *
   * @param usuario principal autenticado extraído do JWT
   * @return 200 OK com as estatísticas consolidadas
   */
  @GetMapping("/estatisticas")
  public ResponseEntity<DashboardStatsResponse> buscarEstatisticas(
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    log.info(
        "[DASHBOARD] GET /estatisticas — workspaceId={} userId={}",
        usuario.workspaceId(),
        usuario.id());
    DashboardStatsResponse response = dashboardService.buscarEstatisticas(usuario);
    log.info(
        "[DASHBOARD] Estatísticas retornadas: totalReceitas={} margemMedia={}",
        response.getTotalReceitas(),
        response.getMargemMedia());
    return ResponseEntity.ok(response);
  }

  /**
   * Retorna a lista de receitas do workspace formatadas para o grid do dashboard.
   *
   * <p>Por padrão retorna apenas receitas {@code publicada}. Use {@code ?status=rascunho} para
   * listar os rascunhos em andamento.
   *
   * <p>Cada item inclui: nome, categoria, rendimento, custo total, preço unitário, margem de lucro,
   * status da margem e status da receita.
   *
   * @param usuario principal autenticado extraído do JWT
   * @param status {@code publicada} (padrão) ou {@code rascunho}
   * @return 200 OK com a lista de cards ordenada por nome
   */
  @GetMapping("/receitas")
  public ResponseEntity<List<ReceitaCardResponse>> listarReceitas(
      @AuthenticationPrincipal UsuarioAutenticado usuario,
      @RequestParam(defaultValue = "publicada") RecipeStatus status) {
    log.info(
        "[DASHBOARD] GET /receitas?status={} — workspaceId={} userId={}",
        status,
        usuario.workspaceId(),
        usuario.id());
    List<ReceitaCardResponse> response = dashboardService.listarReceitas(usuario, status);
    log.info(
        "[DASHBOARD] {} receitas retornadas (status={}) para workspaceId={}",
        response.size(),
        status,
        usuario.workspaceId());
    return ResponseEntity.ok(response);
  }
}
