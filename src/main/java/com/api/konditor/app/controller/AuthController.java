package com.api.konditor.app.controller;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.GoogleAuthRequest;
import com.api.konditor.app.controller.response.GoogleAuthResponse;
import com.api.konditor.app.controller.response.RenovarTokenResponse;
import com.api.konditor.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de autenticação da aplicação.
 *
 * <p>Rotas:
 *
 * <ul>
 *   <li>{@code POST /auth/google} — troca Google ID Token por JWT + cookie de refresh
 *   <li>{@code POST /auth/refresh} — renova o access token via cookie (sem Authorization)
 *   <li>{@code POST /auth/logout} — revoga todas as sessões e limpa o cookie
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Autentica o usuário com um Google ID Token. Retorna o access token no body e o refresh token em
   * cookie HttpOnly.
   */
  @PostMapping("/google")
  public ResponseEntity<GoogleAuthResponse> loginComGoogle(
      @Valid @RequestBody GoogleAuthRequest request) {
    log.info("[AUTH] Requisição de login com Google recebida");
    return ResponseEntity.ok(authService.loginComGoogle(request.getIdToken()));
  }

  /**
   * Renova o access token usando o refresh token do cookie HttpOnly. O cookie é enviado
   * automaticamente pelo browser — nenhum header adicional necessário.
   */
  @PostMapping("/refresh")
  public ResponseEntity<RenovarTokenResponse> renovarToken(
      @CookieValue(name = "refresh_token") String refreshToken) {
    log.info("[AUTH] Requisição de renovação de token recebida");
    return ResponseEntity.ok(authService.renovarToken(refreshToken));
  }

  /** Realiza o logout do usuário autenticado. Revoga todos os refresh tokens e remove o cookie. */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioAutenticado usuario) {
    log.info("[AUTH] Logout solicitado para usuário id={}", usuario.id());
    authService.logout(usuario.id());
    return ResponseEntity.noContent().build();
  }
}
