package com.api.konditor.domain.useCase.impl;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CalcularCustosRequest;
import com.api.konditor.app.controller.request.CriarReceitaRequest;
import com.api.konditor.app.controller.request.IngredienteReceitaRequest;
import com.api.konditor.app.controller.response.BuscaIngredienteResponse;
import com.api.konditor.app.controller.response.CategoriaReceitaResponse;
import com.api.konditor.app.controller.response.CustosCalculadosResponse;
import com.api.konditor.app.controller.response.IngredienteReceitaResponse;
import com.api.konditor.app.controller.response.ReceitaResponse;
import com.api.konditor.app.exception.ReceitaException;
import com.api.konditor.domain.enuns.RecipeStatus;
import com.api.konditor.domain.useCase.ReceitaUseCase;
import com.api.konditor.infra.jpa.entity.IngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductCategoryJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductIngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.ProductJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitConversionJpaEntity;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import com.api.konditor.infra.jpa.entity.WorkspaceJpaEntity;
import com.api.konditor.infra.jpa.repository.IngredientJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductCategoryJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductIngredientJpaRepository;
import com.api.konditor.infra.jpa.repository.ProductJpaRepository;
import com.api.konditor.infra.jpa.repository.UnitConversionJpaRepository;
import com.api.konditor.infra.jpa.repository.UnitJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do caso de uso de receitas.
 *
 * <p>Cobre todo o ciclo de vida de uma receita:
 * criação (rascunho), edição, publicação e cálculo de custos em tempo real.
 *
 * <p>Fórmulas de precificação (percentuais configuráveis por request):
 * <pre>
 *   custoIngredientes = Σ (costPerUnit × quantidade × fatorConversão)
 *   maoDeObra        = custoIngredientes × pctMaoDeObra / 100
 *   custosFixos      = custoIngredientes × pctCustosFixos / 100
 *   custoTotal       = custoIngredientes + maoDeObra + custosFixos
 *   precoSugerido    = custoTotal / (1 - margem / 100)
 * </pre>
 *
 * <p>Para conversão de unidades, consulta a tabela {@code unit_conversions}.
 * Se não houver conversão cadastrada entre as unidades, assume fator = 1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceitaUseCaseImpl implements ReceitaUseCase {

    private static final BigDecimal MARGEM_PADRAO = new BigDecimal("30.00");

    private final ProductJpaRepository           productRepository;
    private final ProductIngredientJpaRepository productIngredientRepository;
    private final ProductCategoryJpaRepository   productCategoryRepository;
    private final IngredientJpaRepository        ingredientRepository;
    private final UnitJpaRepository              unitRepository;
    private final UnitConversionJpaRepository    unitConversionRepository;
    private final WorkspaceJpaRepository         workspaceRepository;

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

        ProductCategoryJpaEntity category = request.getCategoriaId() != null
                ? buscarCategoria(request.getCategoriaId()) : null;
        UnitJpaEntity yieldUnit = request.getRendimentoUnidadeId() != null
                ? buscarUnidade(request.getRendimentoUnidadeId()) : null;

        RecipeStatus status = resolverStatus(request.getStatus());

        ProductJpaEntity product = ProductJpaEntity.builder()
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
                .build();
        product = productRepository.save(product);

        List<IngredienteReceitaResponse> ingredientes =
                salvarIngredientes(product, request.getIngredientes(), workspaceId);

        recalcularCustos(product, ingredientes);
        log.info("[RECEITA] Receita criada id={} status={} custo={}", product.getId(), status, product.getCalculatedCost());

        return montarResponse(product, ingredientes);
    }

    @Override
    @Transactional
    public ReceitaResponse atualizar(UUID id, CriarReceitaRequest request, UsuarioAutenticado usuario) {
        UUID workspaceId = resolverWorkspaceId(usuario);
        log.info("[RECEITA] Atualizando receita id={} — workspaceId={}", id, workspaceId);

        ProductJpaEntity product = buscarReceita(id, workspaceId);
        validarNomeUnico(request.getNome(), workspaceId, id);

        ProductCategoryJpaEntity category = request.getCategoriaId() != null
                ? buscarCategoria(request.getCategoriaId()) : null;
        UnitJpaEntity yieldUnit = request.getRendimentoUnidadeId() != null
                ? buscarUnidade(request.getRendimentoUnidadeId()) : null;

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

        // Substitui ingredientes completamente (delete + re-insert)
        productIngredientRepository.deleteAllByProductId(id);

        List<IngredienteReceitaResponse> ingredientes =
                salvarIngredientes(product, request.getIngredientes(), workspaceId);

        recalcularCustos(product, ingredientes);
        log.info("[RECEITA] Receita id={} atualizada", id);

        return montarResponse(product, ingredientes);
    }

    @Override
    @Transactional
    public ReceitaResponse publicar(UUID id, UsuarioAutenticado usuario) {
        UUID workspaceId = resolverWorkspaceId(usuario);
        log.info("[RECEITA] Publicando receita id={} — workspaceId={}", id, workspaceId);

        ProductJpaEntity product = buscarReceita(id, workspaceId);
        product.setStatus(RecipeStatus.publicada);

        List<IngredienteReceitaResponse> ingredientes = carregarIngredientesResponse(product);
        log.info("[RECEITA] Receita id={} publicada com sucesso", id);

        return montarResponse(product, ingredientes);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceitaResponse buscarPorId(UUID id, UsuarioAutenticado usuario) {
        UUID workspaceId = resolverWorkspaceId(usuario);
        ProductJpaEntity product = buscarReceita(id, workspaceId);
        List<IngredienteReceitaResponse> ingredientes = carregarIngredientesResponse(product);
        return montarResponse(product, ingredientes);
    }

    @Override
    @Transactional(readOnly = true)
    public CustosCalculadosResponse calcularCustos(CalcularCustosRequest request, UsuarioAutenticado usuario) {
        UUID workspaceId = resolverWorkspaceId(usuario);
        log.debug("[RECEITA] Calculando custos — workspaceId={} ingredientes={}", workspaceId, request.getIngredientes().size());

        BigDecimal margemDesejada = coalesce(request.getMargemDesejada(), MARGEM_PADRAO);

        BigDecimal custoIngredientes = BigDecimal.ZERO;
        for (IngredienteReceitaRequest item : request.getIngredientes()) {
            IngredientJpaEntity ingredient = ingredientRepository
                    .findByIdAndWorkspaceIdAndDeletedAtIsNull(item.getIngredienteId(), workspaceId)
                    .orElseThrow(() -> new ReceitaException(
                            "Ingrediente não encontrado: " + item.getIngredienteId()));
            UnitJpaEntity unit = buscarUnidade(item.getUnidadeId());

            BigDecimal fator = resolverFatorConversao(unit, ingredient.getUnit());
            BigDecimal custo = ingredient.getCostPerUnit()
                    .multiply(item.getQuantidade().multiply(fator));
            custoIngredientes = custoIngredientes.add(custo);
        }

        // 2. Mão de obra = valorHora × (minutos / 60)
        BigDecimal custoMaoDeObra = BigDecimal.ZERO;
        if (request.getTempoPreparoMinutos() != null
                && request.getTempoPreparoMinutos().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal horas = request.getTempoPreparoMinutos()
                    .divide(new BigDecimal("60"), 10, RoundingMode.HALF_UP);
            custoMaoDeObra = request.getMaoDeObraValorHora().multiply(horas);
        }

        // 3. Custos fixos — percentual sobre ingredientes ou valor direto
        BigDecimal custosFixos;
        if ("percentual".equalsIgnoreCase(request.getCustosFixosTipo())) {
            custosFixos = percentual(custoIngredientes, request.getCustosFixosValor());
        } else {
            custosFixos = request.getCustosFixosValor();
        }

        // 4. Totais do lote
        BigDecimal custoTotal    = custoIngredientes.add(custoMaoDeObra).add(custosFixos);
        BigDecimal precoSugerido = calcularPrecoSugerido(custoTotal, margemDesejada);
        BigDecimal margemReal    = calcularMargemReal(custoTotal, precoSugerido);

        // 5. Por unidade
        BigDecimal rendimento = request.getRendimentoQuantidade();
        BigDecimal custoTotalPorUnidade = custoTotal
                .divide(rendimento, 4, RoundingMode.HALF_UP);
        BigDecimal precoSugeridoPorUnidade = precoSugerido
                .divide(rendimento, 4, RoundingMode.HALF_UP);

        return CustosCalculadosResponse.builder()
                .custoIngredientes(custoIngredientes.setScale(2, RoundingMode.HALF_UP))
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
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaReceitaResponse> listarCategorias() {
        log.debug("[RECEITA] Listando categorias globais de receita");

        return productCategoryRepository.findAllByDeletedAtIsNullOrderByNameAsc()
                .stream()
                .map(c -> CategoriaReceitaResponse.builder()
                        .id(c.getId().toString())
                        .nome(c.getName())
                        .cor(c.getColor())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuscaIngredienteResponse> buscarIngredientes(String query, UsuarioAutenticado usuario) {
        UUID workspaceId = resolverWorkspaceId(usuario);
        String q = (query == null || query.isBlank()) ? "" : query.trim();
        log.debug("[INGREDIENTE] Busca — query='{}' workspaceId={}", q, workspaceId);

        return ingredientRepository
                .searchByWorkspaceAndName(workspaceId, q)
                .stream()
                .map(i -> BuscaIngredienteResponse.builder()
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
     * Persiste a lista de ingredientes de uma receita e retorna as linhas montadas
     * já com os custos calculados por linha.
     */
    private List<IngredienteReceitaResponse> salvarIngredientes(
            ProductJpaEntity product,
            List<IngredienteReceitaRequest> items,
            UUID workspaceId) {

        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<IngredienteReceitaResponse> responses = new ArrayList<>();
        for (IngredienteReceitaRequest item : items) {

            IngredientJpaEntity ingredient = ingredientRepository
                    .findByIdAndWorkspaceIdAndDeletedAtIsNull(item.getIngredienteId(), workspaceId)
                    .orElseThrow(() -> new ReceitaException(
                            "Ingrediente não encontrado ou não pertence a este workspace: "
                                    + item.getIngredienteId()));

            UnitJpaEntity unit = buscarUnidade(item.getUnidadeId());

            BigDecimal fator = resolverFatorConversao(unit, ingredient.getUnit());
            BigDecimal custoLinha = ingredient.getCostPerUnit()
                    .multiply(item.getQuantidade().multiply(fator))
                    .setScale(4, RoundingMode.HALF_UP);

            ProductIngredientJpaEntity pi = ProductIngredientJpaEntity.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .quantity(item.getQuantidade())
                    .unit(unit)
                    .notes(item.getNotas())
                    .build();
            pi = productIngredientRepository.save(pi);

            responses.add(IngredienteReceitaResponse.builder()
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
     * Carrega os ingredientes de uma receita existente a partir do banco
     * (com FETCH JOINs para evitar N+1) e monta as responses com custo calculado.
     */
    private List<IngredienteReceitaResponse> carregarIngredientesResponse(ProductJpaEntity product) {
        return productIngredientRepository
                .findAllByProductIdWithDetails(product.getId())
                .stream()
                .map(pi -> {
                    IngredientJpaEntity ing  = pi.getIngredient();
                    UnitJpaEntity recipeUnit = pi.getUnit();
                    BigDecimal fator = resolverFatorConversao(recipeUnit, ing.getUnit());
                    BigDecimal custo = ing.getCostPerUnit()
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

    /**
     * Recalcula {@code calculatedCost} e {@code suggestedPrice} da entidade produto
     * com base na lista de ingredientes já montada, e persiste via dirty-checking.
     */
    private void recalcularCustos(ProductJpaEntity product, List<IngredienteReceitaResponse> ingredientes) {
        BigDecimal custoTotal = ingredientes.stream()
                .map(IngredienteReceitaResponse::getCustoCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal precoSugerido = calcularPrecoSugerido(custoTotal, MARGEM_PADRAO);

        product.setCalculatedCost(custoTotal.setScale(4, RoundingMode.HALF_UP));
        product.setSuggestedPrice(precoSugerido.setScale(2, RoundingMode.HALF_UP));
    }

    // =========================================================================
    // Fórmulas financeiras
    // =========================================================================

    /**
     * precoSugerido = custoTotal / (1 - margem/100)
     */
    private BigDecimal calcularPrecoSugerido(BigDecimal custoTotal, BigDecimal margemPercent) {
        if (custoTotal == null || custoTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal divisor = BigDecimal.ONE
                .subtract(margemPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return custoTotal;
        }
        return custoTotal.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    /**
     * margem (%) = ((preco - custo) / preco) × 100
     */
    private BigDecimal calcularMargemReal(BigDecimal custo, BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return preco.subtract(custo)
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
    private BigDecimal resolverFatorConversao(UnitJpaEntity unidadeReceita, UnitJpaEntity unidadeBase) {
        if (unidadeReceita == null || unidadeBase == null) {
            return BigDecimal.ONE;
        }
        if (unidadeReceita.getId().equals(unidadeBase.getId())) {
            return BigDecimal.ONE;
        }
        Optional<UnitConversionJpaEntity> conv = unitConversionRepository
                .findByFromUnitIdAndToUnitId(unidadeReceita.getId(), unidadeBase.getId());
        return conv.map(UnitConversionJpaEntity::getFactor).orElse(BigDecimal.ONE);
    }

    // =========================================================================
    // Montagem da resposta
    // =========================================================================

    private ReceitaResponse montarResponse(ProductJpaEntity product, List<IngredienteReceitaResponse> ingredientes) {
        ProductCategoryJpaEntity cat   = product.getCategory();
        UnitJpaEntity            yUnit = product.getYieldUnit();

        BigDecimal margem = calcularMargemReal(product.getCalculatedCost(), product.getSellingPrice());

        return ReceitaResponse.builder()
                .id(product.getId().toString())
                .nome(product.getName())
                .descricao(product.getDescription())
                .categoriaId(cat   != null ? cat.getId().toString()   : null)
                .categoriaNome(cat != null ? cat.getName()            : null)
                .rendimentoQuantidade(product.getYieldQuantity())
                .rendimentoUnidadeId(yUnit  != null ? yUnit.getId().toString() : null)
                .rendimentoUnidadeSimbolo(yUnit != null ? yUnit.getSymbol() : null)
                .rendimentoUnidadeNome(yUnit    != null ? yUnit.getName()   : null)
                .tempoPreparoMinutos(product.getPrepTimeMinutes())
                .ingredientes(ingredientes)
                .notas(product.getNotes())
                .precoFinal(product.getSellingPrice())
                .precoSugerido(product.getSuggestedPrice())
                .custoCalculado(product.getCalculatedCost())
                .margem(margem.setScale(1, RoundingMode.HALF_UP))
                .status(product.getStatus())
                .ativo(product.isActive())
                .criadoEm(product.getCreatedAt())
                .atualizadoEm(product.getUpdatedAt())
                .build();
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
     * Garante que não existam duas receitas com o mesmo nome no workspace.
     * Quando {@code idExcluir} é fornecido (operação de update), o produto com aquele ID
     * é excluído da checagem.
     */
    private void validarNomeUnico(String nome, UUID workspaceId, UUID idExcluir) {
        boolean existe = productRepository
                .existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(workspaceId, nome.trim());
        if (!existe) return;

        if (idExcluir != null) {
            Optional<ProductJpaEntity> existente = productRepository.findByIdAndDeletedAtIsNull(idExcluir);
            if (existente.isPresent()
                    && existente.get().getName().equalsIgnoreCase(nome.trim())) {
                return; // mesmo produto — update sem mudar nome
            }
        }
        throw new ReceitaException(
                "Já existe uma receita com o nome '" + nome.trim() + "' neste workspace.");
    }

    private WorkspaceJpaEntity buscarWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new ReceitaException("Workspace não encontrado."));
    }

    private ProductCategoryJpaEntity buscarCategoria(UUID categoriaId) {
        return productCategoryRepository
                .findByIdAndDeletedAtIsNull(categoriaId)
                .orElseThrow(() -> new ReceitaException("Categoria não encontrada: " + categoriaId));
    }

    private UnitJpaEntity buscarUnidade(UUID unidadeId) {
        return unitRepository.findByIdAndDeletedAtIsNull(unidadeId)
                .orElseThrow(() -> new ReceitaException("Unidade não encontrada: " + unidadeId));
    }

    private ProductJpaEntity buscarReceita(UUID id, UUID workspaceId) {
        return productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(id, workspaceId)
                .orElseThrow(() -> new ReceitaException("Receita não encontrada: " + id));
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
}

