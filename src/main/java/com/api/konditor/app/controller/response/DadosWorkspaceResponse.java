package com.api.konditor.app.controller.response;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dados do workspace (tenant) ativo retornados nas respostas de autenticação. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DadosWorkspaceResponse {

  private String id;
  private Role role;
  private Plan plano;
}
