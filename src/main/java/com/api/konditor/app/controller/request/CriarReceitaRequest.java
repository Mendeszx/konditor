package com.api.konditor.app.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload para criação e atualização de receitas.
 *
 * <p>Usado por {@code POST /receitas} e {@code PUT /receitas/{id}}.
 */
@Getter
@Setter
@NoArgsConstructor
public class CriarReceitaRequest {

  @NotBlank(message = "Nome da receita é obrigatório")
  private String nome;

  /** Descrição detalhada da receita (opcional). */
  private String descricao;

  /** Quantidade produzida pela receita (ex: 12 para "12 unidades"). */
  @NotNull(message = "Rendimento é obrigatório")
  @Positive(message = "Rendimento deve ser maior que zero")
  private BigDecimal rendimentoQuantidade;

  /** ID da unidade de rendimento (ex: UUID da unidade "unidades", "kg"). */
  @NotNull(message = "Unidade de rendimento é obrigatória")
  private UUID rendimentoUnidadeId;

  /** ID da categoria do produto no workspace. */
  private UUID categoriaId;

  /** Tempo de preparo em minutos. */
  @PositiveOrZero(message = "Tempo de preparo deve ser zero ou positivo")
  private Integer tempoPreparoMinutos;

  /** Lista de ingredientes da receita. Pode ser vazia. */
  @Valid private List<IngredienteReceitaRequest> ingredientes;

  /** Notas do processo de preparo / dicas. */
  private String notas;

  /** Preço de venda final definido pelo usuário. */
  @NotNull(message = "Preço final é obrigatório")
  @PositiveOrZero(message = "Preço final deve ser zero ou positivo")
  private BigDecimal precoFinal;

  /**
   * Status desejado: {@code "rascunho"} (padrão) ou {@code "publicada"}. Ignorado em {@code PUT} se
   * não informado (mantém o status atual).
   */
  private String status;
}
