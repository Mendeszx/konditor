package com.api.konditor.app.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Item do painel de Alerta de Mercado — representa uma variação de preço recente.
 *
 * <p>Retornado pelo endpoint {@code GET /ingredientes/estoque/alertas-mercado}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaMercadoItemResponse {

    private String ingredienteId;
    private String ingredienteNome;

    /**
     * Variação percentual do preço (positivo = alta, negativo = baixa).
     * Ex: {@code 8.5} representa +8,5%; {@code -3.2} representa −3,2%.
     */
    private BigDecimal variacaoPercent;

    private BigDecimal precoAnterior;
    private BigDecimal precoAtual;

    /** {@code "alta"} ou {@code "baixa"}. */
    private String tipo;

    private Instant dataAlteracao;
}

