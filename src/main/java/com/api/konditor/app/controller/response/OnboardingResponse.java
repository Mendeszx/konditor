package com.api.konditor.app.controller.response;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resposta do endpoint {@code POST /onboarding}.
 *
 * <p>Retorna os dados do workspace criado e as informações do usuário
 * necessários para o frontend inicializar a sessão corretamente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponse {

    private String workspaceId;
    private String workspaceNome;
    private String moeda;
    private Role role;
    private Plan plano;
    private DadosUsuarioResponse usuario;
}
