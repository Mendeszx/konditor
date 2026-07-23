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
 * Controller REST responsável pelos endpoints do dashboard de receitas da API Konditor.
 *
 * <p>Base path: {@code /dashboard}
 *
 * <p>Todos os endpoints são protegidos por autenticação JWT. O tenant (workspace) é resolvido
 * automaticamente a partir da claim {@code workspaceId} presente no token — nenhum parâmetro de
 * tenant precisa ser enviado pelo cliente.
 *
 * <h2>Endpoints disponíveis</h2>
 *
 * <table border="1" summary="Endpoints do DashboardController">
 *   <tr><th>Método</th><th>Path</th><th>Descrição</th></tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/dashboard/estatisticas</td>
 *     <td>Retorna estatísticas consolidadas do workspace: totais, margens e destaque da melhor receita.</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/dashboard/receitas</td>
 *     <td>Retorna lista de cards de receitas com custos detalhados, preços e indicadores de margem.</td>
 *   </tr>
 * </table>
 *
 * <h2>Autenticação e autorização</h2>
 *
 * <p>Requer cabeçalho {@code Authorization: Bearer <JWT>}. O principal autenticado é injetado
 * automaticamente via {@link org.springframework.security.core.annotation.AuthenticationPrincipal}
 * como uma instância de {@link com.api.konditor.app.config.security.UsuarioAutenticado}, que expõe:
 *
 * <ul>
 *   <li>{@code id} — ID único do usuário (claim {@code sub})
 *   <li>{@code email} — e-mail do usuário (claim {@code email})
 *   <li>{@code name} — nome completo (claim {@code name})
 *   <li>{@code workspaceId} — ID do workspace ativo (claim {@code workspaceId}); usado como chave
 *       de isolamento multi-tenant em todas as queries
 *   <li>{@code role} — papel do usuário no workspace (claim {@code workspaceRole})
 *   <li>{@code plan} — plano do workspace (claim {@code plan})
 * </ul>
 *
 * <h2>Tratamento de erros</h2>
 *
 * <ul>
 *   <li>{@code 401 Unauthorized} — token JWT ausente, expirado ou inválido
 *   <li>{@code 403 Forbidden} — usuário autenticado sem permissão para o recurso
 *   <li>{@code 500 Internal Server Error} — erros inesperados de processamento
 * </ul>
 *
 * @see com.api.konditor.app.service.DashboardService
 * @see com.api.konditor.domain.usecase.DashboardUseCase
 * @see com.api.konditor.app.controller.response.DashboardStatsResponse
 * @see com.api.konditor.app.controller.response.ReceitaCardResponse
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * Retorna as estatísticas gerais consolidadas do workspace autenticado.
   *
   * <h3>GET /dashboard/estatisticas</h3>
   *
   * <p>Calcula e agrega, em uma única chamada, os seguintes indicadores do workspace:
   *
   * <ul>
   *   <li>Total de receitas <strong>publicadas</strong> ativas
   *   <li>Total de receitas em <strong>rascunho</strong>
   *   <li>Quantidade de receitas com margem abaixo do limiar crítico de <strong>30%</strong>
   *       (alerta vermelho)
   *   <li>Quantidade de receitas abaixo da <strong>margem desejada</strong> pelo usuário, mas acima
   *       do limiar crítico (alerta amarelo)
   *   <li>Média das margens de lucro de todas as receitas publicadas (inteiro arredondado, HALF_UP)
   *   <li>Receita com a <strong>maior margem de lucro</strong> (id, nome e percentual)
   * </ul>
   *
   * <h3>Resposta de sucesso — 200 OK</h3>
   *
   * <p>Retorna {@link com.api.konditor.app.controller.response.DashboardStatsResponse} com os
   * campos:
   *
   * <ul>
   *   <li>{@code totalReceitas} — {@code int} — número de receitas publicadas ativas
   *   <li>{@code totalRascunhos} — {@code int} — número de rascunhos no workspace
   *   <li>{@code receitasComMargemBaixa} — {@code int} — receitas com margem {@literal <} 30%
   *   <li>{@code receitasAbaixoMargemDesejada} — {@code int} — receitas abaixo da meta do usuário
   *       (inclui as de margem baixa)
   *   <li>{@code margemMedia} — {@code int} — média das margens das receitas publicadas (%)
   *   <li>{@code melhorMargem} — {@link
   *       com.api.konditor.app.controller.response.MelhorMargemResponse} com {@code id} (UUID),
   *       {@code nome} (String) e {@code margem} (int); {@code null} se não houver receitas
   *       publicadas
   * </ul>
   *
   * <h3>Comportamento especial</h3>
   *
   * <p>Se o workspace não possuir receitas publicadas, todos os contadores retornam {@code 0} e
   * {@code melhorMargem} retorna {@code null}.
   *
   * <h3>Isolamento multi-tenant</h3>
   *
   * <p>As queries são filtradas pelo {@code workspaceId} extraído do JWT, garantindo que nenhum
   * dado de outros workspaces seja acessado.
   *
   * @param usuario principal autenticado extraído do JWT via {@link
   *     org.springframework.security.core.annotation.AuthenticationPrincipal}; fornece o {@code
   *     workspaceId} para escopo da consulta e o {@code id} do usuário para log
   * @return {@code 200 OK} contendo {@link
   *     com.api.konditor.app.controller.response.DashboardStatsResponse} com as estatísticas
   *     consolidadas do workspace
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
   * <h3>GET /dashboard/receitas?status={status}</h3>
   *
   * <p>Cada item da lista representa um card de receita com todos os dados financeiros calculados,
   * permitindo que o frontend exiba o grid sem processamento adicional.
   *
   * <h3>Parâmetro de query</h3>
   *
   * <ul>
   *   <li>{@code status} ({@link com.api.konditor.domain.enuns.RecipeStatus}) — filtra as receitas
   *       pelo status do ciclo de vida:
   *       <ul>
   *         <li>{@code publicada} <em>(padrão)</em> — receitas finalizadas e visíveis no dashboard
   *         <li>{@code rascunho} — receitas em edição, ainda não publicadas
   *       </ul>
   * </ul>
   *
   * <h3>Resposta de sucesso — 200 OK</h3>
   *
   * <p>Retorna uma {@code List} de {@link
   * com.api.konditor.app.controller.response.ReceitaCardResponse}, ordenada alfabeticamente por
   * nome. Cada item contém:
   *
   * <ul>
   *   <li>{@code id} — {@code String} (UUID) — identificador único da receita
   *   <li>{@code nome} — {@code String} — nome da receita
   *   <li>{@code categoria} — {@code String} — nome da categoria (ex: "Tortas"); {@code null} se
   *       não categorizada
   *   <li>{@code quantidade} — {@code BigDecimal} — rendimento do lote (ex: 24)
   *   <li>{@code unidade} — {@code String} — unidade do rendimento (ex: "mini tortas"); {@code
   *       null} se não cadastrada
   *   <li>{@code custoTotal} — {@code BigDecimal} — custo total do lote = ingredientes + mão de
   *       obra + custos fixos
   *   <li>{@code custoIngredientesPorUnidade} — {@code BigDecimal} — custo de ingredientes dividido
   *       pelo rendimento
   *   <li>{@code custoMaoDeObraPorUnidade} — {@code BigDecimal} — custo de mão de obra dividido
   *       pelo rendimento; {@code 0} se não há tempo de preparo ou valor/hora configurado
   *   <li>{@code custosFixosPorUnidade} — {@code BigDecimal} — custos fixos divididos pelo
   *       rendimento; {@code 0} se não há custos fixos configurados
   *   <li>{@code custoUnitario} — {@code BigDecimal} — custo total por unidade (soma dos três
   *       componentes acima)
   *   <li>{@code precoUnitario} — {@code BigDecimal} — preço de venda por unidade definido pelo
   *       usuário ({@code sellingPrice})
   *   <li>{@code precoSugerido} — {@code BigDecimal} — preço sugerido por unidade calculado pelo
   *       sistema; {@code null} se não disponível
   *   <li>{@code margem} — {@code int} — margem de lucro real em percentual, arredondada HALF_UP;
   *       fórmula: {@code ((precoUnitario - custoUnitario) / precoUnitario) × 100}
   *   <li>{@code margemDesejada} — {@code int} — meta de margem (%) definida pelo usuário ao salvar
   *       a receita
   *   <li>{@code margemStatus} — {@code String} — classificação da margem:
   *       <ul>
   *         <li>{@code "baixa"} — margem {@literal <} 30% (crítico, exibir em vermelho)
   *         <li>{@code "abaixo_desejada"} — margem {@literal >=} 30% mas abaixo da {@code
   *             margemDesejada} (atenção, exibir em amarelo)
   *         <li>{@code "normal"} — margem {@literal >=} {@code margemDesejada} (saudável, exibir em
   *             verde)
   *       </ul>
   *   <li>{@code tempoPreparo} — {@code Integer} (minutos) — tempo estimado de preparo; {@code
   *       null} se não informado
   *   <li>{@code status} — {@link com.api.konditor.domain.enuns.RecipeStatus} — status da receita
   *       ({@code publicada} ou {@code rascunho})
   * </ul>
   *
   * <h3>Isolamento multi-tenant</h3>
   *
   * <p>As queries são filtradas pelo {@code workspaceId} extraído do JWT, garantindo que nenhum
   * dado de outros workspaces seja retornado.
   *
   * @param usuario principal autenticado extraído do JWT via {@link
   *     org.springframework.security.core.annotation.AuthenticationPrincipal}; fornece o {@code
   *     workspaceId} para escopo da consulta e o {@code id} do usuário para log
   * @param status filtro de ciclo de vida das receitas — {@code publicada} (padrão) ou {@code
   *     rascunho}; mapeado diretamente para o enum {@link
   *     com.api.konditor.domain.enuns.RecipeStatus}
   * @return {@code 200 OK} com a lista de {@link
   *     com.api.konditor.app.controller.response.ReceitaCardResponse} ordenada por nome; lista
   *     vazia ({@code []}) se o workspace não possuir receitas com o status solicitado
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
