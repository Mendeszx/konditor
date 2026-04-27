package com.api.konditor.domain.usecase.impl;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CalcularCustosRequest;
import com.api.konditor.app.controller.request.CriarReceitaRequest;
import com.api.konditor.app.controller.request.IngredienteReceitaRequest;
import com.api.konditor.app.controller.request.ReceitaComoIngredienteRequest;
import com.api.konditor.app.controller.response.BuscaIngredienteResponse;
import com.api.konditor.app.controller.response.CategoriaReceitaResponse;
import com.api.konditor.app.controller.response.CustosCalculadosResponse;
import com.api.konditor.app.controller.response.IngredienteReceitaResponse;
import com.api.konditor.app.controller.response.ReceitaComoIngredienteResponse;
import com.api.konditor.app.controller.response.ReceitaResponse;
import com.api.konditor.app.exception.ReceitaException;
import com.api.konditor.domain.enuns.AuditOperation;
import com.api.konditor.domain.enuns.RecipeStatus;
import com.api.konditor.domain.enuns.UnitType;
import com.api.konditor.domain.usecase.ReceitaUseCase;
import com.api.konditor.infra.jpa.entity.AuditLogJpaEntity;
import com.api.konditor.infra.jpa.entity.IngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductCategoryJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductIngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductRecipeIngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitConversionJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import com.api.konditor.infra.jpa.entity.WorkspaceJpaEntity;
import com.api.konditor.infra.jpa.repository.AuditLogJpaRepository;
import com.api.konditor.infra.jpa.repository.IngredientJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductCategoryJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductIngredientJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductRecipeIngredientJpaRepository;
import com.api.konditor.infra.jpa.repository.UnitConversionJpaRepository;
import com.api.konditor.infra.jpa.repository.UnitJpaRepository;
import com.api.konditor.infra.jpa.repository.UserJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação do caso de uso de receitas.
 *
 * <p>Cobre todo o ciclo de vida de uma receita: criação (rascunho), edição, publicação e cálculo de
 * custos em tempo real.
 *
 * <p>Fórmulas de precificação (percentuais configuráveis por request):
 *
 * <pre>
 *   custoIngredientes = Σ (costPerUnit × quantidade × fatorConversão)
 *   maoDeObra        = custoIngredientes × pctMaoDeObra / 100
 *   custosFixos      = custoIngredientes × pctCustosFixos / 100
 *   custoTotal       = custoIngredientes + maoDeObra + custosFixos
 *   precoSugerido    = custoTotal / (1 - margem / 100)
 * </pre>
 *
 * <p>Para conversão de unidades, consulta a tabela {@code unit_conversions}. Se não houver
 * conversão cadastrada entre as unidades, assume fator = 1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceitaUseCaseImpl implements ReceitaUseCase {

  private static final BigDecimal MARGEM_PADRAO = new BigDecimal("30.00");
  private static final String CUSTOS_FIXOS_TIPO_PADRAO = "percentual";

  private final ProductJpaRepository productRepository;
  private final ProductIngredientJpaRepository productIngredientRepository;
  private final ProductRecipeIngredientJpaRepository productRecipeIngredientRepository;
  private final ProductCategoryJpaRepository productCategoryRepository;
  private final IngredientJpaRepository ingredientRepository;
  private final UnitJpaRepository unitRepository;
  private final UnitConversionJpaRepository unitConversionRepository;
  private final WorkspaceJpaRepository workspaceRepository;
  private final UserJpaRepository userRepository;
  private final AuditLogJpaRepository auditLogRepository;

  // =========================================================================
  // Casos de uso
  // =========================================================================

  @Override
  @Transactional
  public ReceitaResponse criar(CriarReceitaRequest request, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.info("[RECEITA] Criando receita '{}' — workspaceId={}", request.getNome(), workspaceId);

    WorkspaceJpaEntity workspace = buscarWorkspace(workspaceId);
    validarNomeUnico(request.getNome(), workspaceId, null);

    ProductCategoryJpaEntity category =
        request.getCategoriaId() != null ? buscarCategoria(request.getCategoriaId()) : null;
    UnitJpaEntity yieldUnit =
        request.getRendimentoUnidadeId() != null
            ? buscarUnidade(request.getRendimentoUnidadeId())
            : null;

    RecipeStatus status = resolverStatus(request.getStatus());

    UnitJpaEntity pesoPorUnidadeUnit =
        request.getPesoPorUnidadeUnidadeId() != null
            ? buscarUnidade(request.getPesoPorUnidadeUnidadeId())
            : null;

    ProductJpaEntity product =
        ProductJpaEntity.builder()
            .workspace(workspace)
            .category(category)
            .name(request.getNome().trim())
            .description(request.getDescricao())
            .yieldQuantity(request.getRendimentoQuantidade())
            .yieldUnit(yieldUnit)
            .prepTimeMinutes(request.getTempoPreparoMinutos())
            .notes(request.getNotas())
            .sellingPrice(request.getPrecoFinal())
            .status(status)
            .unitWeight(request.getPesoPorUnidade())
            .unitWeightUnit(pesoPorUnidadeUnit)
            .laborCostPerHour(coalesce(request.getMaoDeObraValorHora(), BigDecimal.ZERO))
            .fixedCostsValue(coalesce(request.getCustosFixosValor(), BigDecimal.ZERO))
            .fixedCostsType(coalesce(request.getCustosFixosTipo(), CUSTOS_FIXOS_TIPO_PADRAO))
            .desiredMargin(coalesce(request.getMargemDesejada(), MARGEM_PADRAO))
            .build();

    // Aplica todos os custos ANTES do save para que tudo seja persistido em um único INSERT,
    // evitando que Hibernate faça INSERT + UPDATE (o que dispararia @PreUpdate e setaria
    // updatedAt na criação, além de criar uma janela de race condition).
    aplicarCustos(product, request);

    product = productRepository.save(product);

    List<IngredienteReceitaResponse> ingredientes =
        salvarIngredientes(product, request.getIngredientes(), workspaceId);
    List<ReceitaComoIngredienteResponse> receitasComoIngredientes =
        salvarReceitasComoIngredientes(product, request.getReceitasComoIngredientes(), workspaceId);
    log.info(
        "[RECEITA] Receita criada id={} status={} custo={}",
        product.getId(),
        status,
        product.getCalculatedCost());

    UserJpaEntity user = buscarUsuario(UUID.fromString(usuario.id()));
    registrarAuditLog(
        workspace,
        "Receita",
        product.getId(),
        AuditOperation.CREATE,
        "{\"nome\":\"" + product.getName() + "\",\"status\":\"" + status + "\"}",
        user);

    return montarResponse(product, ingredientes, receitasComoIngredientes);
  }

  @Override
  @Transactional
  public ReceitaResponse atualizar(
      UUID id, CriarReceitaRequest request, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.info("[RECEITA] Atualizando receita id={} — workspaceId={}", id, workspaceId);

    ProductJpaEntity product = buscarReceita(id, workspaceId);
    validarNomeUnico(request.getNome(), workspaceId, id);

    ProductCategoryJpaEntity category =
        request.getCategoriaId() != null ? buscarCategoria(request.getCategoriaId()) : null;
    UnitJpaEntity yieldUnit =
        request.getRendimentoUnidadeId() != null
            ? buscarUnidade(request.getRendimentoUnidadeId())
            : null;

    product.setName(request.getNome().trim());
    product.setDescription(request.getDescricao());
    product.setCategory(category);
    product.setYieldQuantity(request.getRendimentoQuantidade());
    product.setYieldUnit(yieldUnit);
    product.setPrepTimeMinutes(request.getTempoPreparoMinutos());
    product.setNotes(request.getNotas());
    product.setSellingPrice(request.getPrecoFinal());
    if (request.getStatus() != null) {
      product.setStatus(resolverStatus(request.getStatus()));
    }
    product.setUnitWeight(request.getPesoPorUnidade());
    product.setUnitWeightUnit(
        request.getPesoPorUnidadeUnidadeId() != null
            ? buscarUnidade(request.getPesoPorUnidadeUnidadeId())
            : null);
    product.setLaborCostPerHour(coalesce(request.getMaoDeObraValorHora(), BigDecimal.ZERO));
    product.setFixedCostsValue(coalesce(request.getCustosFixosValor(), BigDecimal.ZERO));
    product.setFixedCostsType(coalesce(request.getCustosFixosTipo(), CUSTOS_FIXOS_TIPO_PADRAO));
    product.setDesiredMargin(coalesce(request.getMargemDesejada(), MARGEM_PADRAO));

    // Substitui ingredientes completamente (delete + re-insert)
    productIngredientRepository.deleteAllByProductId(id);
    productRecipeIngredientRepository.deleteAllByProductId(id);

    List<IngredienteReceitaResponse> ingredientes =
        salvarIngredientes(product, request.getIngredientes(), workspaceId);
    List<ReceitaComoIngredienteResponse> receitasComoIngredientes =
        salvarReceitasComoIngredientes(product, request.getReceitasComoIngredientes(), workspaceId);

    aplicarCustos(product, request);
    product = productRepository.save(product);
    log.info("[RECEITA] Receita id={} atualizada unitCost={}", id, product.getUnitCost());

    UserJpaEntity user = buscarUsuario(UUID.fromString(usuario.id()));
    product.setUpdatedBy(user);
    productRepository.save(product);
    registrarAuditLog(
        product.getWorkspace(),
        "Receita",
        product.getId(),
        AuditOperation.UPDATE,
        "{\"nome\":\"" + product.getName() + "\",\"status\":\"" + product.getStatus() + "\"}",
        user);

    return montarResponse(product, ingredientes, receitasComoIngredientes);
  }

  @Override
  @Transactional
  public ReceitaResponse publicar(UUID id, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.info("[RECEITA] Publicando receita id={} — workspaceId={}", id, workspaceId);

    ProductJpaEntity product = buscarReceita(id, workspaceId);
    product.setStatus(RecipeStatus.publicada);

    List<IngredienteReceitaResponse> ingredientes = carregarIngredientesResponse(product);
    List<ReceitaComoIngredienteResponse> receitasComoIngredientes =
        carregarReceitasComoIngredientesResponse(product);
    log.info("[RECEITA] Receita id={} publicada com sucesso", id);

    UserJpaEntity user = buscarUsuario(UUID.fromString(usuario.id()));
    registrarAuditLog(
        product.getWorkspace(),
        "Receita",
        product.getId(),
        AuditOperation.UPDATE,
        "{\"status\":\"publicada\",\"evento\":\"publicacao\"}",
        user);

    return montarResponse(product, ingredientes, receitasComoIngredientes);
  }

  @Override
  @Transactional(readOnly = true)
  public ReceitaResponse buscarPorId(UUID id, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    ProductJpaEntity product = buscarReceita(id, workspaceId);
    List<IngredienteReceitaResponse> ingredientes = carregarIngredientesResponse(product);
    List<ReceitaComoIngredienteResponse> receitasComoIngredientes =
        carregarReceitasComoIngredientesResponse(product);
    return montarResponse(product, ingredientes, receitasComoIngredientes);
  }

  @Override
  @Transactional(readOnly = true)
  public CustosCalculadosResponse calcularCustos(
      CalcularCustosRequest request, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.debug("[RECEITA] Calculando custos — workspaceId={}", workspaceId);

    BigDecimal margemDesejada = coalesce(request.getMargemDesejada(), MARGEM_PADRAO);

    List<IngredienteReceitaRequest> ingredientesConvencionais =
        coalesce(request.getIngredientes(), List.of());
    List<ReceitaComoIngredienteRequest> receitasComoIngredientesReq =
        coalesce(request.getReceitasComoIngredientes(), List.of());

    if (ingredientesConvencionais.isEmpty() && receitasComoIngredientesReq.isEmpty()) {
      throw new ReceitaException(
          "Informe ao menos um ingrediente ou uma receita como ingrediente.");
    }

    BigDecimal custoIngredientes = BigDecimal.ZERO;
    for (IngredienteReceitaRequest item : ingredientesConvencionais) {
      IngredientJpaEntity ingredient =
          ingredientRepository
              .findByIdAndWorkspaceIdAndDeletedAtIsNull(item.getIngredienteId(), workspaceId)
              .orElseThrow(
                  () ->
                      new ReceitaException(
                          "Ingrediente não encontrado: " + item.getIngredienteId()));
      UnitJpaEntity unit = buscarUnidade(item.getUnidadeId());

      BigDecimal fator = resolverFatorConversao(unit, ingredient.getUnit());
      BigDecimal custo = ingredient.getCostPerUnit().multiply(item.getQuantidade().multiply(fator));
      custoIngredientes = custoIngredientes.add(custo);
    }

    // Custo das sub-receitas: quantidade × (precoFinal / rendimentoQuantidade)
    List<ReceitaComoIngredienteResponse> receitasComoIngredientesResp = new ArrayList<>();
    for (ReceitaComoIngredienteRequest item : receitasComoIngredientesReq) {
      ProductJpaEntity sub =
          productRepository
              .findByIdAndWorkspaceIdAndDeletedAtIsNull(item.getReceitaId(), workspaceId)
              .orElseThrow(
                  () ->
                      new ReceitaException(
                          "Receita-ingrediente não encontrada: " + item.getReceitaId()));
      BigDecimal precoPorUnidade = calcularPrecoPorUnidade(sub);
      BigDecimal custoLinha =
          precoPorUnidade.multiply(item.getQuantidade()).setScale(4, RoundingMode.HALF_UP);
      custoIngredientes = custoIngredientes.add(custoLinha);

      UnitJpaEntity yUnit = sub.getYieldUnit();
      receitasComoIngredientesResp.add(
          ReceitaComoIngredienteResponse.builder()
              .receitaId(sub.getId().toString())
              .nome(sub.getName())
              .rendimentoQuantidade(sub.getYieldQuantity())
              .rendimentoUnidadeId(yUnit != null ? yUnit.getId().toString() : null)
              .rendimentoUnidadeSimbolo(yUnit != null ? yUnit.getSymbol() : null)
              .rendimentoUnidadeNome(yUnit != null ? yUnit.getName() : null)
              .quantidade(item.getQuantidade())
              .precoPorUnidade(precoPorUnidade.setScale(4, RoundingMode.HALF_UP))
              .custoCalculado(custoLinha)
              .notas(item.getNotas())
              .build());
    }

    // 2. Mão de obra = valorHora × (minutos / 60)
    BigDecimal custoMaoDeObra = BigDecimal.ZERO;
    if (request.getTempoPreparoMinutos() != null
        && request.getTempoPreparoMinutos().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal horas =
          request.getTempoPreparoMinutos().divide(new BigDecimal("60"), 10, RoundingMode.HALF_UP);
      custoMaoDeObra = request.getMaoDeObraValorHora().multiply(horas);
    }

    // 3. Custos fixos — percentual sobre ingredientes ou valor direto
    BigDecimal custosFixos;
    if (CUSTOS_FIXOS_TIPO_PADRAO.equalsIgnoreCase(request.getCustosFixosTipo())) {
      custosFixos = percentual(custoIngredientes, request.getCustosFixosValor());
    } else {
      custosFixos = request.getCustosFixosValor();
    }

    // 4. Totais do lote
    BigDecimal custoTotal = custoIngredientes.add(custoMaoDeObra).add(custosFixos);
    BigDecimal precoSugerido = calcularPrecoSugerido(custoTotal, margemDesejada);
    BigDecimal margemReal = calcularMargemReal(custoTotal, precoSugerido);

    // 5. Por unidade
    BigDecimal rendimento = request.getRendimentoQuantidade();
    BigDecimal custoTotalPorUnidade = custoTotal.divide(rendimento, 4, RoundingMode.HALF_UP);
    BigDecimal precoSugeridoPorUnidade = precoSugerido.divide(rendimento, 4, RoundingMode.HALF_UP);

    // 6. Cálculos aprimorados de rendimento por tipo de unidade
    ResultadoRendimentoAprimorado aprimorado =
        calcularRendimentoAprimorado(
            rendimento,
            custoTotal,
            precoSugerido,
            request.getRendimentoUnidadeId(),
            request.getPesoPorUnidade(),
            request.getPesoPorUnidadeUnidadeId());

    return CustosCalculadosResponse.builder()
        .custoIngredientes(custoIngredientes.setScale(2, RoundingMode.HALF_UP))
        .receitasComoIngredientes(receitasComoIngredientesResp)
        .custoMaoDeObra(custoMaoDeObra.setScale(2, RoundingMode.HALF_UP))
        .custosFixos(custosFixos.setScale(2, RoundingMode.HALF_UP))
        .custoTotal(custoTotal.setScale(2, RoundingMode.HALF_UP))
        .precoSugerido(precoSugerido.setScale(2, RoundingMode.HALF_UP))
        .margem(margemReal.setScale(1, RoundingMode.HALF_UP))
        .rendimentoQuantidade(rendimento)
        .custoTotalPorUnidade(custoTotalPorUnidade.setScale(2, RoundingMode.HALF_UP))
        .precoSugeridoPorUnidade(precoSugeridoPorUnidade.setScale(2, RoundingMode.HALF_UP))
        .maoDeObraValorHora(request.getMaoDeObraValorHora())
        .tempoPreparoMinutos(request.getTempoPreparoMinutos())
        .custosFixosValor(request.getCustosFixosValor())
        .custosFixosTipo(request.getCustosFixosTipo())
        .margemUtilizada(margemDesejada)
        .numeroPorcoesUnidades(
            aprimorado.numeroPorcoesUnidades() != null
                ? aprimorado.numeroPorcoesUnidades().setScale(2, RoundingMode.HALF_UP)
                : null)
        .custoPorGramaOuMl(
            aprimorado.custoPorGramaOuMl() != null
                ? aprimorado.custoPorGramaOuMl().setScale(6, RoundingMode.HALF_UP)
                : null)
        .precoPorGramaOuMl(
            aprimorado.precoPorGramaOuMl() != null
                ? aprimorado.precoPorGramaOuMl().setScale(6, RoundingMode.HALF_UP)
                : null)
        .custoPorPorcaoOuUnidade(
            aprimorado.custoPorPorcaoOuUnidade() != null
                ? aprimorado.custoPorPorcaoOuUnidade().setScale(2, RoundingMode.HALF_UP)
                : null)
        .precoPorPorcaoOuUnidade(
            aprimorado.precoPorPorcaoOuUnidade() != null
                ? aprimorado.precoPorPorcaoOuUnidade().setScale(2, RoundingMode.HALF_UP)
                : null)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoriaReceitaResponse> listarCategorias() {
    log.debug("[RECEITA] Listando categorias globais de receita");

    return productCategoryRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
        .map(
            c ->
                CategoriaReceitaResponse.builder()
                    .id(c.getId().toString())
                    .nome(c.getName())
                    .cor(c.getColor())
                    .build())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<BuscaIngredienteResponse> buscarIngredientes(
      String query, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    String q = (query == null || query.isBlank()) ? "" : query.trim();
    log.debug("[INGREDIENTE] Busca — query='{}' workspaceId={}", q, workspaceId);

    return ingredientRepository.searchByWorkspaceAndName(workspaceId, q).stream()
        .map(
            i ->
                BuscaIngredienteResponse.builder()
                    .id(i.getId().toString())
                    .nome(i.getName())
                    .marca(i.getBrand())
                    .unidadeId(i.getUnit() != null ? i.getUnit().getId().toString() : null)
                    .unidadeSimbolo(i.getUnit() != null ? i.getUnit().getSymbol() : null)
                    .unidadeNome(i.getUnit() != null ? i.getUnit().getName() : null)
                    .custoPorUnidade(i.getCostPerUnit())
                    .build())
        .toList();
  }

  // =========================================================================
  // Persistência de ingredientes
  // =========================================================================

  /**
   * Persiste a lista de ingredientes de uma receita e retorna as linhas montadas já com os custos
   * calculados por linha.
   */
  private List<IngredienteReceitaResponse> salvarIngredientes(
      ProductJpaEntity product, List<IngredienteReceitaRequest> items, UUID workspaceId) {

    if (items == null || items.isEmpty()) {
      return List.of();
    }

    List<IngredienteReceitaResponse> responses = new ArrayList<>();
    for (IngredienteReceitaRequest item : items) {

      IngredientJpaEntity ingredient =
          ingredientRepository
              .findByIdAndWorkspaceIdAndDeletedAtIsNull(item.getIngredienteId(), workspaceId)
              .orElseThrow(
                  () ->
                      new ReceitaException(
                          "Ingrediente não encontrado ou não pertence a este workspace: "
                              + item.getIngredienteId()));

      UnitJpaEntity unit = buscarUnidade(item.getUnidadeId());

      BigDecimal fator = resolverFatorConversao(unit, ingredient.getUnit());
      BigDecimal custoLinha =
          ingredient
              .getCostPerUnit()
              .multiply(item.getQuantidade().multiply(fator))
              .setScale(4, RoundingMode.HALF_UP);

      ProductIngredientJpaEntity pi =
          ProductIngredientJpaEntity.builder()
              .product(product)
              .ingredient(ingredient)
              .quantity(item.getQuantidade())
              .unit(unit)
              .notes(item.getNotas())
              .build();
      pi = productIngredientRepository.save(pi);

      responses.add(
          IngredienteReceitaResponse.builder()
              .id(pi.getId().toString())
              .ingredienteId(ingredient.getId().toString())
              .ingredienteNome(ingredient.getName())
              .marca(ingredient.getBrand())
              .quantidade(item.getQuantidade())
              .unidadeId(unit.getId().toString())
              .unidadeSimbolo(unit.getSymbol())
              .unidadeNome(unit.getName())
              .custoCalculado(custoLinha)
              .custoPorUnidade(ingredient.getCostPerUnit())
              .notas(pi.getNotes())
              .build());
    }
    return responses;
  }

  /**
   * Persiste as receitas usadas como ingrediente e retorna as linhas montadas com custo calculado.
   *
   * <p>O custo por unidade da sub-receita é: {@code precoFinal / rendimentoQuantidade}, ou seja, o
   * preço de venda por unidade definido pelo confeiteiro.
   */
  private List<ReceitaComoIngredienteResponse> salvarReceitasComoIngredientes(
      ProductJpaEntity product, List<ReceitaComoIngredienteRequest> items, UUID workspaceId) {

    if (items == null || items.isEmpty()) {
      return List.of();
    }

    List<ReceitaComoIngredienteResponse> responses = new ArrayList<>();
    for (ReceitaComoIngredienteRequest item : items) {
      if (item.getReceitaId().equals(product.getId())) {
        throw new ReceitaException("Uma receita não pode usar a si mesma como ingrediente.");
      }

      ProductJpaEntity sub =
          productRepository
              .findByIdAndWorkspaceIdAndDeletedAtIsNull(item.getReceitaId(), workspaceId)
              .orElseThrow(
                  () ->
                      new ReceitaException(
                          "Receita-ingrediente não encontrada ou não pertence a este workspace: "
                              + item.getReceitaId()));

      BigDecimal precoPorUnidade = calcularPrecoPorUnidade(sub);
      BigDecimal custoLinha =
          precoPorUnidade.multiply(item.getQuantidade()).setScale(4, RoundingMode.HALF_UP);

      ProductRecipeIngredientJpaEntity ri =
          ProductRecipeIngredientJpaEntity.builder()
              .product(product)
              .subReceita(sub)
              .quantidade(item.getQuantidade())
              .notas(item.getNotas())
              .build();
      ri = productRecipeIngredientRepository.save(ri);

      UnitJpaEntity yUnit = sub.getYieldUnit();
      responses.add(
          ReceitaComoIngredienteResponse.builder()
              .id(ri.getId().toString())
              .receitaId(sub.getId().toString())
              .nome(sub.getName())
              .rendimentoQuantidade(sub.getYieldQuantity())
              .rendimentoUnidadeId(yUnit != null ? yUnit.getId().toString() : null)
              .rendimentoUnidadeSimbolo(yUnit != null ? yUnit.getSymbol() : null)
              .rendimentoUnidadeNome(yUnit != null ? yUnit.getName() : null)
              .quantidade(item.getQuantidade())
              .precoPorUnidade(precoPorUnidade.setScale(4, RoundingMode.HALF_UP))
              .custoCalculado(custoLinha)
              .notas(ri.getNotas())
              .build());
    }
    return responses;
  }

  /**
   * Carrega os ingredientes de uma receita existente a partir do banco (com FETCH JOINs para evitar
   * N+1) e monta as responses com custo calculado.
   */
  private List<IngredienteReceitaResponse> carregarIngredientesResponse(ProductJpaEntity product) {
    return productIngredientRepository.findAllByProductIdWithDetails(product.getId()).stream()
        .map(
            pi -> {
              IngredientJpaEntity ing = pi.getIngredient();
              UnitJpaEntity recipeUnit = pi.getUnit();
              BigDecimal fator = resolverFatorConversao(recipeUnit, ing.getUnit());
              BigDecimal custo =
                  ing.getCostPerUnit()
                      .multiply(pi.getQuantity().multiply(fator))
                      .setScale(4, RoundingMode.HALF_UP);

              return IngredienteReceitaResponse.builder()
                  .id(pi.getId().toString())
                  .ingredienteId(ing.getId().toString())
                  .ingredienteNome(ing.getName())
                  .marca(ing.getBrand())
                  .quantidade(pi.getQuantity())
                  .unidadeId(recipeUnit.getId().toString())
                  .unidadeSimbolo(recipeUnit.getSymbol())
                  .unidadeNome(recipeUnit.getName())
                  .custoCalculado(custo)
                  .custoPorUnidade(ing.getCostPerUnit())
                  .notas(pi.getNotes())
                  .build();
            })
        .toList();
  }

  /** Carrega as receitas-como-ingrediente de uma receita existente com FETCH JOINs. */
  private List<ReceitaComoIngredienteResponse> carregarReceitasComoIngredientesResponse(
      ProductJpaEntity product) {
    List<ReceitaComoIngredienteResponse> result = new ArrayList<>();
    for (ProductRecipeIngredientJpaEntity ri :
        productRecipeIngredientRepository.findAllByProductIdWithDetails(product.getId())) {
      ProductJpaEntity sub = ri.getSubReceita();
      BigDecimal precoPorUnidade = calcularPrecoPorUnidade(sub);
      BigDecimal custoLinha =
          precoPorUnidade.multiply(ri.getQuantidade()).setScale(4, RoundingMode.HALF_UP);
      UnitJpaEntity yUnit = sub.getYieldUnit();
      result.add(
          ReceitaComoIngredienteResponse.builder()
              .id(ri.getId().toString())
              .receitaId(sub.getId().toString())
              .nome(sub.getName())
              .rendimentoQuantidade(sub.getYieldQuantity())
              .rendimentoUnidadeId(yUnit != null ? yUnit.getId().toString() : null)
              .rendimentoUnidadeSimbolo(yUnit != null ? yUnit.getSymbol() : null)
              .rendimentoUnidadeNome(yUnit != null ? yUnit.getName() : null)
              .quantidade(ri.getQuantidade())
              .precoPorUnidade(precoPorUnidade.setScale(4, RoundingMode.HALF_UP))
              .custoCalculado(custoLinha)
              .notas(ri.getNotas())
              .build());
    }
    return result;
  }

  /**
   * Preço de venda por unidade de uma receita = {@code precoFinal / rendimentoQuantidade}. Retorna
   * zero quando o rendimento for nulo ou zero.
   */
  private BigDecimal calcularPrecoPorUnidade(ProductJpaEntity receita) {
    BigDecimal preco = coalesce(receita.getSellingPrice(), BigDecimal.ZERO);
    BigDecimal rendimento = receita.getYieldQuantity();
    if (rendimento == null || rendimento.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return preco.divide(rendimento, 6, RoundingMode.HALF_UP);
  }

  /**
   * Persiste os custos pré-calculados (vindos do frontend via {@code POST /receitas/calcular})
   * diretamente na entidade, sem recalcular. O servidor é a fonte de verdade dos metadados da
   * receita; os valores financeiros são confiados ao resultado do endpoint de cálculo.
   */
  private void aplicarCustos(ProductJpaEntity product, CriarReceitaRequest request) {
    BigDecimal zero = BigDecimal.ZERO;
    // Custos do lote
    product.setIngredientCost(coalesce(request.getCustoIngredientes(), zero));
    product.setLaborCost(coalesce(request.getCustoMaoDeObra(), zero));
    product.setFixedCosts(coalesce(request.getCustosFixos(), zero));
    product.setCalculatedCost(coalesce(request.getCustoCalculado(), zero));
    product.setSuggestedPrice(coalesce(request.getPrecoSugerido(), zero));
    // Parâmetros de referência
    product.setLaborCostPerHour(coalesce(request.getMaoDeObraValorHora(), zero));
    product.setFixedCostsValue(coalesce(request.getCustosFixosValor(), zero));
    product.setFixedCostsType(coalesce(request.getCustosFixosTipo(), CUSTOS_FIXOS_TIPO_PADRAO));
    product.setDesiredMargin(coalesce(request.getMargemDesejada(), MARGEM_PADRAO));
    // Valores calculados aprimorados (por unidade, por porção, por g/ml)
    // Prefer client-provided values; fall back to server-side computation when absent.
    BigDecimal calculatedCost = coalesce(request.getCustoCalculado(), zero);
    BigDecimal yieldQty =
        (product.getYieldQuantity() != null
                && product.getYieldQuantity().compareTo(BigDecimal.ZERO) > 0)
            ? product.getYieldQuantity()
            : null;

    BigDecimal unitCost =
        request.getCustoTotalPorUnidade() != null
            ? request.getCustoTotalPorUnidade()
            : (yieldQty != null
                ? calculatedCost.divide(yieldQty, 4, java.math.RoundingMode.HALF_UP)
                : null);

    BigDecimal suggestedUnitPrice =
        request.getPrecoSugeridoPorUnidade() != null
            ? request.getPrecoSugeridoPorUnidade()
            : (yieldQty != null && product.getSuggestedPrice() != null
                ? product.getSuggestedPrice().divide(yieldQty, 4, java.math.RoundingMode.HALF_UP)
                : null);

    product.setUnitCost(unitCost);
    product.setSuggestedUnitPrice(suggestedUnitPrice);
    product.setPortionCount(request.getNumeroPorcoesUnidades());
    product.setCostPerGramMl(request.getCustoPorGramaOuMl());
    product.setPricePerGramMl(request.getPrecoPorGramaOuMl());
    product.setCostPerPortion(request.getCustoPorPorcaoOuUnidade());
    product.setPricePerPortion(request.getPrecoPorPorcaoOuUnidade());
  }

  // =========================================================================
  // Fórmulas financeiras
  // =========================================================================

  /** precoSugerido = custoTotal / (1 - margem/100) */
  private BigDecimal calcularPrecoSugerido(BigDecimal custoTotal, BigDecimal margemPercent) {
    if (custoTotal == null || custoTotal.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal divisor =
        BigDecimal.ONE.subtract(
            margemPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
      return custoTotal;
    }
    return custoTotal.divide(divisor, 2, RoundingMode.HALF_UP);
  }

  /** margem (%) = ((preco - custo) / preco) × 100 */
  private BigDecimal calcularMargemReal(BigDecimal custo, BigDecimal preco) {
    if (preco == null || preco.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return preco
        .subtract(custo)
        .divide(preco, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
  }

  /** valor × percentual / 100 */
  private BigDecimal percentual(BigDecimal valor, BigDecimal pct) {
    return valor.multiply(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
  }

  /**
   * Resolve o fator de conversão entre a unidade da receita e a unidade base do ingrediente.
   * Retorna 1 se as unidades forem iguais ou se não houver conversão cadastrada.
   */
  private BigDecimal resolverFatorConversao(
      UnitJpaEntity unidadeReceita, UnitJpaEntity unidadeBase) {
    if (unidadeReceita == null || unidadeBase == null) {
      return BigDecimal.ONE;
    }
    if (unidadeReceita.getId().equals(unidadeBase.getId())) {
      return BigDecimal.ONE;
    }
    Optional<UnitConversionJpaEntity> conv =
        unitConversionRepository.findByFromUnitIdAndToUnitId(
            unidadeReceita.getId(), unidadeBase.getId());
    return conv.map(UnitConversionJpaEntity::getFactor).orElse(BigDecimal.ONE);
  }

  // =========================================================================
  // Montagem da resposta
  // =========================================================================

  private ReceitaResponse montarResponse(
      ProductJpaEntity product,
      List<IngredienteReceitaResponse> ingredientes,
      List<ReceitaComoIngredienteResponse> receitasComoIngredientes) {
    ProductCategoryJpaEntity cat = product.getCategory();
    UnitJpaEntity yUnit = product.getYieldUnit();
    UnitJpaEntity pesUnit = product.getUnitWeightUnit();

    // Custo de ingredientes = calculatedCost - laborCost - fixedCosts
    BigDecimal custoMaoDeObra = coalesce(product.getLaborCost(), BigDecimal.ZERO);
    BigDecimal custoFixos = coalesce(product.getFixedCosts(), BigDecimal.ZERO);
    BigDecimal custoTotal = coalesce(product.getCalculatedCost(), BigDecimal.ZERO);
    // Custo de ingredientes: lê do DB (campo direto), fallback: calculatedCost - labor - fixed
    BigDecimal custoIngredientesStored = product.getIngredientCost();
    BigDecimal custoIngredientes =
        custoIngredientesStored != null
            ? custoIngredientesStored
            : custoTotal.subtract(custoMaoDeObra).subtract(custoFixos);

    // Margem real: ((precoFinal - custoTotalPorUnidade) / precoFinal) × 100
    BigDecimal yieldQty =
        (product.getYieldQuantity() != null
                && product.getYieldQuantity().compareTo(BigDecimal.ZERO) > 0)
            ? product.getYieldQuantity()
            : BigDecimal.ONE;
    BigDecimal custoTotalPorUnidade = custoTotal.divide(yieldQty, 4, RoundingMode.HALF_UP);
    BigDecimal margem = calcularMargemReal(custoTotalPorUnidade, product.getSellingPrice());

    ResultadoRendimentoAprimorado aprimorado =
        calcularRendimentoAprimorado(
            product.getYieldQuantity(),
            custoTotal,
            product.getSuggestedPrice() != null ? product.getSuggestedPrice() : BigDecimal.ZERO,
            yUnit != null ? yUnit.getId() : null,
            product.getUnitWeight(),
            pesUnit != null ? pesUnit.getId() : null);

    // Para os campos aprimorados: prefere o valor persistido no DB (enviado pelo frontend
    // via /calcular), com fallback para o recálculo em tempo real.
    BigDecimal numeroPorcoesUnidades =
        product.getPortionCount() != null
            ? product.getPortionCount()
            : aprimorado.numeroPorcoesUnidades();
    BigDecimal custoPorGramaOuMl =
        product.getCostPerGramMl() != null
            ? product.getCostPerGramMl()
            : aprimorado.custoPorGramaOuMl();
    BigDecimal precoPorGramaOuMl =
        product.getPricePerGramMl() != null
            ? product.getPricePerGramMl()
            : aprimorado.precoPorGramaOuMl();
    BigDecimal custoPorPorcaoOuUnidade =
        product.getCostPerPortion() != null
            ? product.getCostPerPortion()
            : aprimorado.custoPorPorcaoOuUnidade();
    BigDecimal precoPorPorcaoOuUnidade =
        product.getPricePerPortion() != null
            ? product.getPricePerPortion()
            : aprimorado.precoPorPorcaoOuUnidade();

    return ReceitaResponse.builder()
        .id(product.getId().toString())
        .nome(product.getName())
        .descricao(product.getDescription())
        .categoriaId(cat != null ? cat.getId().toString() : null)
        .categoriaNome(cat != null ? cat.getName() : null)
        .rendimentoQuantidade(product.getYieldQuantity())
        .rendimentoUnidadeId(yUnit != null ? yUnit.getId().toString() : null)
        .rendimentoUnidadeSimbolo(yUnit != null ? yUnit.getSymbol() : null)
        .rendimentoUnidadeNome(yUnit != null ? yUnit.getName() : null)
        .tempoPreparoMinutos(product.getPrepTimeMinutes())
        .ingredientes(ingredientes)
        .receitasComoIngredientes(receitasComoIngredientes)
        .notas(product.getNotes())
        .precoFinal(product.getSellingPrice())
        .precoSugerido(product.getSuggestedPrice())
        .custoIngredientes(custoIngredientes.setScale(2, RoundingMode.HALF_UP))
        .custoMaoDeObra(custoMaoDeObra.setScale(2, RoundingMode.HALF_UP))
        .custosFixos(custoFixos.setScale(2, RoundingMode.HALF_UP))
        .custoCalculado(custoTotal.setScale(2, RoundingMode.HALF_UP))
        .margem(margem.setScale(1, RoundingMode.HALF_UP))
        .maoDeObraValorHora(coalesce(product.getLaborCostPerHour(), BigDecimal.ZERO))
        .custosFixosValor(coalesce(product.getFixedCostsValue(), BigDecimal.ZERO))
        .custosFixosTipo(coalesce(product.getFixedCostsType(), CUSTOS_FIXOS_TIPO_PADRAO))
        .margemDesejada(coalesce(product.getDesiredMargin(), MARGEM_PADRAO))
        .status(product.getStatus())
        .ativo(product.isActive())
        .criadoEm(product.getCreatedAt())
        .atualizadoEm(product.getUpdatedAt())
        .pesoPorUnidade(product.getUnitWeight())
        .pesoPorUnidadeUnidadeId(pesUnit != null ? pesUnit.getId().toString() : null)
        .pesoPorUnidadeUnidadeSimbolo(pesUnit != null ? pesUnit.getSymbol() : null)
        .numeroPorcoesUnidades(
            numeroPorcoesUnidades != null
                ? numeroPorcoesUnidades.setScale(2, RoundingMode.HALF_UP)
                : null)
        .custoPorGramaOuMl(
            custoPorGramaOuMl != null ? custoPorGramaOuMl.setScale(6, RoundingMode.HALF_UP) : null)
        .precoPorGramaOuMl(
            precoPorGramaOuMl != null ? precoPorGramaOuMl.setScale(6, RoundingMode.HALF_UP) : null)
        .custoPorPorcaoOuUnidade(
            custoPorPorcaoOuUnidade != null
                ? custoPorPorcaoOuUnidade.setScale(2, RoundingMode.HALF_UP)
                : null)
        .precoPorPorcaoOuUnidade(
            precoPorPorcaoOuUnidade != null
                ? precoPorPorcaoOuUnidade.setScale(2, RoundingMode.HALF_UP)
                : null)
        .build();
  }

  /**
   * Resultado dos cálculos aprimorados de rendimento. Todos os campos são {@code null} quando não
   * aplicável (ex: unidade do tipo {@code unit} sem pesoPorUnidade).
   */
  private record ResultadoRendimentoAprimorado(
      BigDecimal numeroPorcoesUnidades,
      BigDecimal custoPorGramaOuMl,
      BigDecimal precoPorGramaOuMl,
      BigDecimal custoPorPorcaoOuUnidade,
      BigDecimal precoPorPorcaoOuUnidade) {}

  /**
   * Centraliza o cálculo de métricas por grama/ml e por unidade/porção.
   *
   * <ul>
   *   <li>Se a unidade de rendimento for {@code weight} ou {@code volume}: calcula {@code
   *       custoPorGramaOuMl} e {@code precoPorGramaOuMl} convertendo o rendimento para a unidade
   *       base do tipo.
   *   <li>Se {@code pesoPorUnidade} e {@code pesoPorUnidadeUnidadeId} forem fornecidos: calcula
   *       {@code numeroPorcoesUnidades}, {@code custoPorPorcaoOuUnidade} e {@code
   *       precoPorPorcaoOuUnidade}.
   * </ul>
   */
  private ResultadoRendimentoAprimorado calcularRendimentoAprimorado(
      BigDecimal rendimentoQuantidade,
      BigDecimal custoTotal,
      BigDecimal precoSugerido,
      UUID rendimentoUnidadeId,
      BigDecimal pesoPorUnidade,
      UUID pesoPorUnidadeUnidadeId) {

    if (rendimentoUnidadeId == null
        || rendimentoQuantidade == null
        || rendimentoQuantidade.compareTo(BigDecimal.ZERO) <= 0) {
      return new ResultadoRendimentoAprimorado(null, null, null, null, null);
    }

    UnitJpaEntity rendUnit =
        unitRepository.findByIdAndDeletedAtIsNull(rendimentoUnidadeId).orElse(null);
    if (rendUnit == null) {
      return new ResultadoRendimentoAprimorado(null, null, null, null, null);
    }

    if (rendUnit.getType() != UnitType.weight && rendUnit.getType() != UnitType.volume) {
      return new ResultadoRendimentoAprimorado(null, null, null, null, null);
    }

    // Custo/preço por grama ou ml
    BigDecimal custoPorGramaOuMl = null;
    BigDecimal precoPorGramaOuMl = null;

    UnitJpaEntity baseUnit =
        unitRepository
            .findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(rendUnit.getType())
            .orElse(null);
    if (baseUnit != null) {
      BigDecimal fatorParaBase = resolverFatorConversao(rendUnit, baseUnit);
      BigDecimal rendimentoEmBase = rendimentoQuantidade.multiply(fatorParaBase);
      if (rendimentoEmBase.compareTo(BigDecimal.ZERO) > 0) {
        custoPorGramaOuMl = custoTotal.divide(rendimentoEmBase, 6, RoundingMode.HALF_UP);
        precoPorGramaOuMl = precoSugerido.divide(rendimentoEmBase, 6, RoundingMode.HALF_UP);
      }
    }

    // Número de unidades/porções e custo/preço por unidade/porção
    BigDecimal numeroPorcoesUnidades = null;
    BigDecimal custoPorPorcaoOuUnidade = null;
    BigDecimal precoPorPorcaoOuUnidade = null;

    if (pesoPorUnidade != null
        && pesoPorUnidadeUnidadeId != null
        && pesoPorUnidade.compareTo(BigDecimal.ZERO) > 0) {
      UnitJpaEntity pesUnit =
          unitRepository.findByIdAndDeletedAtIsNull(pesoPorUnidadeUnidadeId).orElse(null);
      if (pesUnit != null) {
        // Converter pesoPorUnidade para a unidade de rendimento
        BigDecimal fatorPesParaRendUnit = resolverFatorConversao(pesUnit, rendUnit);
        BigDecimal pesEmRendUnit = pesoPorUnidade.multiply(fatorPesParaRendUnit);
        if (pesEmRendUnit.compareTo(BigDecimal.ZERO) > 0) {
          numeroPorcoesUnidades =
              rendimentoQuantidade.divide(pesEmRendUnit, 4, RoundingMode.HALF_UP);
          if (numeroPorcoesUnidades.compareTo(BigDecimal.ZERO) > 0) {
            custoPorPorcaoOuUnidade =
                custoTotal.divide(numeroPorcoesUnidades, 4, RoundingMode.HALF_UP);
            precoPorPorcaoOuUnidade =
                precoSugerido.divide(numeroPorcoesUnidades, 4, RoundingMode.HALF_UP);
          }
        }
      }
    }

    return new ResultadoRendimentoAprimorado(
        numeroPorcoesUnidades,
        custoPorGramaOuMl,
        precoPorGramaOuMl,
        custoPorPorcaoOuUnidade,
        precoPorPorcaoOuUnidade);
  }

  // =========================================================================
  // Validações e helpers
  // =========================================================================

  private UUID resolverWorkspaceId(UsuarioAutenticado usuario) {
    String wid = usuario.workspaceId();
    if (wid == null || wid.isBlank()) {
      throw new ReceitaException(
          "Usuário não está vinculado a nenhum workspace. Conclua o onboarding.");
    }
    return UUID.fromString(wid);
  }

  /**
   * Garante que não existam duas receitas com o mesmo nome no workspace. Quando {@code idExcluir} é
   * fornecido (operação de update), o produto com aquele ID é excluído da checagem.
   */
  private void validarNomeUnico(String nome, UUID workspaceId, UUID idExcluir) {
    boolean existe =
        productRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
            workspaceId, nome.trim());
    if (!existe) return;

    if (idExcluir != null) {
      Optional<ProductJpaEntity> existente =
          productRepository.findByIdAndDeletedAtIsNull(idExcluir);
      if (existente.isPresent() && existente.get().getName().equalsIgnoreCase(nome.trim())) {
        return; // mesmo produto — update sem mudar nome
      }
    }
    throw new ReceitaException(
        "Já existe uma receita com o nome '" + nome.trim() + "' neste workspace.");
  }

  private WorkspaceJpaEntity buscarWorkspace(UUID workspaceId) {
    return workspaceRepository
        .findByIdAndDeletedAtIsNull(workspaceId)
        .orElseThrow(() -> new ReceitaException("Workspace não encontrado."));
  }

  private ProductCategoryJpaEntity buscarCategoria(UUID categoriaId) {
    return productCategoryRepository
        .findByIdAndDeletedAtIsNull(categoriaId)
        .orElseThrow(() -> new ReceitaException("Categoria não encontrada: " + categoriaId));
  }

  private UnitJpaEntity buscarUnidade(UUID unidadeId) {
    return unitRepository
        .findByIdAndDeletedAtIsNull(unidadeId)
        .orElseThrow(() -> new ReceitaException("Unidade não encontrada: " + unidadeId));
  }

  private ProductJpaEntity buscarReceita(UUID id, UUID workspaceId) {
    return productRepository
        .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
        .orElseThrow(() -> new ReceitaException("Receita não encontrada: " + id));
  }

  private UserJpaEntity buscarUsuario(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ReceitaException("Usuário não encontrado: " + userId));
  }

  private RecipeStatus resolverStatus(String status) {
    if (status == null || status.isBlank()) return RecipeStatus.rascunho;
    try {
      return RecipeStatus.valueOf(status.toLowerCase());
    } catch (IllegalArgumentException e) {
      throw new ReceitaException(
          "Status inválido: '" + status + "'. Use 'rascunho' ou 'publicada'.");
    }
  }

  private <T> T coalesce(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  // =========================================================================
  // Audit Log
  // =========================================================================

  private void registrarAuditLog(
      WorkspaceJpaEntity workspace,
      String entityName,
      UUID entityId,
      AuditOperation operation,
      String dataAfter,
      UserJpaEntity performedBy) {
    auditLogRepository.save(
        AuditLogJpaEntity.builder()
            .workspace(workspace)
            .entityName(entityName)
            .entityId(entityId)
            .operation(operation)
            .dataAfter(dataAfter)
            .performedBy(performedBy)
            .build());
  }
}
