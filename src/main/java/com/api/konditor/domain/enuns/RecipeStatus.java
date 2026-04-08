package com.api.konditor.domain.enuns;

/**
 * Status de ciclo de vida de uma receita/produto.
 *
 * <ul>
 *   <li>{@code rascunho}  — receita em edição, invisível no dashboard e listagens.</li>
 *   <li>{@code publicada} — receita finalizada, visível em todas as listagens.</li>
 * </ul>
 */
public enum RecipeStatus {
    rascunho,
    publicada
}

