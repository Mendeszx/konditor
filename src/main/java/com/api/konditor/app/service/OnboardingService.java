package com.api.konditor.app.service;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.OnboardingRequest;
import com.api.konditor.app.controller.response.OnboardingResponse;
import com.api.konditor.domain.useCase.OnboardingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço de aplicação — orquestrador de onboarding.
 *
 * <p>Não contém regras de negócio. Delega toda a lógica ao {@link OnboardingUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

  private final OnboardingUseCase onboardingUseCase;

  public OnboardingResponse executarOnboarding(
      UsuarioAutenticado usuario, OnboardingRequest request) {
    log.debug("[ONBOARDING-SERVICE] Delegando onboarding para use case. userId={}", usuario.id());
    return onboardingUseCase.executar(usuario, request);
  }
}
