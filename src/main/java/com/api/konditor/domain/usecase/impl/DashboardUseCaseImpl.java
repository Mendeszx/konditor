package com.api.konditor.domain.usecase.impl;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.response.DashboardStatsResponse;
import com.api.konditor.app.controller.response.MelhorMargemResponse;
import com.api.konditor.app.controller.response.ReceitaCardResponse;
import com.api.konditor.app.exception.DashboardException;
import com.api.konditor.domain.enuns.RecipeStatus;
import com.api.konditor.domain.usecase.DashboardUseCase;
import com.api.konditor.infra.jpa.entity.ProductCategoryJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import com.api.konditor.infra.jpa.repository.ProductJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação do caso de uso de dashboard.
 *
 * <p>Calcula métricas de receitas (produtos) do workspace autenticado. Toda consulta é filtrada
 * pelo {@code workspaceId} extraído do JWT, garantindo isolamento total entre tenants.
 *
 * <p>Fórmula de margem de lucro por receita:
 *
 * <pre>
 *   custoUnitario = calculatedCost / yieldQuantity
 *   margem (%) = ((sellingPrice - custoUnitario) / sellingPrice) × 100
 * </pre>
 *
 * <p>Uma margem é classificada como {@code "baixa"} quando inferior a {@link #LIMIAR_MARGEM_BAIXA}
 * por cento.
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

    List<ProductJpaEntity> receitas =
        productRepository.findAllActiveByWorkspaceIdWithDetails(
            workspaceId, RecipeStatus.publicada);

    long totalRascunhos =
        productRepository.countActiveByWorkspaceIdAndStatus(workspaceId, RecipeStatus.rascunho);

    if (receitas.isEmpty()) {
      log.info(
          "[DASHBOARD-STATS] Nenhuma receita publicada encontrada para workspaceId={}",
          workspaceId);
      return new DashboardStatsResponse(0, (int) totalRascunhos, 0, 0, 0, null);
    }

    List<BigDecimal> margens = receitas.stream().map(this::calcularMargem).toList();

    int margemMedia = calcularMedia(margens);

    int receitasComMargemBaixa =
        (int)
            margens.stream()
                .filter(m -> m.setScale(0, RoundingMode.HALF_UP).intValue() < LIMIAR_MARGEM_BAIXA)
                .count();

    // Receitas onde a margem real ficou abaixo da meta do próprio usuário
    int receitasAbaixoMargemDesejada = 0;
    for (int i = 0; i < receitas.size(); i++) {
      ProductJpaEntity p = receitas.get(i);
      int margemReal = margens.get(i).setScale(0, RoundingMode.HALF_UP).intValue();
      int meta =
          p.getDesiredMargin() != null
              ? p.getDesiredMargin().setScale(0, RoundingMode.HALF_UP).intValue()
              : 30;
      if (margemReal < meta) {
        receitasAbaixoMargemDesejada++;
      }
    }

    int melhorIndice = encontrarIndiceMelhorMargem(margens);
    ProductJpaEntity melhorProduto = receitas.get(melhorIndice);
    int melhorMargemInt = margens.get(melhorIndice).setScale(0, RoundingMode.HALF_UP).intValue();
    MelhorMargemResponse melhorMargem =
        new MelhorMargemResponse(
            melhorProduto.getId().toString(), melhorProduto.getName(), melhorMargemInt);

    log.info(
        "[DASHBOARD-STATS] Stats calculadas: total={} rascunhos={} margemBaixa={} abaixoDesejada={}"
            + " margemMedia={}% melhorMargem={} ({}%)",
        receitas.size(),
        totalRascunhos,
        receitasComMargemBaixa,
        receitasAbaixoMargemDesejada,
        margemMedia,
        melhorProduto.getName(),
        melhorMargem.getMargem());

    return new DashboardStatsResponse(
        receitas.size(),
        (int) totalRascunhos,
        receitasComMargemBaixa,
        receitasAbaixoMargemDesejada,
        margemMedia,
        melhorMargem);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReceitaCardResponse> listarReceitas(UsuarioAutenticado usuario, RecipeStatus status) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.info(
        "[DASHBOARD-RECEITAS] Listando receitas status={} — workspaceId={}", status, workspaceId);

    List<ProductJpaEntity> receitas =
        productRepository.findAllActiveByWorkspaceIdWithDetails(workspaceId, status);

    List<ReceitaCardResponse> cards = receitas.stream().map(this::montarCard).toList();

    log.info(
        "[DASHBOARD-RECEITAS] {} receitas retornadas para workspaceId={}",
        cards.size(),
        workspaceId);
    return cards;
  }

  // =========================================================================
  // Construção do card de receita
  // =========================================================================

  private ReceitaCardResponse montarCard(ProductJpaEntity produto) {
    BigDecimal margem = calcularMargem(produto);
    int margemInt = margem.setScale(0, RoundingMode.HALF_UP).intValue();
    int margemDesejadaInt =
        produto.getDesiredMargin() != null
            ? produto.getDesiredMargin().setScale(0, RoundingMode.HALF_UP).intValue()
            : 30;

    String margemStatus;
    if (margemInt < LIMIAR_MARGEM_BAIXA) {
      margemStatus = "baixa";
    } else if (margemInt < margemDesejadaInt) {
      margemStatus = "abaixo_desejada";
    } else {
      margemStatus = "normal";
    }

    ProductCategoryJpaEntity category = produto.getCategory();
    UnitJpaEntity yieldUnit = produto.getYieldUnit();
    UnitJpaEntity weightUnit = produto.getUnitWeightUnit();

    BigDecimal yieldQty =
        (produto.getYieldQuantity() != null
                && produto.getYieldQuantity().compareTo(BigDecimal.ZERO) > 0)
            ? produto.getYieldQuantity()
            : BigDecimal.ONE;

    BigDecimal custoTotal =
        produto.getCalculatedCost() != null
            ? produto.getCalculatedCost().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // Breakdown de custo: prefere valores persistidos, fallback para derivação
    BigDecimal custoMaoDeObra =
        produto.getLaborCost() != null ? produto.getLaborCost() : BigDecimal.ZERO;
    BigDecimal custoFixos =
        produto.getFixedCosts() != null ? produto.getFixedCosts() : BigDecimal.ZERO;
    BigDecimal custoIngredientes =
        produto.getIngredientCost() != null
            ? produto.getIngredientCost()
            : (produto.getCalculatedCost() != null ? produto.getCalculatedCost() : BigDecimal.ZERO)
                .subtract(custoMaoDeObra)
                .subtract(custoFixos);

    BigDecimal custoIngredientesPorUnidade =
        custoIngredientes
            .divide(yieldQty, 4, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP);
    BigDecimal custoMaoDeObraPorUnidade =
        custoMaoDeObra.divide(yieldQty, 4, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    BigDecimal custosFixosPorUnidade =
        custoFixos.divide(yieldQty, 4, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);

    // Custo e preço por unidade: prefere valor persistido (unitCost / suggestedUnitPrice)
    BigDecimal custoUnitario =
        produto.getUnitCost() != null
            ? produto.getUnitCost().setScale(2, RoundingMode.HALF_UP)
            : (produto.getCalculatedCost() != null
                ? produto
                    .getCalculatedCost()
                    .divide(yieldQty, 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

    BigDecimal precoSugerido =
        produto.getSuggestedUnitPrice() != null
            ? produto.getSuggestedUnitPrice().setScale(2, RoundingMode.HALF_UP)
            : (produto.getSuggestedPrice() != null
                ? produto
                    .getSuggestedPrice()
                    .divide(yieldQty, 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP)
                : null);

    BigDecimal precoUnitario =
        produto.getSellingPrice() != null
            ? produto.getSellingPrice().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // Campos aprimorados (V4) — persistidos na receita
    BigDecimal numeroPorcoesUnidades =
        produto.getPortionCount() != null
            ? produto.getPortionCount().setScale(2, RoundingMode.HALF_UP)
            : null;
    BigDecimal custoPorGramaOuMl =
        produto.getCostPerGramMl() != null
            ? produto.getCostPerGramMl().setScale(6, RoundingMode.HALF_UP)
            : null;
    BigDecimal precoPorGramaOuMl =
        produto.getPricePerGramMl() != null
            ? produto.getPricePerGramMl().setScale(6, RoundingMode.HALF_UP)
            : null;
    BigDecimal custoPorPorcaoOuUnidade =
        produto.getCostPerPortion() != null
            ? produto.getCostPerPortion().setScale(2, RoundingMode.HALF_UP)
            : null;
    BigDecimal precoPorPorcaoOuUnidade =
        produto.getPricePerPortion() != null
            ? produto.getPricePerPortion().setScale(2, RoundingMode.HALF_UP)
            : null;

    return new ReceitaCardResponse(
        produto.getId().toString(),
        produto.getName(),
        category != null ? category.getName() : null,
        produto.getYieldQuantity(),
        yieldUnit != null ? yieldUnit.getName() : null,
        custoTotal,
        custoIngredientesPorUnidade,
        custoMaoDeObraPorUnidade,
        custosFixosPorUnidade,
        custoUnitario,
        precoUnitario,
        precoSugerido,
        margemInt,
        margemDesejadaInt,
        margemStatus,
        produto.getPrepTimeMinutes(),
        produto.getStatus(),
        produto.getUnitWeight(),
        weightUnit != null ? weightUnit.getSymbol() : null,
        numeroPorcoesUnidades,
        custoPorGramaOuMl,
        precoPorGramaOuMl,
        custoPorPorcaoOuUnidade,
        precoPorPorcaoOuUnidade);
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
   *
   * <ul>
   *   <li>{@code sellingPrice} é nulo ou zero (divisão impossível)
   * </ul>
   *
   * <p>Retorna {@code 100} quando {@code calculatedCost} é nulo (sem ingredientes cadastrados — sem
   * custo registrado, portanto margem total).
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

    // margem = ((sellingPrice - calculatedCost) / sellingPrice) * 100
    // Ambos estão no nível do lote (total), então não divide pelo rendimento
    BigDecimal lucroUnitario = sellingPrice.subtract(calculatedCost);
    BigDecimal margem =
        lucroUnitario
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

  /** Calcula a média aritmética de uma lista de valores, arredondada para inteiro (HALF_UP). */
  private int calcularMedia(List<BigDecimal> valores) {
    BigDecimal soma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return soma.divide(BigDecimal.valueOf(valores.size()), 0, RoundingMode.HALF_UP).intValue();
  }

  /** Retorna o índice da maior margem na lista. */
  private int encontrarIndiceMelhorMargem(List<BigDecimal> margens) {
    int melhorIndice = 0;
    BigDecimal melhorValor = margens.get(0);

    for (int i = 1; i < margens.size(); i++) {
      if (margens.get(i).compareTo(melhorValor) > 0) {
        melhorValor = margens.get(i);
        melhorIndice = i;
      }
    }
    return melhorIndice;
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
          "Usuário não está vinculado a nenhum workspace. Conclua o onboarding antes de acessar o"
              + " dashboard.");
    }
    return UUID.fromString(workspaceIdStr);
  }
}
