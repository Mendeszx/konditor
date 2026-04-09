package com.api.konditor.app.controller;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.OnboardingRequest;
import com.api.konditor.app.controller.response.OnboardingResponse;
import com.api.konditor.app.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de onboarding da aplicação.
 *
 * <p>Rotas:
 *
 * <ul>
 *   <li>{@code POST /onboarding} — cria o workspace inicial do usuário recém-autenticado
 * </ul>
 *
 * <p>Requer usuário autenticado via JWT. Os dados do usuário são extraídos diretamente do token —
 * nenhuma informação pessoal precisa ser enviada no body.
 */
@Slf4j
@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

  private final OnboardingService onboardingService;

  /**
   * Realiza o onboarding do usuário autenticado.
   *
   * <p>Cria o workspace, vincula o usuário como {@code owner} e provisiona a assinatura gratuita.
   * Operação idempotente — retorna 422 se o usuário já possui um workspace ativo.
   *
   * @param usuario principal autenticado extraído do JWT
   * @param request dados do workspace a ser criado
   * @return 201 Created com os dados do workspace e do usuário
   */
  @PostMapping
  public ResponseEntity<OnboardingResponse> realizarOnboarding(
      @AuthenticationPrincipal UsuarioAutenticado usuario,
      @Valid @RequestBody OnboardingRequest request) {
    log.info(
        "[ONBOARDING] Requisição recebida para usuário id={} email={}",
        usuario.id(),
        usuario.email());
    OnboardingResponse response = onboardingService.executarOnboarding(usuario, request);
    log.info(
        "[ONBOARDING] Onboarding concluído. workspaceId={} userId={}",
        response.getWorkspaceId(),
        usuario.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
