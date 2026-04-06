package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resposta do endpoint {@code POST /auth/google}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private DadosUsuarioResponse usuario;
    private DadosWorkspaceResponse workspace;
}
