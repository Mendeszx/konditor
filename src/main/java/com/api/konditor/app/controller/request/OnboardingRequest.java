package com.api.konditor.app.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload recebido pelo endpoint {@code POST /onboarding}.
 *
 * <p>Contém os dados que o novo usuário informa durante o fluxo de onboarding: o nome do workspace
 * (confeitaria/doceria) e a moeda preferida. O usuário já está autenticado via JWT — seus dados são
 * extraídos do token.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingRequest {

  @NotBlank(message = "O nome do workspace é obrigatório")
  @Size(min = 2, max = 100, message = "O nome do workspace deve ter entre 2 e 100 caracteres")
  private String nomeWorkspace;

  @NotBlank(message = "A moeda é obrigatória")
  @Size(
      min = 3,
      max = 3,
      message = "A moeda deve ser um código ISO 4217 de 3 caracteres (ex: BRL, USD)")
  private String moeda;
}
