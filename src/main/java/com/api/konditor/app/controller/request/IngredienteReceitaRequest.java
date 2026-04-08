package com.api.konditor.app.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Um ingrediente dentro do payload de criação/atualização de receita
 * ou do endpoint de cálculo de custos.
 */
@Getter
@Setter
@NoArgsConstructor
public class IngredienteReceitaRequest {

    /** ID do ingrediente cadastrado no workspace. */
    @NotNull(message = "ID do ingrediente é obrigatório")
    private UUID ingredienteId;

    /** Quantidade utilizada nesta receita. */
    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    /** ID da unidade da quantidade acima (pode diferir da unidade base do ingrediente). */
    @NotNull(message = "ID da unidade é obrigatório")
    private UUID unidadeId;
}

