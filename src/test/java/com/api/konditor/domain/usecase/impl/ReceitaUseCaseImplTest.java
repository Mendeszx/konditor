package com.api.konditor.domain.usecase.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CalcularCustosRequest;
import com.api.konditor.app.controller.request.CriarReceitaRequest;
import com.api.konditor.app.controller.request.IngredienteReceitaRequest;
import com.api.konditor.app.controller.request.ReceitaComoIngredienteRequest;
import com.api.konditor.app.controller.response.BuscaIngredienteResponse;
import com.api.konditor.app.controller.response.CategoriaReceitaResponse;
import com.api.konditor.app.controller.response.CustosCalculadosResponse;
import com.api.konditor.app.controller.response.ReceitaComoIngredienteResponse;
import com.api.konditor.app.controller.response.ReceitaResponse;
import com.api.konditor.app.exception.ReceitaException;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.RecipeStatus;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.domain.enuns.UnitType;
import com.api.konditor.infra.jpa.entity.CategoriaProdutoJpaEntity;
import com.api.konditor.infra.jpa.entity.ConversaoUnidadeJpaEntity;
import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import com.api.konditor.infra.jpa.entity.IngredienteJpaEntity;
import com.api.konditor.infra.jpa.entity.IngredienteProdutoJpaEntity;
import com.api.konditor.infra.jpa.entity.LogAuditoriaJpaEntity;
import com.api.konditor.infra.jpa.entity.ProdutoJpaEntity;
import com.api.konditor.infra.jpa.entity.ReceitaComoIngredienteJpaEntity;
import com.api.konditor.infra.jpa.entity.UnidadeJpaEntity;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitarios de {@link ReceitaUseCaseImpl} cobrindo todos os casos de uso: criar, atualizar,
 * publicar, buscarPorId, calcularCustos, listarCategorias, buscarIngredientes.
 *
 * <p>Os testes de calcularCustos sao orientados a mutantes (mutation testing via PIT).
 */
@ExtendWith(MockitoExtension.class)
class ReceitaUseCaseImplTest {

  @Mock private ProductJpaRepository productRepository;
  @Mock private ProductIngredientJpaRepository productIngredientRepository;
  @Mock private ProductRecipeIngredientJpaRepository productRecipeIngredientRepository;
  @Mock private ProductCategoryJpaRepository productCategoryRepository;
  @Mock private IngredientJpaRepository ingredientRepository;
  @Mock private UnitJpaRepository unitRepository;
  @Mock private UnitConversionJpaRepository unitConversionRepository;
  @Mock private WorkspaceJpaRepository workspaceRepository;
  @Mock private UserJpaRepository userRepository;
  @Mock private AuditLogJpaRepository auditLogRepository;

  @InjectMocks private ReceitaUseCaseImpl sut;

  // ── IDs fixos reutilizados nos testes ─────────────────────────────────────
  private static final UUID WORKSPACE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PRODUCT_ID = UUID.randomUUID();
  private static final UUID SUB_RECEITA_ID = UUID.randomUUID();
  private static final UUID ING_ID = UUID.randomUUID();
  private static final UUID UNIT_G_ID = UUID.randomUUID();
  private static final UUID UNIT_KG_ID = UUID.randomUUID();
  private static final UUID UNIT_ML_ID = UUID.randomUUID();
  private static final UUID UNIT_L_ID = UUID.randomUUID();
  private static final UUID UNIT_COLHER_SOPA_ID = UUID.randomUUID();
  private static final UUID UNIT_UN_ID = UUID.randomUUID();
  private static final UUID UNIT_DZ_ID = UUID.randomUUID();

  // ── Fixtures ──────────────────────────────────────────────────────────────
  private UsuarioAutenticado usuario;
  private EspacoTrabalhoJpaEntity workspace;
  private UsuarioJpaEntity userEntity;
  private UnidadeJpaEntity unitG;
  private UnidadeJpaEntity unitKg;
  private UnidadeJpaEntity unitMl;
  private UnidadeJpaEntity unitL;
  private UnidadeJpaEntity unitColherSopa;
  private UnidadeJpaEntity unitUn;
  private UnidadeJpaEntity unitDz;

  @BeforeEach
  void setUp() {
    usuario =
        new UsuarioAutenticado(
            USER_ID.toString(),
            "chef@konditor.io",
            "Chef Teste",
            WORKSPACE_ID.toString(),
            Role.owner,
            Plan.premium);

    workspace = EspacoTrabalhoJpaEntity.builder().id(WORKSPACE_ID).nome("Meu Workspace").build();
    userEntity = UsuarioJpaEntity.builder().id(USER_ID).email("chef@konditor.io").build();

    unitG = unit(UNIT_G_ID, "Grama", "g", UnitType.weight);
    unitKg = unit(UNIT_KG_ID, "Quilograma", "kg", UnitType.weight);
    unitMl = unit(UNIT_ML_ID, "Mililitro", "ml", UnitType.volume);
    unitL = unit(UNIT_L_ID, "Litro", "L", UnitType.volume);
    unitColherSopa = unit(UNIT_COLHER_SOPA_ID, "Colher de sopa", "colher sopa", UnitType.volume);
    unitUn = unit(UNIT_UN_ID, "Unidade", "un", UnitType.unit);
    unitDz = unit(UNIT_DZ_ID, "Duzia", "dz", UnitType.unit);
  }

  // =========================================================================
  // criar()
  // =========================================================================

  @Nested
  @DisplayName("criar()")
  class CriarTest {

    @Test
    @DisplayName("Cria receita rascunho sem ingredientes e registra audit log")
    void criar_semIngredientes_retornaResponse() {
      CriarReceitaRequest req = criarRequest("Bolo de Cenoura", null, "rascunho");

      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Bolo de Cenoura", RecipeStatus.rascunho);
      when(productRepository.save(any())).thenReturn(saved);

      ReceitaResponse resp = sut.criar(req, usuario);

      assertThat(resp.getId()).isEqualTo(PRODUCT_ID.toString());
      assertThat(resp.getNome()).isEqualTo("Bolo de Cenoura");
      assertThat(resp.getStatus()).isEqualTo(RecipeStatus.rascunho);
      verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName(
        "Cria receita com custo pre-calculado (fluxo: /calcular -> /criar): custo e persistido")
    void criar_comIngrediente_calculaCusto() {
      // Fluxo real: cliente chama /calcular primeiro e recebe custoCalculado = 5.0000
      // (500g * R$0.01/g), depois envia esse valor pre-calculado no corpo do /criar.
      IngredienteJpaEntity farinha = ingrediente(ING_ID, "Farinha", new BigDecimal("0.01"), unitG);
      CriarReceitaRequest req =
          criarRequestComIngrediente("Bolo", ING_ID, UNIT_G_ID, new BigDecimal("500"), "rascunho");
      req.setCustoIngredientes(new BigDecimal("5.0000")); // 500g * R$0.01
      req.setCustoCalculado(new BigDecimal("5.0000")); // sem MO e fixos neste cenário

      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(farinha));
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.rascunho);
      saved.setCalculatedCost(new BigDecimal("5.0000"));
      saved.setIngredientCost(new BigDecimal("5.0000"));
      IngredienteProdutoJpaEntity pi = piEntity(saved, farinha, unitG, new BigDecimal("500"));
      when(productRepository.save(any())).thenReturn(saved);
      when(productIngredientRepository.save(any())).thenReturn(pi);

      ReceitaResponse resp = sut.criar(req, usuario);

      assertThat(resp.getCustoCalculado()).isEqualByComparingTo("5.0000");
    }

    @Test
    @DisplayName("Nome duplicado no workspace lanca ReceitaException")
    void criar_nomeDuplicado_lancaExcecao() {
      CriarReceitaRequest req = criarRequest("Bolo de Cenoura", null, null);
      mockWorkspace();
      mockNomeUnico(true);

      assertThatThrownBy(() -> sut.criar(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Bolo de Cenoura");
    }

    @Test
    @DisplayName("Status nulo na request resulta em status rascunho")
    void criar_statusNulo_virarascunho() {
      CriarReceitaRequest req = criarRequest("Torta", null, null);
      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Torta", RecipeStatus.rascunho);
      when(productRepository.save(any())).thenReturn(saved);

      ReceitaResponse resp = sut.criar(req, usuario);

      assertThat(resp.getStatus()).isEqualTo(RecipeStatus.rascunho);
    }

    @Test
    @DisplayName("Status invalido lanca ReceitaException")
    void criar_statusInvalido_lancaExcecao() {
      // rendimentoUnidadeId=null evita chamada a buscarUnidade antes de resolverStatus
      CriarReceitaRequest req = criarRequest("Torta", null, "invalido");
      req.setRendimentoUnidadeId(null);
      mockWorkspace();
      mockNomeUnico(false);

      assertThatThrownBy(() -> sut.criar(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("inválido");
    }

    @Test
    @DisplayName("Workspace nao encontrado lanca ReceitaException")
    void criar_workspaceNaoEncontrado_lancaExcecao() {
      CriarReceitaRequest req = criarRequest("Torta", null, null);
      when(workspaceRepository.findByIdAndDeletedAtIsNull(WORKSPACE_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> sut.criar(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Workspace");
    }

    @Test
    @DisplayName("Workspace ausente no token lanca ReceitaException")
    void criar_workspaceAusenteNoToken_lancaExcecao() {
      UsuarioAutenticado semWs =
          new UsuarioAutenticado(USER_ID.toString(), "x@x.com", "X", null, Role.owner, Plan.free);
      CriarReceitaRequest req = criarRequest("Torta", null, null);

      assertThatThrownBy(() -> sut.criar(req, semWs)).isInstanceOf(ReceitaException.class);
    }
  }

  // =========================================================================
  // atualizar()
  // =========================================================================

  @Nested
  @DisplayName("atualizar()")
  class AtualizarTest {

    @Test
    @DisplayName("Atualiza receita existente com novos dados")
    void atualizar_dadosValidos_retornaResponseAtualizada() {
      ProdutoJpaEntity existente = produtoSalvo(PRODUCT_ID, "Bolo Antigo", RecipeStatus.rascunho);
      existente.setWorkspace(workspace);

      CriarReceitaRequest req = criarRequest("Bolo Novo", null, "publicada");

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      mockNomeUnicoParaId(false, PRODUCT_ID);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();
      lenient()
          .when(productIngredientRepository.findAllByProductIdWithDetails(any()))
          .thenReturn(List.of());
      when(productRepository.save(any())).thenReturn(existente);

      ReceitaResponse resp = sut.atualizar(PRODUCT_ID, req, usuario);

      assertThat(resp.getNome()).isEqualTo("Bolo Novo");
      assertThat(resp.getStatus()).isEqualTo(RecipeStatus.publicada);
      verify(productIngredientRepository).deleteAllByProductId(PRODUCT_ID);
      verify(productRecipeIngredientRepository).deleteAllByProductId(PRODUCT_ID);
      verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Atualizar com mesmo nome (proprio produto) nao lanca excecao")
    void atualizar_mesmoNomeProprioProduto_semExcecao() {
      ProdutoJpaEntity existente = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.rascunho);
      existente.setWorkspace(workspace);

      CriarReceitaRequest req = criarRequest("Bolo", null, null);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      // nome existe mas pertence ao proprio produto
      when(productRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
              WORKSPACE_ID, "Bolo"))
          .thenReturn(true);
      when(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
          .thenReturn(Optional.of(existente));
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();
      lenient()
          .when(productIngredientRepository.findAllByProductIdWithDetails(any()))
          .thenReturn(List.of());
      when(productRepository.save(any())).thenReturn(existente);

      assertThatCode(() -> sut.atualizar(PRODUCT_ID, req, usuario)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Atualizar com nome de outro produto lanca ReceitaException")
    void atualizar_nomeDuplicadoOutroProduto_lancaExcecao() {
      UUID outroId = UUID.randomUUID();
      ProdutoJpaEntity existente = produtoSalvo(PRODUCT_ID, "Bolo A", RecipeStatus.rascunho);
      existente.setWorkspace(workspace);

      CriarReceitaRequest req = criarRequest("Bolo B", null, null);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      when(productRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
              WORKSPACE_ID, "Bolo B"))
          .thenReturn(true);
      when(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
          .thenReturn(Optional.of(existente));

      assertThatThrownBy(() -> sut.atualizar(PRODUCT_ID, req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Bolo B");
    }

    @Test
    @DisplayName("Receita nao encontrada lanca ReceitaException")
    void atualizar_receitaNaoEncontrada_lancaExcecao() {
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> sut.atualizar(PRODUCT_ID, criarRequest("X", null, null), usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("não encontrada");
    }

    @Test
    @DisplayName("Status nulo na request de atualizacao mantém status atual")
    void atualizar_statusNulo_mantemStatusAtual() {
      ProdutoJpaEntity existente = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.publicada);
      existente.setWorkspace(workspace);

      CriarReceitaRequest req = criarRequest("Bolo", null, null);
      req.setStatus(null);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      when(productRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
              WORKSPACE_ID, "Bolo"))
          .thenReturn(true);
      when(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID))
          .thenReturn(Optional.of(existente));
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();
      lenient()
          .when(productIngredientRepository.findAllByProductIdWithDetails(any()))
          .thenReturn(List.of());
      when(productRepository.save(any())).thenReturn(existente);

      ReceitaResponse resp = sut.atualizar(PRODUCT_ID, req, usuario);

      assertThat(resp.getStatus()).isEqualTo(RecipeStatus.publicada);
    }
  }

  // =========================================================================
  // publicar()
  // =========================================================================

  @Nested
  @DisplayName("publicar()")
  class PublicarTest {

    @Test
    @DisplayName("Muda status da receita para publicada")
    void publicar_receitaExistente_statusPublicada() {
      ProdutoJpaEntity existente = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.rascunho);
      existente.setWorkspace(workspace);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      when(productIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of());
      mockUsuario();

      ReceitaResponse resp = sut.publicar(PRODUCT_ID, usuario);

      assertThat(resp.getStatus()).isEqualTo(RecipeStatus.publicada);
      verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Publicar receita inexistente lanca ReceitaException")
    void publicar_receitaInexistente_lancaExcecao() {
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> sut.publicar(PRODUCT_ID, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Receita não encontrada");
    }

    @Test
    @DisplayName("Audit log registrado com evento de publicacao")
    void publicar_registraAuditLogComEventoPublicacao() {
      ProdutoJpaEntity existente = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.rascunho);
      existente.setWorkspace(workspace);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      when(productIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of());
      mockUsuario();

      sut.publicar(PRODUCT_ID, usuario);

      ArgumentCaptor<LogAuditoriaJpaEntity> cap =
          ArgumentCaptor.forClass(LogAuditoriaJpaEntity.class);
      verify(auditLogRepository).save(cap.capture());
      assertThat(cap.getValue().getDataAfter()).contains("publicacao");
    }
  }

  // =========================================================================
  // buscarPorId()
  // =========================================================================

  @Nested
  @DisplayName("buscarPorId()")
  class BuscarPorIdTest {

    @Test
    @DisplayName("Retorna receita com ingredientes carregados")
    void buscarPorId_receitaExistente_retornaResponse() {
      ProdutoJpaEntity product = produtoSalvo(PRODUCT_ID, "Torta", RecipeStatus.publicada);
      product.setWorkspace(workspace);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(product));
      when(productIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of());

      ReceitaResponse resp = sut.buscarPorId(PRODUCT_ID, usuario);

      assertThat(resp.getId()).isEqualTo(PRODUCT_ID.toString());
      assertThat(resp.getNome()).isEqualTo("Torta");
    }

    @Test
    @DisplayName("Receita nao encontrada lanca ReceitaException")
    void buscarPorId_inexistente_lancaExcecao() {
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> sut.buscarPorId(PRODUCT_ID, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Receita não encontrada");
    }

    @Test
    @DisplayName("Ingredientes sao mapeados na response com custo calculado")
    void buscarPorId_comIngredientes_mapeiaCustoCalculado() {
      ProdutoJpaEntity product = produtoSalvo(PRODUCT_ID, "Brownie", RecipeStatus.publicada);
      product.setWorkspace(workspace);
      IngredienteJpaEntity farinha = ingrediente(ING_ID, "Farinha", new BigDecimal("0.01"), unitG);
      IngredienteProdutoJpaEntity pi = piEntity(product, farinha, unitG, new BigDecimal("200"));

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(product));
      when(productIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of(pi));
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      ReceitaResponse resp = sut.buscarPorId(PRODUCT_ID, usuario);

      assertThat(resp.getIngredientes()).hasSize(1);
      // 200 * 0.01 * 1 = 2.00
      assertThat(resp.getIngredientes().get(0).getCustoCalculado()).isEqualByComparingTo("2.0000");
    }
  }

  // =========================================================================
  // listarCategorias()
  // =========================================================================

  @Nested
  @DisplayName("listarCategorias()")
  class ListarCategoriasTest {

    @Test
    @DisplayName("Retorna lista de categorias mapeada corretamente")
    void listarCategorias_retornaListaMapeada() {
      UUID cat1Id = UUID.randomUUID();
      UUID cat2Id = UUID.randomUUID();
      CategoriaProdutoJpaEntity c1 = categoria(cat1Id, "Bolo", "#FF0000");
      CategoriaProdutoJpaEntity c2 = categoria(cat2Id, "Brigadeiro", "#00FF00");

      when(productCategoryRepository.findAllByDeletedAtIsNullOrderByNameAsc())
          .thenReturn(List.of(c1, c2));

      List<CategoriaReceitaResponse> result = sut.listarCategorias();

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getId()).isEqualTo(cat1Id.toString());
      assertThat(result.get(0).getNome()).isEqualTo("Bolo");
      assertThat(result.get(0).getCor()).isEqualTo("#FF0000");
      assertThat(result.get(1).getNome()).isEqualTo("Brigadeiro");
    }

    @Test
    @DisplayName("Lista vazia retorna lista vazia")
    void listarCategorias_listaVazia_retornaVazia() {
      when(productCategoryRepository.findAllByDeletedAtIsNullOrderByNameAsc())
          .thenReturn(List.of());

      assertThat(sut.listarCategorias()).isEmpty();
    }
  }

  // =========================================================================
  // buscarIngredientes()
  // =========================================================================

  @Nested
  @DisplayName("buscarIngredientes()")
  class BuscarIngredientesTest {

    @Test
    @DisplayName("Retorna ingredientes filtrados pela query")
    void buscarIngredientes_comQuery_retornaFiltrado() {
      IngredienteJpaEntity farinha = ingrediente(ING_ID, "Farinha", new BigDecimal("0.01"), unitG);
      farinha.setBrand("Dona Benta");

      when(ingredientRepository.searchByWorkspaceAndName(WORKSPACE_ID, "far"))
          .thenReturn(List.of(farinha));

      List<BuscaIngredienteResponse> result = sut.buscarIngredientes("  far  ", usuario);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getNome()).isEqualTo("Farinha");
      assertThat(result.get(0).getMarca()).isEqualTo("Dona Benta");
      assertThat(result.get(0).getUnidadeSimbolo()).isEqualTo("g");
      assertThat(result.get(0).getCustoPorUnidade()).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("Query nula converte para string vazia")
    void buscarIngredientes_queryNula_passaStringVazia() {
      when(ingredientRepository.searchByWorkspaceAndName(WORKSPACE_ID, "")).thenReturn(List.of());

      List<BuscaIngredienteResponse> result = sut.buscarIngredientes(null, usuario);

      assertThat(result).isEmpty();
      verify(ingredientRepository).searchByWorkspaceAndName(WORKSPACE_ID, "");
    }

    @Test
    @DisplayName("Ingrediente sem unidade mapeia campos de unidade como null")
    void buscarIngredientes_semUnidade_camposNulos() {
      IngredienteJpaEntity ing = ingrediente(ING_ID, "Sal", new BigDecimal("0.005"), null);

      when(ingredientRepository.searchByWorkspaceAndName(WORKSPACE_ID, ""))
          .thenReturn(List.of(ing));

      List<BuscaIngredienteResponse> result = sut.buscarIngredientes("", usuario);

      assertThat(result.get(0).getUnidadeId()).isNull();
      assertThat(result.get(0).getUnidadeSimbolo()).isNull();
      assertThat(result.get(0).getUnidadeNome()).isNull();
    }
  }

  // =========================================================================
  // calcularCustos() — cobertura de formulas / mutation testing
  // =========================================================================

  @Nested
  @DisplayName("calcularCustos() — ingredientes e conversoes")
  class IngredientesConversaoTest {

    @Test
    @DisplayName("Mesma unidade: fator=1, custo = costPerUnit x quantidade")
    void mesmaUnidade_fatorUm() {
      setupIngredienteSimples(new BigDecimal("0.01"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "500", "0", "0", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("Conversao kg para g (fator 1000): 0.5 kg a R$0.01/g = R$5.00")
    void conversao_kg_para_g() {
      setupIngredienteSimples(new BigDecimal("0.01"), unitG);
      mockUnidade(UNIT_KG_ID, unitKg);
      mockConversao(UNIT_KG_ID, UNIT_G_ID, new BigDecimal("1000.000000"));

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_KG_ID, "0.5", "0", "0", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("Conversao L para ml (fator 1000): 2L a R$0.005/ml = R$10.00")
    void conversao_L_para_ml() {
      setupIngredienteSimples(new BigDecimal("0.005"), unitMl);
      mockUnidade(UNIT_L_ID, unitL);
      mockConversao(UNIT_L_ID, UNIT_ML_ID, new BigDecimal("1000.000000"));

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_L_ID, "2", "0", "0", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Conversao colher sopa para ml (fator 15): 4 colheres a R$0.10/ml = R$6.00")
    void conversao_colherSopa_para_ml() {
      setupIngredienteSimples(new BigDecimal("0.10"), unitMl);
      mockUnidade(UNIT_COLHER_SOPA_ID, unitColherSopa);
      mockConversao(UNIT_COLHER_SOPA_ID, UNIT_ML_ID, new BigDecimal("15.000000"));

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_COLHER_SOPA_ID, "4", "0", "0", "0", "fixo", "1", null),
              usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("Conversao duzia para unidade (fator 12): 2 dz a R$0.75/un = R$18.00")
    void conversao_dz_para_un() {
      setupIngredienteSimples(new BigDecimal("0.75"), unitUn);
      mockUnidade(UNIT_DZ_ID, unitDz);
      mockConversao(UNIT_DZ_ID, UNIT_UN_ID, new BigDecimal("12.000000"));

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_DZ_ID, "2", "0", "0", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("Sem conversao cadastrada: fallback fator=1")
    void semConversao_fallbackFatorUm() {
      setupIngredienteSimples(new BigDecimal("2.00"), unitG);
      mockUnidade(UNIT_KG_ID, unitKg);
      when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_KG_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_KG_ID, "3", "0", "0", "0", "fixo", "1", null), usuario);

      // 3 * 1 * 2.00 = 6.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("Multiplos ingredientes: soma correta")
    void multiplosIngredientes_somaCorreta() {
      UUID ing2Id = UUID.randomUUID();
      IngredienteJpaEntity farinha = ingrediente(ING_ID, "Farinha", new BigDecimal("0.01"), unitG);
      IngredienteJpaEntity manteiga =
          ingrediente(ing2Id, "Manteiga", new BigDecimal("0.05"), unitG);

      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(farinha));
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ing2Id, WORKSPACE_ID))
          .thenReturn(Optional.of(manteiga));
      when(unitRepository.findByIdAndDeletedAtIsNull(UNIT_G_ID)).thenReturn(Optional.of(unitG));
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CalcularCustosRequest req = new CalcularCustosRequest();
      req.setIngredientes(List.of(item(ING_ID, UNIT_G_ID, "500"), item(ing2Id, UNIT_G_ID, "200")));
      req.setRendimentoQuantidade(new BigDecimal("10"));
      req.setMaoDeObraValorHora(BigDecimal.ZERO);
      req.setTempoPreparoMinutos(BigDecimal.ZERO);
      req.setCustosFixosValor(BigDecimal.ZERO);
      req.setCustosFixosTipo("fixo");

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 500*0.01 + 200*0.05 = 5 + 10 = 15
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("15.00");
    }
  }

  @Nested
  @DisplayName("calcularCustos() — mao de obra")
  class MaoDeObraTest {

    @Test
    @DisplayName("valorHora=25, tempo=120 min: custo=50.00")
    void maoDeObra_120min_custo50() {
      setupIngredienteSimples(new BigDecimal("10.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "25", "120", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoMaoDeObra()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("valorHora=30, tempo=90 min: custo=45.00")
    void maoDeObra_90min_custo45() {
      setupIngredienteSimples(new BigDecimal("10.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "30", "90", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoMaoDeObra()).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("Tempo zero: custo mao de obra = 0")
    void tempoZero_maoDeObraZero() {
      setupIngredienteSimples(new BigDecimal("10.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "25", "0", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoMaoDeObra()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("valorHora=0: custo mao de obra = 0 mesmo com tempo > 0")
    void valorHoraZero_maoDeObraZero() {
      setupIngredienteSimples(new BigDecimal("10.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "120", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustoMaoDeObra()).isEqualByComparingTo("0.00");
    }
  }

  @Nested
  @DisplayName("calcularCustos() — custos fixos")
  class CustosFixosTest {

    @Test
    @DisplayName("Tipo fixo: valor direto R$50.00")
    void custosFixos_tipoFixo() {
      setupIngredienteSimples(new BigDecimal("100.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "50", "fixo", "1", null), usuario);

      assertThat(resp.getCustosFixos()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Tipo percentual: 20% sobre R$100.00 = R$20.00")
    void custosFixos_percentual20() {
      setupIngredienteSimples(new BigDecimal("100.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "20", "percentual", "1", null), usuario);

      assertThat(resp.getCustosFixos()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("Tipo PERCENTUAL (uppercase): equalsIgnoreCase aceita")
    void custosFixos_percentualUppercase() {
      setupIngredienteSimples(new BigDecimal("100.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "20", "PERCENTUAL", "1", null), usuario);

      assertThat(resp.getCustosFixos()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("Custos fixos zero: nao altera custo total")
    void custosFixos_zero() {
      setupIngredienteSimples(new BigDecimal("80.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "1", null), usuario);

      assertThat(resp.getCustosFixos()).isEqualByComparingTo("0.00");
      assertThat(resp.getCustoTotal()).isEqualByComparingTo("80.00");
    }
  }

  @Nested
  @DisplayName("calcularCustos() — margem e preco sugerido")
  class MargemPrecoTest {

    @Test
    @DisplayName("Margem padrao 30% quando null: custo=100 -> preco=142.86")
    void margemPadrao30_precoSugerido() {
      setupIngredienteSimples(new BigDecimal("100.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "1", null), usuario);

      assertThat(resp.getPrecoSugerido()).isEqualByComparingTo("142.86");
      assertThat(resp.getMargemUtilizada()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Margem 50%: custo=100 -> preco=200.00")
    void margemCinquentaPorcento() {
      setupIngredienteSimples(new BigDecimal("100.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "1", "50"), usuario);

      assertThat(resp.getPrecoSugerido()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Margem 0%: preco sugerido = custo total")
    void margemZero_precoIgualCusto() {
      setupIngredienteSimples(new BigDecimal("80.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "1", "0"), usuario);

      assertThat(resp.getPrecoSugerido()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("Margem real calculada corretamente: custo=70, margem=30% -> real~30.0")
    void margemReal_calculada() {
      setupIngredienteSimples(new BigDecimal("70.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "1", "30"), usuario);

      // 70 / 0.70 = 100 -> margem = (100-70)/100*100 = 30.0
      assertThat(resp.getMargem()).isEqualByComparingTo("30.0");
    }

    @Test
    @DisplayName("Custo zero: preco sugerido = 0 sem ArithmeticException")
    void custoZero_precoSugeridoZero_semExcecao() {
      setupIngredienteSimples(BigDecimal.ZERO, unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "10", "0", "0", "0", "fixo", "1", "30"), usuario);

      assertThat(resp.getPrecoSugerido()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(resp.getMargem()).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }

  @Nested
  @DisplayName("calcularCustos() — custo e preco por unidade")
  class PorUnidadeTest {

    @Test
    @DisplayName("Rendimento 10: custoTotal=100 -> custoTotalPorUnidade=10.00")
    void custoTotalPorUnidade() {
      setupIngredienteSimples(new BigDecimal("100.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "10", "0"), usuario);

      assertThat(resp.getCustoTotalPorUnidade()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Rendimento 12: precoSugerido=200 (margem 50%) -> precoSugeridoPorUnidade=16.67")
    void precoSugeridoPorUnidade() {
      setupIngredienteSimples(new BigDecimal("100.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "12", "50"), usuario);

      assertThat(resp.getPrecoSugeridoPorUnidade()).isEqualByComparingTo("16.67");
    }

    @Test
    @DisplayName("Rendimento retornado corretamente na response")
    void rendimentoRetornado() {
      setupIngredienteSimples(new BigDecimal("10.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "5", null), usuario);

      assertThat(resp.getRendimentoQuantidade()).isEqualByComparingTo("5");
    }
  }

  @Nested
  @DisplayName("calcularCustos() — cenario completo")
  class CenarioCompletoTest {

    @Test
    @DisplayName("Bolo: farinha+manteiga + mao-de-obra + 10% fixos + margem 40%")
    void bolo_cenarioCompleto() {
      UUID ing2Id = UUID.randomUUID();
      IngredienteJpaEntity farinha = ingrediente(ING_ID, "Farinha", new BigDecimal("0.01"), unitG);
      IngredienteJpaEntity manteiga =
          ingrediente(ing2Id, "Manteiga", new BigDecimal("0.05"), unitG);

      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(farinha));
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ing2Id, WORKSPACE_ID))
          .thenReturn(Optional.of(manteiga));
      when(unitRepository.findByIdAndDeletedAtIsNull(UNIT_G_ID)).thenReturn(Optional.of(unitG));
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CalcularCustosRequest req = new CalcularCustosRequest();
      req.setIngredientes(List.of(item(ING_ID, UNIT_G_ID, "500"), item(ing2Id, UNIT_G_ID, "200")));
      req.setRendimentoQuantidade(new BigDecimal("10"));
      req.setMaoDeObraValorHora(new BigDecimal("30"));
      req.setTempoPreparoMinutos(new BigDecimal("60"));
      req.setCustosFixosValor(new BigDecimal("10"));
      req.setCustosFixosTipo("percentual");
      req.setMargemDesejada(new BigDecimal("40"));

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("15.00");
      assertThat(resp.getCustoMaoDeObra()).isEqualByComparingTo("30.00");
      // 10% de 15 = 1.50
      assertThat(resp.getCustosFixos()).isEqualByComparingTo("1.50");
      // 15 + 30 + 1.50 = 46.50
      assertThat(resp.getCustoTotal()).isEqualByComparingTo("46.50");
      // 46.50 / 0.60 = 77.50
      assertThat(resp.getPrecoSugerido()).isEqualByComparingTo("77.50");
      // 46.50 / 10 = 4.65
      assertThat(resp.getCustoTotalPorUnidade()).isEqualByComparingTo("4.65");
      // 77.50 / 10 = 7.75
      assertThat(resp.getPrecoSugeridoPorUnidade()).isEqualByComparingTo("7.75");
    }

    @Test
    @DisplayName("Campos de entrada ecoados na response")
    void camposEntradaEcoados() {
      setupIngredienteSimples(new BigDecimal("10.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "25", "45", "5", "fixo", "1", "35"), usuario);

      assertThat(resp.getMaoDeObraValorHora()).isEqualByComparingTo("25");
      assertThat(resp.getTempoPreparoMinutos()).isEqualByComparingTo("45");
      assertThat(resp.getCustosFixosValor()).isEqualByComparingTo("5");
      assertThat(resp.getCustosFixosTipo()).isEqualTo("fixo");
      assertThat(resp.getMargemUtilizada()).isEqualByComparingTo("35");
    }
  }

  @Nested
  @DisplayName("calcularCustos() — erros e fronteiras")
  class CalcularCustosErrosTest {

    @Test
    @DisplayName("Ingrediente nao encontrado lanca ReceitaException")
    void ingredienteNaoEncontrado_lancaExcecao() {
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(
              any(UUID.class), eq(WORKSPACE_ID)))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  sut.calcularCustos(
                      reqSimples(
                          UUID.randomUUID(), UNIT_G_ID, "100", "0", "0", "0", "fixo", "1", null),
                      usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Ingrediente não encontrado");
    }

    @Test
    @DisplayName("Unidade nao encontrada lanca ReceitaException")
    void unidadeNaoEncontrada_lancaExcecao() {
      IngredienteJpaEntity ing = ingrediente(ING_ID, "X", new BigDecimal("1.00"), unitG);
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(ing));
      when(unitRepository.findByIdAndDeletedAtIsNull(UNIT_G_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  sut.calcularCustos(
                      reqSimples(ING_ID, UNIT_G_ID, "10", "0", "0", "0", "fixo", "1", null),
                      usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Unidade não encontrada");
    }

    @Test
    @DisplayName("Workspace ausente no token lanca ReceitaException")
    void workspaceAusente_lancaExcecao() {
      UsuarioAutenticado semWs =
          new UsuarioAutenticado(USER_ID.toString(), "x@x.com", "X", null, Role.owner, Plan.free);

      assertThatThrownBy(
              () ->
                  sut.calcularCustos(
                      reqSimples(
                          UUID.randomUUID(), UNIT_G_ID, "1", "0", "0", "0", "fixo", "1", null),
                      semWs))
          .isInstanceOf(ReceitaException.class);
    }

    @Test
    @DisplayName("Workspace em branco no token lanca ReceitaException")
    void workspaceEmBranco_lancaExcecao() {
      UsuarioAutenticado semWs =
          new UsuarioAutenticado(USER_ID.toString(), "x@x.com", "X", "  ", Role.owner, Plan.free);

      assertThatThrownBy(
              () ->
                  sut.calcularCustos(
                      reqSimples(
                          UUID.randomUUID(), UNIT_G_ID, "1", "0", "0", "0", "fixo", "1", null),
                      semWs))
          .isInstanceOf(ReceitaException.class);
    }
  }

  // =========================================================================
  // calcularCustos() — rendimento aprimorado (peso/volume por unidade/porção)
  // =========================================================================

  @Nested
  @DisplayName("calcularCustos() — rendimento aprimorado")
  class RendimentoAprimoradoTest {

    // ── Sem rendimentoUnidadeId: campos aprimorados devem ser null ──────────

    @Test
    @DisplayName("Sem rendimentoUnidadeId: nenhum campo aprimorado é calculado")
    void semRendimentoUnidadeId_camposAprimoradosNull() {
      setupIngredienteSimples(new BigDecimal("10.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      // reqSimples não preenche rendimentoUnidadeId
      CustosCalculadosResponse resp =
          sut.calcularCustos(
              reqSimples(ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "450", null), usuario);

      assertThat(resp.getCustoPorGramaOuMl()).isNull();
      assertThat(resp.getPrecoPorGramaOuMl()).isNull();
      assertThat(resp.getNumeroPorcoesUnidades()).isNull();
      assertThat(resp.getCustoPorPorcaoOuUnidade()).isNull();
      assertThat(resp.getPrecoPorPorcaoOuUnidade()).isNull();
    }

    // ── Rendimento em unidade (type=unit): campos de peso/ml devem ser null ─

    @Test
    @DisplayName("Rendimento em 'un' (tipo unit): campos de grama/ml e porções são null")
    void rendimentoEmUnidade_camposGramaOuMlNull() {
      setupIngredienteSimples(new BigDecimal("1.00"), unitUn);
      mockUnidade(UNIT_UN_ID, unitUn);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_UN_ID, UNIT_UN_ID))
          .thenReturn(Optional.empty());

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID, UNIT_UN_ID, "1", "0", "0", "0", "fixo", "30", null, UNIT_UN_ID, null, null);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getCustoPorGramaOuMl()).isNull();
      assertThat(resp.getPrecoPorGramaOuMl()).isNull();
      assertThat(resp.getNumeroPorcoesUnidades()).isNull();
    }

    // ── Rendimento em peso (g), sem pesoPorUnidade ──────────────────────────

    @Test
    @DisplayName("Rendimento 450g, custo=R$9.00: custoPorGrama = 0.02")
    void rendimentoPeso_semPesoPorUnidade_custoPorGrama() {
      setupIngredienteSimples(new BigDecimal("0.02"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());
      // base unit para weight = g
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.of(unitG));
      // fator g -> g (mesma unidade, retorna 1)
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID, UNIT_G_ID, "450", "0", "0", "0", "fixo", "450", null, UNIT_G_ID, null, null);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 450 * 0.02 = 9.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("9.00");
      // 9.00 / 450g = 0.02 por grama
      assertThat(resp.getCustoPorGramaOuMl()).isEqualByComparingTo("0.020000");
      assertThat(resp.getNumeroPorcoesUnidades()).isNull();
      assertThat(resp.getCustoPorPorcaoOuUnidade()).isNull();
    }

    @Test
    @DisplayName("Rendimento 0.450kg, custo=R$9.00: custoPorGrama = 0.02 (conversão kg→g)")
    void rendimentoEmKg_custoPorGrama_comConversao() {
      // Ingrediente custo por grama
      setupIngredienteSimples(new BigDecimal("0.02"), unitG);
      mockUnidade(UNIT_KG_ID, unitKg);
      mockConversao(UNIT_KG_ID, UNIT_G_ID, new BigDecimal("1000.000000"));
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.of(unitG));
      // Para calcularRendimentoAprimorado: fator kg -> g = 1000
      // (already mocked above via mockConversao)

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_KG_ID,
              "0.45",
              "0",
              "0",
              "0",
              "fixo",
              "0.450",
              null,
              UNIT_KG_ID,
              null,
              null);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 0.45 kg * fator 1000 = 450g de ingrediente no cálculo
      // custoPorGrama = custoTotal / (0.450 * 1000) = custoTotal / 450
      // custoIngredientes = 0.45 * 1000 * 0.02 = 9.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("9.00");
      assertThat(resp.getCustoPorGramaOuMl()).isEqualByComparingTo("0.020000");
    }

    // ── Rendimento em peso (g) + pesoPorUnidade ─────────────────────────────

    @Test
    @DisplayName("Rendimento 450g + 15g por brigadeiro: 30 unidades calculadas")
    void rendimentoPeso_comPesoPorUnidade_calculaNumeroUnidades() {
      setupIngredienteSimples(new BigDecimal("0.02"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.of(unitG));

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_G_ID,
              "450",
              "0",
              "0",
              "0",
              "fixo",
              "450",
              null,
              UNIT_G_ID,
              new BigDecimal("15"),
              UNIT_G_ID);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 450g / 15g = 30 unidades
      assertThat(resp.getNumeroPorcoesUnidades()).isEqualByComparingTo("30.00");
      // custo = 450 * 0.02 = 9.00; por unidade = 9.00 / 30 = 0.30
      assertThat(resp.getCustoPorPorcaoOuUnidade()).isEqualByComparingTo("0.30");
    }

    @Test
    @DisplayName("Rendimento 450g + pesoPorUnidade 15g: precoSugerido por unidade calculado")
    void rendimentoPeso_comPesoPorUnidade_precoSugeridoPorUnidade() {
      setupIngredienteSimples(new BigDecimal("0.02"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.of(unitG));

      // custo = 9.00, margem 30% -> precoSugerido = 9.00 / 0.70 = 12.86
      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_G_ID,
              "450",
              "0",
              "0",
              "0",
              "fixo",
              "450",
              "30",
              UNIT_G_ID,
              new BigDecimal("15"),
              UNIT_G_ID);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 30 brigadeiros, precoSugerido/30
      assertThat(resp.getNumeroPorcoesUnidades()).isEqualByComparingTo("30.00");
      assertThat(resp.getPrecoPorPorcaoOuUnidade())
          .isEqualByComparingTo(
              resp.getPrecoSugerido()
                  .divide(new BigDecimal("30"), 2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Rendimento 450g + 0.015kg por unidade (peso em kg): 30 unidades com conversão")
    void rendimentoPeso_pesoPorUnidadeEmKg_conversao30Unidades() {
      setupIngredienteSimples(new BigDecimal("0.02"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      // também precisamos resolver a unidade do pesoPorUnidade (kg)
      mockUnidade(UNIT_KG_ID, unitKg);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.of(unitG));
      // pesoPorUnidade unit = kg; rendUnit = g -> fator kg->g = 1000
      mockConversao(UNIT_KG_ID, UNIT_G_ID, new BigDecimal("1000.000000"));

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_G_ID,
              "450",
              "0",
              "0",
              "0",
              "fixo",
              "450",
              null,
              UNIT_G_ID,
              new BigDecimal("0.015"),
              UNIT_KG_ID);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 0.015kg * 1000 = 15g por unidade; 450g / 15g = 30 unidades
      assertThat(resp.getNumeroPorcoesUnidades()).isEqualByComparingTo("30.00");
    }

    // ── Rendimento em volume (ml), sem pesoPorUnidade ───────────────────────

    @Test
    @DisplayName("Rendimento 1000ml, custo=R$5.00: custoPorMl = 0.005")
    void rendimentoVolume_semPesoPorUnidade_custoPorMl() {
      setupIngredienteSimples(new BigDecimal("0.005"), unitMl);
      mockUnidade(UNIT_ML_ID, unitMl);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_ML_ID, UNIT_ML_ID))
          .thenReturn(Optional.empty());
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.volume))
          .thenReturn(Optional.of(unitMl));

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_ML_ID,
              "1000",
              "0",
              "0",
              "0",
              "fixo",
              "1000",
              null,
              UNIT_ML_ID,
              null,
              null);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 1000 * 0.005 = 5.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("5.00");
      assertThat(resp.getCustoPorGramaOuMl()).isEqualByComparingTo("0.005000");
      assertThat(resp.getNumeroPorcoesUnidades()).isNull();
    }

    @Test
    @DisplayName("Rendimento 1L, custo=R$5.00: custoPorMl = 0.005 (conversão L→ml)")
    void rendimentoEmLitro_custoPorMl_comConversao() {
      setupIngredienteSimples(new BigDecimal("0.005"), unitMl);
      mockUnidade(UNIT_L_ID, unitL);
      mockConversao(UNIT_L_ID, UNIT_ML_ID, new BigDecimal("1000.000000"));
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.volume))
          .thenReturn(Optional.of(unitMl));

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID, UNIT_L_ID, "1", "0", "0", "0", "fixo", "1", null, UNIT_L_ID, null, null);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // custo: 1L * 1000 * 0.005 = 5.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("5.00");
      // custoPorMl = 5.00 / (1 * 1000) = 0.005
      assertThat(resp.getCustoPorGramaOuMl()).isEqualByComparingTo("0.005000");
    }

    // ── Rendimento em volume + pesoPorUnidade ────────────────────────────────

    @Test
    @DisplayName("Rendimento 1000ml + 50ml por porção: 20 porções calculadas")
    void rendimentoVolume_comPesoPorUnidade_calculaNumeroPorcoes() {
      setupIngredienteSimples(new BigDecimal("0.005"), unitMl);
      mockUnidade(UNIT_ML_ID, unitMl);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_ML_ID, UNIT_ML_ID))
          .thenReturn(Optional.empty());
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.volume))
          .thenReturn(Optional.of(unitMl));

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_ML_ID,
              "1000",
              "0",
              "0",
              "0",
              "fixo",
              "1000",
              null,
              UNIT_ML_ID,
              new BigDecimal("50"),
              UNIT_ML_ID);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 1000ml / 50ml = 20 porções
      assertThat(resp.getNumeroPorcoesUnidades()).isEqualByComparingTo("20.00");
      // custo = 5.00; por porção = 5.00 / 20 = 0.25
      assertThat(resp.getCustoPorPorcaoOuUnidade()).isEqualByComparingTo("0.25");
    }

    @Test
    @DisplayName("Rendimento 1L + 50ml por porção: 20 porções com conversão L→ml")
    void rendimentoEmLitro_porcaoEmMl_conversao20Porcoes() {
      setupIngredienteSimples(new BigDecimal("0.005"), unitMl);
      mockUnidade(UNIT_L_ID, unitL);
      // também precisamos resolver a unidade do pesoPorUnidade (ml)
      mockUnidade(UNIT_ML_ID, unitMl);
      mockConversao(UNIT_L_ID, UNIT_ML_ID, new BigDecimal("1000.000000"));
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.volume))
          .thenReturn(Optional.of(unitMl));
      // pesoPorUnidade em ml, rendUnit em L -> fator ml->L necessário
      // resolverFatorConversao(unitMl, unitL) precisa de conversão ml->L
      mockConversao(UNIT_ML_ID, UNIT_L_ID, new BigDecimal("0.001000"));

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_L_ID,
              "1",
              "0",
              "0",
              "0",
              "fixo",
              "1",
              null,
              UNIT_L_ID,
              new BigDecimal("50"),
              UNIT_ML_ID);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 50ml * 0.001 = 0.05L por porção; 1L / 0.05L = 20 porções
      assertThat(resp.getNumeroPorcoesUnidades()).isEqualByComparingTo("20.00");
    }

    // ── Validações de divisão por zero e robustez ───────────────────────────

    @Test
    @DisplayName("Unidade de rendimento nao encontrada: campos aprimorados null (sem excecao)")
    void rendimentoUnidadeNaoEncontrada_camposNull() {
      setupIngredienteSimples(new BigDecimal("1.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());
      UUID unidadeInexistente = UUID.randomUUID();
      // não mocka findByIdAndDeletedAtIsNull para unidadeInexistente → retorna empty

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_G_ID,
              "1",
              "0",
              "0",
              "0",
              "fixo",
              "450",
              null,
              unidadeInexistente,
              null,
              null);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getCustoPorGramaOuMl()).isNull();
      assertThat(resp.getNumeroPorcoesUnidades()).isNull();
    }

    @Test
    @DisplayName("Base unit nao encontrada para peso: custoPorGrama fica null")
    void baseUnitNaoEncontrada_custoPorGramaNull() {
      setupIngredienteSimples(new BigDecimal("1.00"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.empty());

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID, UNIT_G_ID, "1", "0", "0", "0", "fixo", "450", null, UNIT_G_ID, null, null);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getCustoPorGramaOuMl()).isNull();
    }

    @Test
    @DisplayName("pesoPorUnidade zero: numeroPorcoesUnidades fica null (evita divisao por zero)")
    void pesoPorUnidadeZero_numeroPorcoesNull() {
      setupIngredienteSimples(new BigDecimal("0.02"), unitG);
      mockUnidade(UNIT_G_ID, unitG);
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.of(unitG));

      CalcularCustosRequest req =
          reqSimplesComUnidadeRendimento(
              ING_ID,
              UNIT_G_ID,
              "450",
              "0",
              "0",
              "0",
              "fixo",
              "450",
              null,
              UNIT_G_ID,
              BigDecimal.ZERO,
              UNIT_G_ID);

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getNumeroPorcoesUnidades()).isNull();
      assertThat(resp.getCustoPorPorcaoOuUnidade()).isNull();
    }

    // ── criar() com pesoPorUnidade ────────────────────────────────────────────

    @Test
    @DisplayName(
        "Criar receita com pesoPorUnidade: campos persistidos e response inclui"
            + " numeroPorcoesUnidades")
    void criar_comPesoPorUnidade_responseIncluidaNumeroPorcoes() {
      CriarReceitaRequest req = criarRequest("Brigadeiro", null, "rascunho");
      req.setRendimentoQuantidade(new BigDecimal("450"));
      req.setRendimentoUnidadeId(UNIT_G_ID);
      req.setPesoPorUnidade(new BigDecimal("15"));
      req.setPesoPorUnidadeUnidadeId(UNIT_G_ID);

      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();
      when(unitRepository.findFirstByTypeAndIsBaseTrueAndDeletedAtIsNull(UnitType.weight))
          .thenReturn(Optional.of(unitG));
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      ProdutoJpaEntity saved =
          ProdutoJpaEntity.builder()
              .id(PRODUCT_ID)
              .name("Brigadeiro")
              .status(RecipeStatus.rascunho)
              .sellingPrice(BigDecimal.ZERO)
              .yieldQuantity(new BigDecimal("450"))
              .yieldUnit(unitG)
              .calculatedCost(BigDecimal.ZERO)
              .suggestedPrice(BigDecimal.ZERO)
              .unitWeight(new BigDecimal("15"))
              .unitWeightUnit(unitG)
              .build();
      saved.setActive(true);
      when(productRepository.save(any())).thenReturn(saved);

      ReceitaResponse resp = sut.criar(req, usuario);

      assertThat(resp.getPesoPorUnidade()).isEqualByComparingTo("15");
      assertThat(resp.getPesoPorUnidadeUnidadeSimbolo()).isEqualTo("g");
      // 450g / 15g = 30 unidades
      assertThat(resp.getNumeroPorcoesUnidades()).isEqualByComparingTo("30.00");
    }
  }

  // =========================================================================
  // calcularCustos() — receita como ingrediente
  // =========================================================================

  @Nested
  @DisplayName("calcularCustos() — receita como ingrediente")
  class ReceitaComoIngredienteCalcularTest {

    @Test
    @DisplayName("Custo = quantidade × (precoFinal / rendimentoQuantidade)")
    void custoSubReceita_formulaCorreta() {
      // precoFinal=50, rendimento=20 → precoPorUnidade=2.50; 5 × 2.50 = 12.50
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Creme", new BigDecimal("50"), new BigDecimal("20"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CalcularCustosRequest req = reqApenasSubReceitas(SUB_RECEITA_ID, new BigDecimal("5"));

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("12.50");
      assertThat(resp.getCustoTotal()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("Response inclui lista receitasComoIngredientes com precoPorUnidade e custo")
    void response_contemListaReceitasComoIngredientes() {
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Recheio", new BigDecimal("40"), new BigDecimal("8"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CalcularCustosRequest req = reqApenasSubReceitas(SUB_RECEITA_ID, new BigDecimal("2"));

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getReceitasComoIngredientes()).hasSize(1);
      ReceitaComoIngredienteResponse ri = resp.getReceitasComoIngredientes().get(0);
      assertThat(ri.getReceitaId()).isEqualTo(SUB_RECEITA_ID.toString());
      assertThat(ri.getNome()).isEqualTo("Recheio");
      assertThat(ri.getQuantidade()).isEqualByComparingTo("2");
      // 40 / 8 = 5.00 por unidade
      assertThat(ri.getPrecoPorUnidade()).isEqualByComparingTo("5.0000");
      // 2 × 5.00 = 10.00
      assertThat(ri.getCustoCalculado()).isEqualByComparingTo("10.0000");
    }

    @Test
    @DisplayName("Ingrediente convencional + sub-receita: custo total é a soma correta")
    void ingredienteConvencionalMaisSubReceita_somaCorreta() {
      // ingrediente: 500g × R$0.01/g = 5.00
      IngredienteJpaEntity farinha = ingrediente(ING_ID, "Farinha", new BigDecimal("0.01"), unitG);
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(farinha));
      when(unitRepository.findByIdAndDeletedAtIsNull(UNIT_G_ID)).thenReturn(Optional.of(unitG));
      lenient()
          .when(unitConversionRepository.findByFromUnitIdAndToUnitId(UNIT_G_ID, UNIT_G_ID))
          .thenReturn(Optional.empty());

      // sub-receita: 3 × (R$30/10) = 3 × 3.00 = 9.00
      ProdutoJpaEntity sub =
          subReceita(
              SUB_RECEITA_ID, "Cobertura", new BigDecimal("30"), new BigDecimal("10"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CalcularCustosRequest req = new CalcularCustosRequest();
      req.setIngredientes(List.of(item(ING_ID, UNIT_G_ID, "500")));
      req.setReceitasComoIngredientes(
          List.of(receitaComoIngredienteItem(SUB_RECEITA_ID, new BigDecimal("3"))));
      req.setRendimentoQuantidade(new BigDecimal("1"));
      req.setMaoDeObraValorHora(BigDecimal.ZERO);
      req.setTempoPreparoMinutos(BigDecimal.ZERO);
      req.setCustosFixosValor(BigDecimal.ZERO);
      req.setCustosFixosTipo("fixo");

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 5.00 + 9.00 = 14.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("14.00");
    }

    @Test
    @DisplayName("Apenas sub-receitas, sem ingredientes convencionais: não lança exceção")
    void apenasSubReceitas_semIngredientesConvencionais_funciona() {
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Massa", new BigDecimal("20"), new BigDecimal("10"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      // ingredientes = null → coalesce para List.of()
      CalcularCustosRequest req = reqApenasSubReceitas(SUB_RECEITA_ID, new BigDecimal("2"));

      assertThatCode(() -> sut.calcularCustos(req, usuario)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Ingredientes e sub-receitas ambos vazios: lança ReceitaException")
    void ambosVazios_lancaExcecao() {
      CalcularCustosRequest req = new CalcularCustosRequest();
      req.setIngredientes(List.of());
      req.setReceitasComoIngredientes(List.of());
      req.setRendimentoQuantidade(new BigDecimal("1"));
      req.setMaoDeObraValorHora(BigDecimal.ZERO);
      req.setTempoPreparoMinutos(BigDecimal.ZERO);
      req.setCustosFixosValor(BigDecimal.ZERO);
      req.setCustosFixosTipo("fixo");

      assertThatThrownBy(() -> sut.calcularCustos(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("ao menos um ingrediente");
    }

    @Test
    @DisplayName("Ingredientes null e sub-receitas null: lança ReceitaException")
    void ambosNull_lancaExcecao() {
      CalcularCustosRequest req = new CalcularCustosRequest();
      req.setIngredientes(null);
      req.setReceitasComoIngredientes(null);
      req.setRendimentoQuantidade(new BigDecimal("1"));
      req.setMaoDeObraValorHora(BigDecimal.ZERO);
      req.setTempoPreparoMinutos(BigDecimal.ZERO);
      req.setCustosFixosValor(BigDecimal.ZERO);
      req.setCustosFixosTipo("fixo");

      assertThatThrownBy(() -> sut.calcularCustos(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("ao menos um ingrediente");
    }

    @Test
    @DisplayName("Sub-receita não encontrada no workspace: lança ReceitaException")
    void subReceitaNaoEncontrada_lancaExcecao() {
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.empty());

      CalcularCustosRequest req = reqApenasSubReceitas(SUB_RECEITA_ID, new BigDecimal("1"));

      assertThatThrownBy(() -> sut.calcularCustos(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("Receita-ingrediente não encontrada");
    }

    @Test
    @DisplayName(
        "Sub-receita com rendimentoQuantidade zero: precoPorUnidade=0, sem ArithmeticException")
    void subReceitaRendimentoZero_custoZero_semExcecao() {
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Massa", new BigDecimal("50"), BigDecimal.ZERO, unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CalcularCustosRequest req = reqApenasSubReceitas(SUB_RECEITA_ID, new BigDecimal("5"));

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Sub-receita com precoFinal null: custo tratado como zero")
    void subReceitaPrecoFinalNull_custoZero() {
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Massa", null, new BigDecimal("10"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CalcularCustosRequest req = reqApenasSubReceitas(SUB_RECEITA_ID, new BigDecimal("3"));

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Múltiplas sub-receitas: soma de todos os custos")
    void multiplosSubReceitas_somaCorreta() {
      UUID sub2Id = UUID.randomUUID();
      // sub1: 2 × (R$20/5) = 2 × 4.00 = 8.00
      ProdutoJpaEntity sub1 =
          subReceita(SUB_RECEITA_ID, "Recheio", new BigDecimal("20"), new BigDecimal("5"), unitUn);
      // sub2: 3 × (R$30/10) = 3 × 3.00 = 9.00
      ProdutoJpaEntity sub2 =
          subReceita(sub2Id, "Cobertura", new BigDecimal("30"), new BigDecimal("10"), unitUn);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub1));
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(sub2Id, WORKSPACE_ID))
          .thenReturn(Optional.of(sub2));

      CalcularCustosRequest req = new CalcularCustosRequest();
      req.setReceitasComoIngredientes(
          List.of(
              receitaComoIngredienteItem(SUB_RECEITA_ID, new BigDecimal("2")),
              receitaComoIngredienteItem(sub2Id, new BigDecimal("3"))));
      req.setRendimentoQuantidade(new BigDecimal("1"));
      req.setMaoDeObraValorHora(BigDecimal.ZERO);
      req.setTempoPreparoMinutos(BigDecimal.ZERO);
      req.setCustosFixosValor(BigDecimal.ZERO);
      req.setCustosFixosTipo("fixo");

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // 8.00 + 9.00 = 17.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("17.00");
      assertThat(resp.getReceitasComoIngredientes()).hasSize(2);
    }

    @Test
    @DisplayName("Sub-receita custo entra na base para custos fixos percentuais")
    void subReceita_custoFixoPercentual_calculadoSobreTotal() {
      // sub-receita: 1 × (R$100/10) = 10.00
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Massa", new BigDecimal("100"), new BigDecimal("10"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CalcularCustosRequest req = reqApenasSubReceitas(SUB_RECEITA_ID, new BigDecimal("1"));
      req.setCustosFixosValor(new BigDecimal("20"));
      req.setCustosFixosTipo("percentual");

      CustosCalculadosResponse resp = sut.calcularCustos(req, usuario);

      // custo ingredientes = 10.00; 20% = 2.00
      assertThat(resp.getCustoIngredientes()).isEqualByComparingTo("10.00");
      assertThat(resp.getCustosFixos()).isEqualByComparingTo("2.00");
    }
  }

  // =========================================================================
  // criar() — receita como ingrediente
  // =========================================================================

  @Nested
  @DisplayName("criar() — receita como ingrediente")
  class CriarComReceitaComoIngredienteTest {

    @Test
    @DisplayName("Salva ProductRecipeIngredientJpaEntity ao criar receita com sub-receita")
    void criar_comSubReceita_salvaEntidade() {
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Creme", new BigDecimal("50"), new BigDecimal("20"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CriarReceitaRequest req = criarRequest("Bolo Recheado", null, "rascunho");
      req.setReceitasComoIngredientes(
          List.of(receitaComoIngredienteItem(SUB_RECEITA_ID, new BigDecimal("2"))));

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Bolo Recheado", RecipeStatus.rascunho);
      when(productRepository.save(any())).thenReturn(saved);
      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();

      ReceitaComoIngredienteJpaEntity riSaved = riEntity(saved, sub, new BigDecimal("2"));
      when(productRecipeIngredientRepository.save(any())).thenReturn(riSaved);

      sut.criar(req, usuario);

      verify(productRecipeIngredientRepository).save(any());
    }

    @Test
    @DisplayName("Response de criar inclui receitasComoIngredientes com custo calculado")
    void criar_comSubReceita_responseContemCusto() {
      // precoFinal=40, rendimento=8 → 2 × (40/8) = 2 × 5.00 = 10.00
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Recheio", new BigDecimal("40"), new BigDecimal("8"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));

      CriarReceitaRequest req = criarRequest("Torta", null, "rascunho");
      req.setReceitasComoIngredientes(
          List.of(receitaComoIngredienteItem(SUB_RECEITA_ID, new BigDecimal("2"))));

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Torta", RecipeStatus.rascunho);
      when(productRepository.save(any())).thenReturn(saved);
      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();

      ReceitaComoIngredienteJpaEntity riSaved = riEntity(saved, sub, new BigDecimal("2"));
      when(productRecipeIngredientRepository.save(any())).thenReturn(riSaved);

      ReceitaResponse resp = sut.criar(req, usuario);

      assertThat(resp.getReceitasComoIngredientes()).hasSize(1);
      ReceitaComoIngredienteResponse ri = resp.getReceitasComoIngredientes().get(0);
      assertThat(ri.getNome()).isEqualTo("Recheio");
      assertThat(ri.getPrecoPorUnidade()).isEqualByComparingTo("5.0000");
      assertThat(ri.getCustoCalculado()).isEqualByComparingTo("10.0000");
    }

    @Test
    @DisplayName("Auto-referência: receita não pode usar a si mesma como ingrediente")
    void criar_autoReferencia_lancaExcecao() {
      CriarReceitaRequest req = criarRequest("Brigadeiro", null, "rascunho");
      // a sub-receita tem o mesmo ID que será gerado para o produto salvo
      req.setReceitasComoIngredientes(
          List.of(receitaComoIngredienteItem(PRODUCT_ID, new BigDecimal("1"))));

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Brigadeiro", RecipeStatus.rascunho);
      when(productRepository.save(any())).thenReturn(saved);
      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);

      assertThatThrownBy(() -> sut.criar(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("si mesma");
    }

    @Test
    @DisplayName("Sub-receita de workspace diferente: lança ReceitaException")
    void criar_subReceitaOutroWorkspace_lancaExcecao() {
      // productRepository retorna empty para o workspace atual
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.empty());

      CriarReceitaRequest req = criarRequest("Bolo", null, "rascunho");
      req.setReceitasComoIngredientes(
          List.of(receitaComoIngredienteItem(SUB_RECEITA_ID, new BigDecimal("1"))));

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.rascunho);
      when(productRepository.save(any())).thenReturn(saved);
      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);

      assertThatThrownBy(() -> sut.criar(req, usuario))
          .isInstanceOf(ReceitaException.class)
          .hasMessageContaining("não pertence a este workspace");
    }

    @Test
    @DisplayName("Sem sub-receitas: response traz lista vazia em receitasComoIngredientes")
    void criar_semSubReceitas_retornaListaVazia() {
      CriarReceitaRequest req = criarRequest("Pão de Mel", null, "rascunho");

      ProdutoJpaEntity saved = produtoSalvo(PRODUCT_ID, "Pão de Mel", RecipeStatus.rascunho);
      when(productRepository.save(any())).thenReturn(saved);
      mockWorkspace();
      mockNomeUnico(false);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();

      ReceitaResponse resp = sut.criar(req, usuario);

      assertThat(resp.getReceitasComoIngredientes()).isEmpty();
    }
  }

  // =========================================================================
  // atualizar() — receita como ingrediente
  // =========================================================================

  @Nested
  @DisplayName("atualizar() — receita como ingrediente")
  class AtualizarComReceitaComoIngredienteTest {

    @Test
    @DisplayName("Atualizar chama deleteAllByProductId no repo de sub-receitas antes de re-inserir")
    void atualizar_deletaEReinsereSubReceitas() {
      ProdutoJpaEntity existente = produtoSalvo(PRODUCT_ID, "Bolo Antigo", RecipeStatus.rascunho);
      existente.setWorkspace(workspace);

      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Recheio", new BigDecimal("30"), new BigDecimal("6"), unitUn);
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(SUB_RECEITA_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(sub));
      mockNomeUnicoParaId(false, PRODUCT_ID);
      mockUnidade(UNIT_G_ID, unitG);
      mockUsuario();
      lenient()
          .when(productIngredientRepository.findAllByProductIdWithDetails(any()))
          .thenReturn(List.of());

      ReceitaComoIngredienteJpaEntity riSaved = riEntity(existente, sub, new BigDecimal("2"));
      when(productRecipeIngredientRepository.save(any())).thenReturn(riSaved);
      when(productRepository.save(any())).thenReturn(existente);

      CriarReceitaRequest req = criarRequest("Bolo Novo", null, "rascunho");
      req.setReceitasComoIngredientes(
          List.of(receitaComoIngredienteItem(SUB_RECEITA_ID, new BigDecimal("2"))));

      ReceitaResponse resp = sut.atualizar(PRODUCT_ID, req, usuario);

      verify(productRecipeIngredientRepository).deleteAllByProductId(PRODUCT_ID);
      verify(productRecipeIngredientRepository).save(any());
      assertThat(resp.getReceitasComoIngredientes()).hasSize(1);
    }
  }

  // =========================================================================
  // buscarPorId() — receita como ingrediente
  // =========================================================================

  @Nested
  @DisplayName("buscarPorId() — receita como ingrediente")
  class BuscarPorIdComReceitaComoIngredienteTest {

    @Test
    @DisplayName("Carrega sub-receitas da receita existente e mapeia custo corretamente")
    void buscarPorId_comSubReceita_mapeiaCusto() {
      ProdutoJpaEntity product = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.publicada);
      product.setWorkspace(workspace);

      // sub-receita: precoFinal=60, rendimento=12 → 1 × (60/12) = 5.00
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Creme", new BigDecimal("60"), new BigDecimal("12"), unitUn);
      ReceitaComoIngredienteJpaEntity ri = riEntity(product, sub, new BigDecimal("1"));

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(product));
      when(productIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of());
      when(productRecipeIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of(ri));

      ReceitaResponse resp = sut.buscarPorId(PRODUCT_ID, usuario);

      assertThat(resp.getReceitasComoIngredientes()).hasSize(1);
      ReceitaComoIngredienteResponse riResp = resp.getReceitasComoIngredientes().get(0);
      assertThat(riResp.getNome()).isEqualTo("Creme");
      assertThat(riResp.getPrecoPorUnidade()).isEqualByComparingTo("5.0000");
      assertThat(riResp.getCustoCalculado()).isEqualByComparingTo("5.0000");
    }

    @Test
    @DisplayName("Sem sub-receitas: retorna lista vazia")
    void buscarPorId_semSubReceitas_listaVazia() {
      ProdutoJpaEntity product = produtoSalvo(PRODUCT_ID, "Torta", RecipeStatus.publicada);
      product.setWorkspace(workspace);

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(product));
      when(productIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of());
      when(productRecipeIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of());

      ReceitaResponse resp = sut.buscarPorId(PRODUCT_ID, usuario);

      assertThat(resp.getReceitasComoIngredientes()).isEmpty();
    }
  }

  // =========================================================================
  // publicar() — receita como ingrediente
  // =========================================================================

  @Nested
  @DisplayName("publicar() — receita como ingrediente")
  class PublicarComReceitaComoIngredienteTest {

    @Test
    @DisplayName("Publicar carrega sub-receitas e as inclui na response")
    void publicar_comSubReceita_responseInclui() {
      ProdutoJpaEntity product = produtoSalvo(PRODUCT_ID, "Bolo", RecipeStatus.rascunho);
      product.setWorkspace(workspace);

      // sub-receita: precoFinal=20, rendimento=4 → 2 × 5.00 = 10.00
      ProdutoJpaEntity sub =
          subReceita(SUB_RECEITA_ID, "Massa", new BigDecimal("20"), new BigDecimal("4"), unitUn);
      ReceitaComoIngredienteJpaEntity ri = riEntity(product, sub, new BigDecimal("2"));

      when(productRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(PRODUCT_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(product));
      when(productIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of());
      when(productRecipeIngredientRepository.findAllByProductIdWithDetails(PRODUCT_ID))
          .thenReturn(List.of(ri));
      mockUsuario();

      ReceitaResponse resp = sut.publicar(PRODUCT_ID, usuario);

      assertThat(resp.getStatus()).isEqualTo(RecipeStatus.publicada);
      assertThat(resp.getReceitasComoIngredientes()).hasSize(1);
      assertThat(resp.getReceitasComoIngredientes().get(0).getCustoCalculado())
          .isEqualByComparingTo("10.0000");
    }
  }

  // =========================================================================
  // Helpers de setup
  // =========================================================================

  private void setupIngredienteSimples(BigDecimal costPerUnit, UnidadeJpaEntity baseUnit) {
    IngredienteJpaEntity ing = ingrediente(ING_ID, "Ingrediente", costPerUnit, baseUnit);
    when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
        .thenReturn(Optional.of(ing));
  }

  private void mockWorkspace() {
    when(workspaceRepository.findByIdAndDeletedAtIsNull(WORKSPACE_ID))
        .thenReturn(Optional.of(workspace));
  }

  private void mockNomeUnico(boolean existe) {
    when(productRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
            eq(WORKSPACE_ID), any()))
        .thenReturn(existe);
  }

  private void mockNomeUnicoParaId(boolean existe, UUID id) {
    when(productRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
            eq(WORKSPACE_ID), any()))
        .thenReturn(existe);
  }

  private void mockUnidade(UUID id, UnidadeJpaEntity unit) {
    lenient().when(unitRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(unit));
  }

  private void mockUsuario() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
  }

  private void mockConversao(UUID fromId, UUID toId, BigDecimal factor) {
    when(unitConversionRepository.findByFromUnitIdAndToUnitId(fromId, toId))
        .thenReturn(Optional.of(ConversaoUnidadeJpaEntity.builder().factor(factor).build()));
  }

  // =========================================================================
  // Builders de entidades
  // =========================================================================

  private static UnidadeJpaEntity unit(UUID id, String name, String symbol) {
    return UnidadeJpaEntity.builder().id(id).name(name).symbol(symbol).build();
  }

  private static UnidadeJpaEntity unit(UUID id, String name, String symbol, UnitType type) {
    return UnidadeJpaEntity.builder().id(id).name(name).symbol(symbol).type(type).build();
  }

  private static IngredienteJpaEntity ingrediente(
      UUID id, String nome, BigDecimal costPerUnit, UnidadeJpaEntity unit) {
    return IngredienteJpaEntity.builder()
        .id(id)
        .name(nome)
        .costPerUnit(costPerUnit)
        .unit(unit)
        .build();
  }

  private static ProdutoJpaEntity produtoSalvo(UUID id, String nome, RecipeStatus status) {
    ProdutoJpaEntity p =
        ProdutoJpaEntity.builder()
            .id(id)
            .name(nome)
            .status(status)
            .sellingPrice(BigDecimal.ZERO)
            .yieldQuantity(new BigDecimal("1"))
            .calculatedCost(BigDecimal.ZERO)
            .suggestedPrice(BigDecimal.ZERO)
            .build();
    p.setActive(true);
    return p;
  }

  private static CategoriaProdutoJpaEntity categoria(UUID id, String nome, String cor) {
    return CategoriaProdutoJpaEntity.builder().id(id).name(nome).color(cor).build();
  }

  private static IngredienteProdutoJpaEntity piEntity(
      ProdutoJpaEntity product, IngredienteJpaEntity ing, UnidadeJpaEntity unit, BigDecimal qty) {
    return IngredienteProdutoJpaEntity.builder()
        .id(UUID.randomUUID())
        .product(product)
        .ingredient(ing)
        .unit(unit)
        .quantity(qty)
        .build();
  }

  /**
   * Cria uma sub-receita (ProductJpaEntity) com os campos necessários para o cálculo de custo:
   * precoFinal, rendimentoQuantidade e yieldUnit.
   */
  private static ProdutoJpaEntity subReceita(
      UUID id,
      String nome,
      BigDecimal precoFinal,
      BigDecimal rendimento,
      UnidadeJpaEntity yieldUnit) {
    ProdutoJpaEntity p =
        ProdutoJpaEntity.builder()
            .id(id)
            .name(nome)
            .status(RecipeStatus.publicada)
            .sellingPrice(precoFinal)
            .yieldQuantity(rendimento)
            .yieldUnit(yieldUnit)
            .calculatedCost(BigDecimal.ZERO)
            .suggestedPrice(BigDecimal.ZERO)
            .build();
    p.setActive(true);
    return p;
  }

  private static ReceitaComoIngredienteJpaEntity riEntity(
      ProdutoJpaEntity product, ProdutoJpaEntity sub, BigDecimal quantidade) {
    return ReceitaComoIngredienteJpaEntity.builder()
        .id(UUID.randomUUID())
        .product(product)
        .subReceita(sub)
        .quantidade(quantidade)
        .build();
  }

  // =========================================================================
  // Builders de requests
  // =========================================================================

  private static IngredienteReceitaRequest item(UUID ingId, UUID unitId, String qtd) {
    IngredienteReceitaRequest r = new IngredienteReceitaRequest();
    r.setIngredienteId(ingId);
    r.setUnidadeId(unitId);
    r.setQuantidade(new BigDecimal(qtd));
    return r;
  }

  /** Monta um CriarReceitaRequest com um unico ingrediente e campos minimos. */
  private static CriarReceitaRequest criarRequest(String nome, UUID categoriaId, String status) {
    CriarReceitaRequest r = new CriarReceitaRequest();
    r.setNome(nome);
    r.setCategoriaId(categoriaId);
    r.setStatus(status);
    r.setRendimentoQuantidade(new BigDecimal("1"));
    r.setRendimentoUnidadeId(UNIT_G_ID);
    r.setPrecoFinal(BigDecimal.ZERO);
    return r;
  }

  private static CriarReceitaRequest criarRequestComIngrediente(
      String nome, UUID ingId, UUID unitId, BigDecimal qtd, String status) {
    CriarReceitaRequest r = criarRequest(nome, null, status);
    r.setIngredientes(List.of(item(ingId, unitId, qtd.toPlainString())));
    return r;
  }

  /**
   * Monta um CalcularCustosRequest apenas com sub-receitas (sem ingredientes convencionais).
   * Parâmetros de custo fixo, MO e margem zerados para isolar o comportamento da sub-receita.
   */
  private static CalcularCustosRequest reqApenasSubReceitas(UUID subId, BigDecimal quantidade) {
    CalcularCustosRequest req = new CalcularCustosRequest();
    req.setReceitasComoIngredientes(List.of(receitaComoIngredienteItem(subId, quantidade)));
    req.setRendimentoQuantidade(new BigDecimal("1"));
    req.setMaoDeObraValorHora(BigDecimal.ZERO);
    req.setTempoPreparoMinutos(BigDecimal.ZERO);
    req.setCustosFixosValor(BigDecimal.ZERO);
    req.setCustosFixosTipo("fixo");
    return req;
  }

  private static ReceitaComoIngredienteRequest receitaComoIngredienteItem(
      UUID receitaId, BigDecimal quantidade) {
    ReceitaComoIngredienteRequest r = new ReceitaComoIngredienteRequest();
    r.setReceitaId(receitaId);
    r.setQuantidade(quantidade);
    return r;
  }

  /**
   * Monta um CalcularCustosRequest com um unico ingrediente. Parametros como String para
   * simplicidade; null em margem usa o padrao 30%.
   */
  private static CalcularCustosRequest reqSimples(
      UUID ingId,
      UUID unitId,
      String qtd,
      String valorHora,
      String tempoMin,
      String custosFixosValor,
      String custosFixosTipo,
      String rendimento,
      String margem) {
    CalcularCustosRequest r = new CalcularCustosRequest();
    r.setIngredientes(List.of(item(ingId, unitId, qtd)));
    r.setRendimentoQuantidade(new BigDecimal(rendimento));
    r.setMaoDeObraValorHora(new BigDecimal(valorHora));
    r.setTempoPreparoMinutos(new BigDecimal(tempoMin));
    r.setCustosFixosValor(new BigDecimal(custosFixosValor));
    r.setCustosFixosTipo(custosFixosTipo);
    r.setMargemDesejada(margem != null ? new BigDecimal(margem) : null);
    return r;
  }

  /**
   * Monta um CalcularCustosRequest com suporte aos campos de rendimento aprimorado.
   *
   * @param rendimentoUnidadeId UUID da unidade de rendimento (pode ser null)
   * @param pesoPorUnidade peso/volume de cada unidade/porção (pode ser null)
   * @param pesoPorUnidadeUnidadeId UUID da unidade do pesoPorUnidade (pode ser null)
   */
  private static CalcularCustosRequest reqSimplesComUnidadeRendimento(
      UUID ingId,
      UUID unitId,
      String qtd,
      String valorHora,
      String tempoMin,
      String custosFixosValor,
      String custosFixosTipo,
      String rendimento,
      String margem,
      UUID rendimentoUnidadeId,
      BigDecimal pesoPorUnidade,
      UUID pesoPorUnidadeUnidadeId) {
    CalcularCustosRequest r =
        reqSimples(
            ingId,
            unitId,
            qtd,
            valorHora,
            tempoMin,
            custosFixosValor,
            custosFixosTipo,
            rendimento,
            margem);
    r.setRendimentoUnidadeId(rendimentoUnidadeId);
    r.setPesoPorUnidade(pesoPorUnidade);
    r.setPesoPorUnidadeUnidadeId(pesoPorUnidadeUnidadeId);
    return r;
  }
}
