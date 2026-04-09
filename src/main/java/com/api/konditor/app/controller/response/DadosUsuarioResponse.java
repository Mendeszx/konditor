package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dados básicos do usuário retornados nas respostas de autenticação. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DadosUsuarioResponse {

  private String id;
  private String nome;
  private String email;
}
