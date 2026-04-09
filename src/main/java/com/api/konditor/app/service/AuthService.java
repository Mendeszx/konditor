package com.api.konditor.app.service;

import com.api.konditor.app.controller.response.GoogleAuthResponse;
import com.api.konditor.app.controller.response.RenovarTokenResponse;
import com.api.konditor.domain.useCase.AuthUseCase;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Serviço de aplicação — orquestrador de autenticação.
 *
 * <p>Não contém regras de negócio. Delega toda a lógica ao {@link AuthUseCase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthUseCase authUseCase;

  public GoogleAuthResponse loginComGoogle(String idToken) {
    return authUseCase.loginComGoogle(idToken, currentResponse());
  }

  public RenovarTokenResponse renovarToken(String refreshToken) {
    return authUseCase.renovarToken(refreshToken, currentResponse());
  }

  public void logout(String userId) {
    authUseCase.logout(userId, currentResponse());
  }

  private HttpServletResponse currentResponse() {
    return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
        .getResponse();
  }
}
