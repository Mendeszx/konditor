package com.api.konditor.domain.usecase.impl;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CriarIngredienteRequest;
import com.api.konditor.app.controller.response.CategoriaIngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteCardResponse;
import com.api.konditor.app.controller.response.IngredienteResponse;
import com.api.konditor.app.controller.response.IngredienteResumoResponse;
import com.api.konditor.app.controller.response.PaginaResponse;
import com.api.konditor.app.exception.IngredienteException;
import com.api.konditor.domain.enuns.AuditOperation;
import com.api.konditor.domain.usecase.IngredienteUseCase;
import com.api.konditor.infra.jpa.entity.AuditLogJpaEntity;
import com.api.konditor.infra.jpa.entity.IngredientCategoryJpaEntity;
import com.api.konditor.infra.jpa.entity.IngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.IngredientPriceHistoryJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductIngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitConversionJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import com.api.konditor.infra.jpa.entity.WorkspaceJpaEntity;
import com.api.konditor.infra.jpa.repository.AuditLogJpaRepository;
import com.api.konditor.infra.jpa.repository.IngredientCategoryJpaRepository;
import com.api.konditor.infra.jpa.repository.IngredientJpaRepository;
import com.api.konditor.infra.jpa.repository.IngredientPriceHistoryJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductIngredientJpaRepository;
import com.api.konditor.infra.jpa.repository.UnitConversionJpaRepository;
import com.api.konditor.infra.jpa.repository.UnitJpaRepository;
import com.api.konditor.infra.jpa.repository.UserJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredienteUseCaseImpl implements IngredienteUseCase {

  private static final BigDecimal MARGEM_PADRAO = new BigDecimal("30.00");

  private final IngredientJpaRepository ingredientRepository;
  private final IngredientCategoryJpaRepository categoryRepository;
  private final IngredientPriceHistoryJpaRepository priceHistoryRepository;
  private final UnitJpaRepository unitRepository;
  private final WorkspaceJpaRepository workspaceRepository;
  private final UserJpaRepository userRepository;
  private final ProductIngredientJpaRepository productIngredientRepository;
  private final UnitConversionJpaRepository unitConversionRepository;
  private final AuditLogJpaRepository auditLogRepository;

  // =========================================================================
  // Casos de uso
  // =========================================================================

  @Override
  @Transactional(readOnly = true)
  public PaginaResponse<IngredienteCardResponse> listar(
      UsuarioAutenticado usuario, UUID categoriaId, int pagina, int tamanho) {

    UUID workspaceId = resolverWorkspaceId(usuario);
    log.debug(
        "[INGREDIENTE] Listando — workspaceId={} categoriaId={} pagina={} tamanho={}",
        workspaceId,
        categoriaId,
        pagina,
        tamanho);

    PageRequest pageRequest = PageRequest.of(pagina, tamanho);

    Page<IngredientJpaEntity> page =
        categoriaId != null
            ? ingredientRepository.findPageByWorkspaceIdAndCategoryId(
                workspaceId, categoriaId, pageRequest)
            : ingredientRepository.findPageByWorkspaceId(workspaceId, pageRequest);

    List<UUID> ids = page.getContent().stream().map(IngredientJpaEntity::getId).toList();

    Map<UUID, IngredientPriceHistoryJpaEntity> historicoPorId =
        ids.isEmpty()
            ? Map.of()
            : priceHistoryRepository.findMostRecentByIngredientIds(ids).stream()
                .collect(Collectors.toMap(h -> h.getIngredient().getId(), h -> h, (a, b) -> a));

    List<IngredienteCardResponse> cards =
        page.getContent().stream().map(i -> toCard(i, historicoPorId.get(i.getId()))).toList();

    return new PaginaResponse<>(
        cards, pagina, tamanho, page.getTotalElements(), page.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public IngredienteResumoResponse resumo(UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.debug("[INGREDIENTE] Resumo — workspaceId={}", workspaceId);
    long total = ingredientRepository.countByWorkspaceIdAndDeletedAtIsNull(workspaceId);
    return new IngredienteResumoResponse(total);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoriaIngredienteResponse> listarCategorias(UsuarioAutenticado usuario) {
    log.debug("[INGREDIENTE] Listando categorias globais — userId={}", usuario.id());

    return categoryRepository.findAllByDeletedAtIsNull(Sort.by("name").ascending()).stream()
        .map(
            c ->
                CategoriaIngredienteResponse.builder()
                    .id(c.getId().toString())
                    .nome(c.getName())
                    .cor(c.getColor())
                    .build())
        .toList();
  }

  @Override
  @Transactional
  public IngredienteResponse criar(CriarIngredienteRequest request, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.info(
        "[INGREDIENTE] Criando ingrediente '{}' — workspaceId={}", request.getNome(), workspaceId);

    validarNomeUnico(request.getNome(), workspaceId, null);
    validarCodigoUnico(request.getCodigo(), workspaceId, null);

    WorkspaceJpaEntity workspace = buscarWorkspace(workspaceId);
    UnitJpaEntity unit = buscarUnidade(request.getUnidadeId());
    IngredientCategoryJpaEntity category =
        request.getCategoriaId() != null ? buscarCategoria(request.getCategoriaId()) : null;
    UserJpaEntity createdBy = buscarUsuario(UUID.fromString(usuario.id()));

    IngredientJpaEntity entity =
        IngredientJpaEntity.builder()
            .workspace(workspace)
            .name(request.getNome().trim())
            .code(request.getCodigo())
            .description(request.getDescricao())
            .brand(request.getMarca())
            .category(category)
            .unit(unit)
            .costPerUnit(request.getPrecoPorUnidade())
            .notes(request.getNotas())
            .createdBy(createdBy)
            .build();

    entity = ingredientRepository.save(entity);
    log.info(
        "[INGREDIENTE] Ingrediente criado id={} — workspaceId={}", entity.getId(), workspaceId);

    registrarAuditLog(
        entity.getWorkspace(),
        "Ingrediente",
        entity.getId(),
        AuditOperation.CREATE,
        "{\"nome\":\"" + entity.getName() + "\",\"unidade\":\"" + unit.getSymbol() + "\"}",
        createdBy);

    return montarResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public IngredienteResponse buscarPorId(UUID id, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.debug("[INGREDIENTE] Buscando por id={} — workspaceId={}", id, workspaceId);
    IngredientJpaEntity entity = buscarIngrediente(id, workspaceId);
    return montarResponse(entity);
  }

  @Override
  @Transactional
  public IngredienteResponse atualizar(
      UUID id, CriarIngredienteRequest request, UsuarioAutenticado usuario) {
    UUID workspaceId = resolverWorkspaceId(usuario);
    log.info("[INGREDIENTE] Atualizando ingrediente id={} — workspaceId={}", id, workspaceId);

    IngredientJpaEntity entity = buscarIngrediente(id, workspaceId);
    validarNomeUnico(request.getNome(), workspaceId, id);
    validarCodigoUnico(request.getCodigo(), workspaceId, id);

    UnitJpaEntity unit = buscarUnidade(request.getUnidadeId());
    IngredientCategoryJpaEntity category =
        request.getCategoriaId() != null ? buscarCategoria(request.getCategoriaId()) : null;
    UserJpaEntity updatedBy = buscarUsuario(UUID.fromString(usuario.id()));

    entity.setName(request.getNome().trim());
    entity.setCode(request.getCodigo());
    entity.setDescription(request.getDescricao());
    entity.setBrand(request.getMarca());
    entity.setCategory(category);
    entity.setUnit(unit);
    entity.setCostPerUnit(request.getPrecoPorUnidade());
    entity.setNotes(request.getNotas());
    entity.setUpdatedBy(updatedBy);

    recalcularReceitasAfetadas(entity);
    log.info("[INGREDIENTE] Ingrediente id={} atualizado — workspaceId={}", id, workspaceId);

    registrarAuditLog(
        entity.getWorkspace(),
        "Ingrediente",
        entity.getId(),
        AuditOperation.UPDATE,
        "{\"nome\":\"" + entity.getName() + "\"}",
        updatedBy);

    return montarResponse(entity);
  }

  // =========================================================================
  // Recálculo em cascata
  // =========================================================================

  /**
   * Recalcula {@code calculatedCost} e {@code suggestedPrice} de todas as receitas que contêm o
   * ingrediente atualizado.
   *
   * <p>Fluxo:
   *
   * <ol>
   *   <li>Busca todas as linhas de receita que referenciam este ingrediente.
   *   <li>Agrupa por produto (receita).
   *   <li>Para cada produto, carrega TODAS as suas linhas e recalcula o custo total.
   *   <li>Atualiza {@code calculatedCost} e {@code suggestedPrice} via dirty-checking JPA.
   * </ol>
   */
  private void recalcularReceitasAfetadas(IngredientJpaEntity ingrediente) {
    List<ProductIngredientJpaEntity> linhasAfetadas =
        productIngredientRepository.findAllByIngredientIdWithDetails(ingrediente.getId());

    if (linhasAfetadas.isEmpty()) {
      log.debug("[INGREDIENTE] Nenhuma receita usa o ingrediente id={}", ingrediente.getId());
      return;
    }

    // Distinct products via Map (preserva a entidade gerenciada)
    Map<UUID, ProductJpaEntity> produtosAfetados =
        linhasAfetadas.stream()
            .collect(
                Collectors.toMap(
                    pi -> pi.getProduct().getId(),
                    ProductIngredientJpaEntity::getProduct,
                    (a, b) -> a));

    for (ProductJpaEntity produto : produtosAfetados.values()) {
      // Carrega TODAS as linhas do produto (não só as do ingrediente alterado)
      List<ProductIngredientJpaEntity> todasLinhas =
          productIngredientRepository.findAllByProductIdWithDetails(produto.getId());

      BigDecimal custoTotal =
          todasLinhas.stream()
              .map(
                  pi -> {
                    IngredientJpaEntity ing = pi.getIngredient();
                    BigDecimal fator = resolverFatorConversao(pi.getUnit(), ing.getUnit());
                    return ing.getCostPerUnit()
                        .multiply(pi.getQuantity().multiply(fator))
                        .setScale(4, RoundingMode.HALF_UP);
                  })
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal divisor =
          BigDecimal.ONE.subtract(
              MARGEM_PADRAO.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
      BigDecimal precoSugerido =
          custoTotal.compareTo(BigDecimal.ZERO) == 0
              ? BigDecimal.ZERO
              : custoTotal.divide(divisor, 2, RoundingMode.HALF_UP);

      produto.setCalculatedCost(custoTotal.setScale(4, RoundingMode.HALF_UP));
      produto.setSuggestedPrice(precoSugerido.setScale(2, RoundingMode.HALF_UP));
    }

    log.info(
        "[INGREDIENTE] {} receita(s) recalculadas após atualização do ingrediente id={}",
        produtosAfetados.size(),
        ingrediente.getId());
  }

  /**
   * Resolve o fator de conversão entre a unidade usada na receita e a unidade base do ingrediente.
   * Retorna 1 se as unidades forem iguais ou se não houver conversão cadastrada.
   */
  private BigDecimal resolverFatorConversao(
      UnitJpaEntity unidadeReceita, UnitJpaEntity unidadeBase) {
    if (unidadeReceita == null || unidadeBase == null) return BigDecimal.ONE;
    if (unidadeReceita.getId().equals(unidadeBase.getId())) return BigDecimal.ONE;
    return unitConversionRepository
        .findByFromUnitIdAndToUnitId(unidadeReceita.getId(), unidadeBase.getId())
        .map(UnitConversionJpaEntity::getFactor)
        .orElse(BigDecimal.ONE);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private IngredienteCardResponse toCard(
      IngredientJpaEntity i, IngredientPriceHistoryJpaEntity historico) {

    BigDecimal variacao = null;
    if (historico != null && historico.getOldPrice().compareTo(BigDecimal.ZERO) != 0) {
      variacao =
          historico
              .getNewPrice()
              .subtract(historico.getOldPrice())
              .divide(historico.getOldPrice(), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
    }

    IngredientCategoryJpaEntity cat = i.getCategory();

    return IngredienteCardResponse.builder()
        .id(i.getId().toString())
        .codigo(i.getCode())
        .nome(i.getName())
        .categoria(cat != null ? cat.getName() : null)
        .categoriaId(cat != null ? cat.getId().toString() : null)
        .descricao(i.getDescription())
        .unidade(i.getUnit() != null ? i.getUnit().getSymbol() : null)
        .preco(i.getCostPerUnit())
        .variacaoPreco(variacao)
        .build();
  }

  private UUID resolverWorkspaceId(UsuarioAutenticado usuario) {
    String wid = usuario.workspaceId();
    if (wid == null || wid.isBlank()) {
      throw new IngredienteException(
          "Usuário não está vinculado a nenhum workspace. Conclua o onboarding.");
    }
    return UUID.fromString(wid);
  }

  private IngredienteResponse montarResponse(IngredientJpaEntity i) {
    IngredientCategoryJpaEntity cat = i.getCategory();
    UnitJpaEntity unit = i.getUnit();

    return IngredienteResponse.builder()
        .id(i.getId().toString())
        .codigo(i.getCode())
        .nome(i.getName())
        .descricao(i.getDescription())
        .marca(i.getBrand())
        .categoriaId(cat != null ? cat.getId().toString() : null)
        .categoriaNome(cat != null ? cat.getName() : null)
        .unidadeId(unit != null ? unit.getId().toString() : null)
        .unidadeSimbolo(unit != null ? unit.getSymbol() : null)
        .unidadeNome(unit != null ? unit.getName() : null)
        .precoPorUnidade(i.getCostPerUnit())
        .notas(i.getNotes())
        .criadoEm(i.getCreatedAt())
        .atualizadoEm(i.getUpdatedAt())
        .build();
  }

  /**
   * Garante que não existam dois ingredientes com o mesmo nome no workspace. Quando {@code
   * idExcluir} é fornecido (operação de update), o ingrediente com aquele ID é excluído da checagem
   * (permite salvar sem mudar o nome).
   */
  private void validarNomeUnico(String nome, UUID workspaceId, UUID idExcluir) {
    boolean existe =
        ingredientRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
            workspaceId, nome.trim());
    if (!existe) return;

    if (idExcluir != null) {
      Optional<IngredientJpaEntity> existente =
          ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(idExcluir, workspaceId);
      if (existente.isPresent() && existente.get().getName().equalsIgnoreCase(nome.trim())) {
        return; // mesmo ingrediente — update sem mudar nome
      }
    }
    throw new IngredienteException(
        "Já existe um ingrediente com o nome '" + nome.trim() + "' neste workspace.");
  }

  /**
   * Garante que não existam dois ingredientes com o mesmo código no workspace. Quando {@code
   * idExcluir} é fornecido (operação de update), o ingrediente com aquele ID é excluído da checagem
   * (permite salvar sem mudar o código).
   */
  private void validarCodigoUnico(String codigo, UUID workspaceId, UUID idExcluir) {
    if (codigo == null || codigo.isBlank()) return;

    Optional<IngredientJpaEntity> existente =
        ingredientRepository.findByWorkspaceIdAndCodeAndDeletedAtIsNull(workspaceId, codigo.trim());

    if (existente.isEmpty()) return;

    if (idExcluir != null && existente.get().getId().equals(idExcluir)) {
      return; // mesmo ingrediente — update sem mudar o código
    }

    throw new IngredienteException(
        "Já existe um ingrediente com o código '" + codigo.trim() + "' neste workspace.");
  }

  private WorkspaceJpaEntity buscarWorkspace(UUID workspaceId) {
    return workspaceRepository
        .findByIdAndDeletedAtIsNull(workspaceId)
        .orElseThrow(() -> new IngredienteException("Workspace não encontrado."));
  }

  private IngredientCategoryJpaEntity buscarCategoria(UUID categoriaId) {
    return categoryRepository
        .findByIdAndDeletedAtIsNull(categoriaId)
        .orElseThrow(
            () ->
                new IngredienteException(
                    "Categoria de ingrediente não encontrada: " + categoriaId));
  }

  private UnitJpaEntity buscarUnidade(UUID unidadeId) {
    return unitRepository
        .findByIdAndDeletedAtIsNull(unidadeId)
        .orElseThrow(
            () -> new IngredienteException("Unidade de medida não encontrada: " + unidadeId));
  }

  private IngredientJpaEntity buscarIngrediente(UUID id, UUID workspaceId) {
    return ingredientRepository
        .findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
        .orElseThrow(() -> new IngredienteException("Ingrediente não encontrado: " + id));
  }

  private UserJpaEntity buscarUsuario(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new IngredienteException("Usuário não encontrado: " + userId));
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
