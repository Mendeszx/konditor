package com.api.konditor.app.controller.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Uma receita usada como ingrediente dentro de outra receita. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceitaComoIngredienteResponse {

  /** ID do vínculo receita-como-ingrediente. */
  private String id;

  private String receitaId;
  private String nome;

  /** Rendimento total da sub-receita (ex: 20 brigadeiros). */
  private BigDecimal rendimentoQuantidade;

  private String rendimentoUnidadeId;
  private String rendimentoUnidadeSimbolo;
  private String rendimentoUnidadeNome;

  /** Quantidade de unidades desta sub-receita utilizadas na receita-pai. */
  private BigDecimal quantidade;

  /** Preço de venda por unidade da sub-receita = precoFinal / rendimentoQuantidade. */
  private BigDecimal precoPorUnidade;

  /** Custo desta linha = quantidade × precoPorUnidade. */
  private BigDecimal custoCalculado;

  private String notas;
}
