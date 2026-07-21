package com.api.konditor.app.controller.request;

/**
 * Limites máximos sãos para os valores numéricos dos formulários (preços, quantidades, tempos).
 *
 * <p>Servem para barrar valores exorbitantes/digitação equivocada (ex: colar um número gigante no
 * campo de preço). São propositalmente generosos — muito acima de qualquer uso real de uma
 * confeitaria — apenas para conter absurdos. Os mesmos limites são aplicados no front (atributo
 * {@code max} + clamp) para feedback imediato; o backend é a autoridade final.
 */
public final class LimitesValores {

  private LimitesValores() {}

  /** Máximo para valores monetários (preço, custo, valor/hora): R$ 1.000.000,00. */
  public static final String MAX_VALOR = "1000000.00";

  /** Máximo para quantidades e pesos (rendimento, estoque, qtd. de ingrediente): 1.000.000. */
  public static final String MAX_QUANTIDADE = "1000000";

  /** Máximo para tempo de preparo em minutos (~69 dias). */
  public static final String MAX_TEMPO_MINUTOS = "100000";

  public static final String MSG_MAX_VALOR = "Valor deve ser no máximo 1.000.000,00";
  public static final String MSG_MAX_QUANTIDADE = "Quantidade deve ser no máximo 1.000.000";
  public static final String MSG_MAX_TEMPO = "Tempo de preparo deve ser no máximo 100.000 minutos";
}
