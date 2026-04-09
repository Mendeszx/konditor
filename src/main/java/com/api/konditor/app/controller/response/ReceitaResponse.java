package com.api.konditor.app.controller.response;

import com.api.konditor.domain.enuns.RecipeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Representação completa de uma receita, retornada pelos endpoints de CRUD.
 *
 * <p>Retornado por:
 * <ul>
 *   <li>{@code POST /receitas}</li>
 *   <li>{@code GET  /receitas/{id}}</li>
 *   <li>{@code PUT  /receitas/{id}}</li>
 *   <li>{@code POST /receitas/{id}/publicar}</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceitaResponse {

    private String id;
    private String nome;
    private String descricao;

    private String categoriaId;
    private String categoriaNome;

    /** Quantidade produzida pelo lote (ex: 12). */
    private BigDecimal rendimentoQuantidade;
    private String rendimentoUnidadeId;
    private String rendimentoUnidadeSimbolo;
    private String rendimentoUnidadeNome;

    /** Tempo estimado de preparo em minutos. */
    private Integer tempoPreparoMinutos;

    /** Ingredientes com quantidade, unidade e custo calculado por linha. */
    private List<IngredienteReceitaResponse> ingredientes;

    /** Notas do processo de preparo / dicas. */
    private String notas;

    /** Preço de venda final definido pelo usuário. */
    private BigDecimal precoFinal;

    /** Preço sugerido calculado pelo servidor com base no custo total e na margem padrão. */
    private BigDecimal precoSugerido;

    /** Custo total de ingredientes × fator de conversão (soma de todos os ingredientes). */
    private BigDecimal custoCalculado;

    /**
     * Margem real (%) calculada com base no {@code precoFinal} e no {@code custoCalculado}.
     * Fórmula: {@code ((precoFinal - custoCalculado) / precoFinal) × 100}.
     * {@code null} quando {@code precoFinal} é zero.
     */
    private BigDecimal margem;

    /** Status do ciclo de vida: {@code rascunho} ou {@code publicada}. */
    private RecipeStatus status;

    private boolean ativo;
    private Instant criadoEm;
    private Instant atualizadoEm;
}

