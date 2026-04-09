package com.api.konditor.app.controller;

import com.api.konditor.app.controller.response.UnidadeResponse;
import com.api.konditor.app.service.UnidadeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints para consulta de unidades de medida.
 *
 * <p>Rotas:
 *
 * <ul>
 *   <li>{@code GET /unidades} — lista todas as unidades (com filtro opcional por tipo)
 * </ul>
 *
 * <p>Unidades são recursos globais reutilizados por ingredientes e receitas de todos os workspaces.
 * Requer JWT válido.
 */
@Slf4j
@RestController
@RequestMapping("/unidades")
@RequiredArgsConstructor
public class UnidadeController {

  private final UnidadeService unidadeService;

  /**
   * Lista todas as unidades de medida ativas, ordenadas por nome.
   *
   * <p>Use os UUIDs retornados no campo {@code unidadeId} ao criar ou atualizar ingredientes.
   *
   * @param tipo filtro opcional: {@code weight} (peso), {@code volume} ou {@code unit} (contagem)
   * @return 200 OK com a lista de unidades
   */
  @GetMapping
  public ResponseEntity<List<UnidadeResponse>> listar(@RequestParam(required = false) String tipo) {
    log.info("[UNIDADE] GET /unidades — tipo={}", tipo);
    List<UnidadeResponse> response = unidadeService.listar(tipo);
    log.info("[UNIDADE] {} unidades retornadas — tipo={}", response.size(), tipo);
    return ResponseEntity.ok(response);
  }
}
