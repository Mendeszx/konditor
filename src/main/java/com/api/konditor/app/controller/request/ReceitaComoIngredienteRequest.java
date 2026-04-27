package com.api.konditor.app.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Uma receita usada como ingrediente em outra receita. */
@Getter
@Setter
@NoArgsConstructor
public class ReceitaComoIngredienteRequest {

  @NotNull(message = "ID da receita-ingrediente é obrigatório")
  private UUID receitaId;

  /** Quantidade de unidades da sub-receita (na sua unidade de rendimento). */
  @NotNull(message = "Quantidade é obrigatória")
  @Positive(message = "Quantidade deve ser maior que zero")
  private BigDecimal quantidade;

  private String notas;
}
