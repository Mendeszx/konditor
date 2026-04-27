package com.api.konditor.domain.usecase;

import com.api.konditor.app.controller.response.GoogleAuthResponse;
import com.api.konditor.app.controller.response.RenovarTokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthUseCase {

  GoogleAuthResponse loginComGoogle(String idToken, HttpServletResponse response);

  RenovarTokenResponse renovarToken(String refreshToken, HttpServletResponse response);

  void logout(String userId, HttpServletResponse response);
}
