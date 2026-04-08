package com.api.konditor.domain.useCase.impl;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.response.DashboardStatsResponse;
import com.api.konditor.app.controller.response.MelhorMargemResponse;
import com.api.konditor.app.controller.response.ReceitaCardResponse;
import com.api.konditor.app.exception.DashboardException;
import com.api.konditor.domain.enuns.RecipeStatus;
import com.api.konditor.domain.useCase.DashboardUseCase;
import com.api.konditor.infra.jpa.entity.ProductCategoryJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import com.api.konditor.infra.jpa.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Implementação do caso de uso de dashboard.
 *
 * <p>Calcula métricas de receitas (produtos) do workspace autenticado.
 * Toda consulta é filtrada pelo {@code workspaceId} extraído do JWT,
 * garantindo isolamento total entre tenants.
 *
 * <p>Fórmula de margem de lucro por receita:
 * <pre>
 *   custoUnitario = calculatedCost / yieldQuantity
 *   margem (%) = ((sellingPrice - custoUnitario) / sellingPrice) × 100
 * </pre>
 *
 * <p>Uma margem é classificada como {@code "baixa"} quando inferior a
 * {@link #LIMIAR_MARGEM_BAIXA} por cento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardUseCaseImpl implements DashboardUseCase {

    /** Percentual abaixo do qual a margem é considerada baixa (destacada em vermelho no front). */
    private static final int LIMIAR_MARGEM_BAIXA = 30;

    private final ProductJpaRepository productRepository;

    // =========================================================================
    // Casos de uso
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse buscarEstatisticas(UsuarioAutenticado usuario) {
        UUID workspaceId = resolverWorkspaceId(usuario);
        log.info("[DASHBOARD-STATS] Buscando estatísticas para workspaceId={}", workspaceId);

        List<ProductJpaEntity> receitas = productRepository.findAllActiveByWorkspaceIdWithDetails(workspaceId, RecipeStatus.publicada);

        if (receitas.isEmpty()) {
            log.info("[DASHBOARD-STATS] Nenhuma receita ativa encontrada para workspaceId={}", workspaceId);
            return new DashboardStatsResponse(0, BigDecimal.ZERO, null);
        }

        List<BigDecimal> margens = receitas.stream()
                .map(this::calcularMargem)
                .toList();

        BigDecimal margemMedia = calcularMedia(margens);

        ProductJpaEntity melhorProduto = encontrarMelhorMargem(receitas, margens);
        BigDecimal melhorMargemValor = calcularMargem(melhorProduto);
        MelhorMargemResponse melhorMargem = new MelhorMargemResponse(
                melhorProduto.getName(),
                melhorMargemValor.setScale(0, RoundingMode.HALF_UP).intValue()
        );

        log.info("[DASHBOARD-STATS] Stats calculadas: total={} margemMedia={} melhorMargem={} ({}%)",
                receitas.size(), margemMedia, melhorProduto.getName(),
                melhorMargem.getMargem());

        return new DashboardStatsResponse(receitas.size(), margemMedia, melhorMargem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceitaCardResponse> listarReceitas(UsuarioAutenticado usuario, RecipeStatus status) {
        UUID workspaceId = resolverWorkspaceId(usuario);
        log.info("[DASHBOARD-RECEITAS] Listando receitas status={} — workspaceId={}", status, workspaceId);

        List<ProductJpaEntity> receitas = productRepository.findAllActiveByWorkspaceIdWithDetails(workspaceId, status);

        List<ReceitaCardResponse> cards = receitas.stream()
                .map(this::montarCard)
                .toList();

        log.info("[DASHBOARD-RECEITAS] {} receitas retornadas para workspaceId={}", cards.size(), workspaceId);
        return cards;
    }

    // =========================================================================
    // Construção do card de receita
    // =========================================================================

    private ReceitaCardResponse montarCard(ProductJpaEntity produto) {
        BigDecimal margem = calcularMargem(produto);
        int margemInt = margem.setScale(0, RoundingMode.HALF_UP).intValue();
        String margemStatus = margemInt < LIMIAR_MARGEM_BAIXA ? "baixa" : "normal";

        ProductCategoryJpaEntity category = produto.getCategory();
        UnitJpaEntity yieldUnit = produto.getYieldUnit();

        return new ReceitaCardResponse(
                produto.getId().toString(),
                produto.getName(),
                category != null ? category.getName() : null,
                produto.getYieldQuantity(),
                yieldUnit != null ? yieldUnit.getName() : null,
                produto.getCalculatedCost() != null
                        ? produto.getCalculatedCost().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                produto.getSellingPrice().setScale(2, RoundingMode.HALF_UP),
                margemInt,
                margemStatus,
                "custos.html?id=" + produto.getId(),
                produto.getStatus()
        );
    }

    // =========================================================================
    // Cálculo de margem
    // =========================================================================

    /**
     * Calcula a margem de lucro percentual de um produto.
     *
     * <pre>
     *   custoUnitario = calculatedCost / yieldQuantity
     *   margem (%) = ((sellingPrice - custoUnitario) / sellingPrice) × 100
     * </pre>
     *
     * <p>Retorna {@code 0} quando:
     * <ul>
     *   <li>{@code sellingPrice} é nulo ou zero (divisão impossível)</li>
     *   <li>{@code calculatedCost} é nulo (sem ingredientes cadastrados — custo = 0 → margem = 100%)</li>
     * </ul>
     */
    private BigDecimal calcularMargem(ProductJpaEntity produto) {
        BigDecimal sellingPrice = produto.getSellingPrice();
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal calculatedCost = produto.getCalculatedCost();
        if (calculatedCost == null) {
            // Sem custo registrado → margem total
            return BigDecimal.valueOf(100);
        }

        BigDecimal yieldQuantity = produto.getYieldQuantity();
        if (yieldQuantity == null || yieldQuantity.compareTo(BigDecimal.ZERO) == 0) {
            yieldQuantity = BigDecimal.ONE;
        }

        // custoUnitario = calculatedCost / yieldQuantity
        BigDecimal custoUnitario = calculatedCost.divide(yieldQuantity, 4, RoundingMode.HALF_UP);

        // margem = ((sellingPrice - custoUnitario) / sellingPrice) * 100
        BigDecimal lucroUnitario = sellingPrice.subtract(custoUnitario);
        BigDecimal margem = lucroUnitario
                .divide(sellingPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Garante que o valor fique no intervalo [0, 100]
        if (margem.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (margem.compareTo(BigDecimal.valueOf(100)) > 0) return BigDecimal.valueOf(100);

        return margem;
    }

    // =========================================================================
    // Helpers estatísticos
    // =========================================================================

    /**
     * Calcula a média aritmética de uma lista de valores, arredondada para 1 casa decimal.
     */
    private BigDecimal calcularMedia(List<BigDecimal> valores) {
        BigDecimal soma = valores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(valores.size()), 1, RoundingMode.HALF_UP);
    }

    /**
     * Retorna o produto com a maior margem de lucro calculada.
     */
    private ProductJpaEntity encontrarMelhorMargem(List<ProductJpaEntity> receitas, List<BigDecimal> margens) {
        int melhorIndice = 0;
        BigDecimal melhorValor = margens.get(0);

        for (int i = 1; i < margens.size(); i++) {
            if (margens.get(i).compareTo(melhorValor) > 0) {
                melhorValor = margens.get(i);
                melhorIndice = i;
            }
        }
        return receitas.get(melhorIndice);
    }

    // =========================================================================
    // Helpers de segurança
    // =========================================================================

    /**
     * Extrai e valida o workspaceId do token do usuário autenticado.
     *
     * @throws DashboardException se o usuário não estiver vinculado a um workspace
     */
    private UUID resolverWorkspaceId(UsuarioAutenticado usuario) {
        String workspaceIdStr = usuario.workspaceId();
        if (workspaceIdStr == null || workspaceIdStr.isBlank()) {
            log.warn("[DASHBOARD] Tentativa de acesso sem workspaceId — userId={}", usuario.id());
            throw new DashboardException(
                    "Usuário não está vinculado a nenhum workspace. Conclua o onboarding antes de acessar o dashboard.");
        }
        return UUID.fromString(workspaceIdStr);
    }
}

