package com.api.konditor.domain.useCase;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.OnboardingRequest;
import com.api.konditor.app.controller.response.OnboardingResponse;

/**
 * Caso de uso responsável por realizar o onboarding de um novo usuário.
 *
 * <p>Cria o workspace inicial, vincula o usuário como {@code owner}, e provisiona a assinatura
 * gratuita ({@code free}).
 */
public interface OnboardingUseCase {

  /**
   * Executa o onboarding do usuário autenticado.
   *
   * @param usuario principal extraído do JWT (usuário já autenticado)
   * @param request dados informados pelo usuário no formulário de onboarding
   * @return dados do workspace criado e do usuário
   */
  OnboardingResponse executar(UsuarioAutenticado usuario, OnboardingRequest request);
}
