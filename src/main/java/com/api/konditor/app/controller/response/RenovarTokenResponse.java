package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resposta do endpoint {@code POST /auth/refresh}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RenovarTokenResponse {

  private String accessToken;
  private String tokenType;
  private long expiresIn;
}
