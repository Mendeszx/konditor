package com.api.konditor.app.controller.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resposta genérica para listagens paginadas.
 *
 * @param <T> tipo do item da página
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginaResponse<T> {

  private List<T> conteudo;
  private int pagina;
  private int tamanho;
  private long totalElementos;
  private int totalPaginas;
}
