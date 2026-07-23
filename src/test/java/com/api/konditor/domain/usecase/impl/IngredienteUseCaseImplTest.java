package com.api.konditor.domain.usecase.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.CriarIngredienteRequest;
import com.api.konditor.app.controller.response.IngredienteCardResponse;
import com.api.konditor.app.controller.response.IngredienteResponse;
import com.api.konditor.app.controller.response.PaginaResponse;
import com.api.konditor.app.exception.IngredienteException;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import com.api.konditor.infra.jpa.entity.HistoricoPrecoIngredienteJpaEntity;
import com.api.konditor.infra.jpa.entity.IngredienteJpaEntity;
import com.api.konditor.infra.jpa.entity.LogAuditoriaJpaEntity;
import com.api.konditor.infra.jpa.entity.UnidadeJpaEntity;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Testes unitários de {@link IngredienteUseCaseImpl} com ênfase no isolamento multi-tenant: toda
 * operação usa o {@code workspaceId} das claims do JWT, nunca dados de outro workspace.
 */
@ExtendWith(MockitoExtension.class)
class IngredienteUseCaseImplTest {

  private static final UUID WORKSPACE_ID = UUID.randomUUID();
  private static final UUID OUTRO_WORKSPACE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ING_ID = UUID.randomUUID();
  private static final UUID UNIT_KG_ID = UUID.randomUUID();

  @Mock private IngredientJpaRepository ingredientRepository;
  @Mock private IngredientCategoryJpaRepository categoryRepository;
  @Mock private IngredientPriceHistoryJpaRepository priceHistoryRepository;
  @Mock private UnitJpaRepository unitRepository;
  @Mock private WorkspaceJpaRepository workspaceRepository;
  @Mock private UserJpaRepository userRepository;
  @Mock private ProductIngredientJpaRepository productIngredientRepository;
  @Mock private UnitConversionJpaRepository unitConversionRepository;
  @Mock private AuditLogJpaRepository auditLogRepository;

  @InjectMocks private IngredienteUseCaseImpl sut;

  private UsuarioAutenticado usuario;
  private EspacoTrabalhoJpaEntity workspace;
  private UsuarioJpaEntity userEntity;
  private UnidadeJpaEntity unitKg;

  @BeforeEach
  void setUp() {
    usuario =
        new UsuarioAutenticado(
            USER_ID.toString(),
            "chef@konditor.io",
            "Chef Teste",
            WORKSPACE_ID.toString(),
            Role.owner,
            Plan.free);
    workspace = EspacoTrabalhoJpaEntity.builder().id(WORKSPACE_ID).nome("Doceria").build();
    userEntity = UsuarioJpaEntity.builder().id(USER_ID).email("chef@konditor.io").build();
    unitKg = UnidadeJpaEntity.builder().id(UNIT_KG_ID).name("Quilograma").symbol("kg").build();
  }

  private IngredienteJpaEntity ingrediente(UUID id, String nome) {
    return IngredienteJpaEntity.builder()
        .id(id)
        .workspace(workspace)
        .name(nome)
        .unit(unitKg)
        .costPerUnit(new BigDecimal("10.00"))
        .build();
  }

  private CriarIngredienteRequest requestValido() {
    CriarIngredienteRequest request = new CriarIngredienteRequest();
    request.setNome("Chocolate 70%");
    request.setUnidadeId(UNIT_KG_ID);
    request.setPrecoPorUnidade(new BigDecimal("55.90"));
    return request;
  }

  @Nested
  @DisplayName("Isolamento multi-tenant")
  class IsolamentoMultiTenant {

    @Test
    @DisplayName("Listagem consulta apenas o workspace das claims do token")
    void listar_usaWorkspaceIdDoToken() {
      when(ingredientRepository.findPageByWorkspaceId(eq(WORKSPACE_ID), any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(ingrediente(ING_ID, "Farinha"))));
      when(priceHistoryRepository.findMostRecentByIngredientIds(List.of(ING_ID)))
          .thenReturn(List.of());

      PaginaResponse<IngredienteCardResponse> pagina = sut.listar(usuario, null, 0, 20);

      assertThat(pagina.getConteudo()).hasSize(1);
      // A query é sempre executada com o workspaceId do token — nunca de parâmetro externo
      verify(ingredientRepository).findPageByWorkspaceId(eq(WORKSPACE_ID), any(PageRequest.class));
      verify(ingredientRepository, never())
          .findPageByWorkspaceId(eq(OUTRO_WORKSPACE_ID), any(PageRequest.class));
    }

    @Test
    @DisplayName("Ingrediente de outro workspace não é encontrado (escopo por workspaceId)")
    void buscarPorId_ingredienteDeOutroWorkspace_naoEhAcessivel() {
      // O ingrediente existe no banco, mas pertence a OUTRO workspace — a query escopada
      // pelo workspaceId do token não o retorna.
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> sut.buscarPorId(ING_ID, usuario))
          .isInstanceOf(IngredienteException.class)
          .hasMessageContaining("não encontrado");

      verify(ingredientRepository).findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID);
      verify(ingredientRepository, never())
          .findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, OUTRO_WORKSPACE_ID);
    }

    @Test
    @DisplayName("Usuário sem workspace no token (onboarding pendente) é rejeitado")
    void semWorkspaceNoToken_lancaExcecao() {
      UsuarioAutenticado semWorkspace =
          new UsuarioAutenticado(
              USER_ID.toString(), "chef@konditor.io", "Chef Teste", null, null, null);

      assertThatThrownBy(() -> sut.listar(semWorkspace, null, 0, 20))
          .isInstanceOf(IngredienteException.class)
          .hasMessageContaining("onboarding");

      verifyNoInteractions(ingredientRepository);
    }
  }

  @Nested
  @DisplayName("criar")
  class Criar {

    @Test
    @DisplayName("Cria o ingrediente no workspace do token e registra auditoria")
    void criar_comSucesso() {
      when(ingredientRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
              WORKSPACE_ID, "Chocolate 70%"))
          .thenReturn(false);
      when(workspaceRepository.findByIdAndExcluidoEmIsNull(WORKSPACE_ID))
          .thenReturn(Optional.of(workspace));
      when(unitRepository.findByIdAndDeletedAtIsNull(UNIT_KG_ID)).thenReturn(Optional.of(unitKg));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
      when(ingredientRepository.save(any(IngredienteJpaEntity.class)))
          .thenAnswer(
              inv -> {
                IngredienteJpaEntity e = inv.getArgument(0);
                e.setId(ING_ID);
                return e;
              });

      IngredienteResponse response = sut.criar(requestValido(), usuario);

      assertThat(response.getId()).isEqualTo(ING_ID.toString());
      assertThat(response.getNome()).isEqualTo("Chocolate 70%");
      assertThat(response.getPrecoPorUnidade()).isEqualByComparingTo("55.90");

      ArgumentCaptor<IngredienteJpaEntity> captor =
          ArgumentCaptor.forClass(IngredienteJpaEntity.class);
      verify(ingredientRepository).save(captor.capture());
      assertThat(captor.getValue().getWorkspace().getId()).isEqualTo(WORKSPACE_ID);

      verify(auditLogRepository).save(any(LogAuditoriaJpaEntity.class));
    }

    @Test
    @DisplayName("Nome duplicado no mesmo workspace é rejeitado")
    void criar_nomeDuplicado_lancaExcecao() {
      when(ingredientRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
              WORKSPACE_ID, "Chocolate 70%"))
          .thenReturn(true);

      assertThatThrownBy(() -> sut.criar(requestValido(), usuario))
          .isInstanceOf(IngredienteException.class)
          .hasMessageContaining("Já existe um ingrediente com o nome");

      verify(ingredientRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("listar — variação de preço")
  class VariacaoDePreco {

    @Test
    @DisplayName("Card expõe a variação percentual do último reajuste de preço")
    void listar_calculaVariacaoPercentual() {
      IngredienteJpaEntity ing = ingrediente(ING_ID, "Manteiga");
      HistoricoPrecoIngredienteJpaEntity historico =
          HistoricoPrecoIngredienteJpaEntity.builder()
              .ingredient(ing)
              .oldPrice(new BigDecimal("10.00"))
              .newPrice(new BigDecimal("12.50"))
              .build();

      when(ingredientRepository.findPageByWorkspaceId(eq(WORKSPACE_ID), any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(ing)));
      when(priceHistoryRepository.findMostRecentByIngredientIds(List.of(ING_ID)))
          .thenReturn(List.of(historico));

      PaginaResponse<IngredienteCardResponse> pagina = sut.listar(usuario, null, 0, 20);

      assertThat(pagina.getConteudo().get(0).getVariacaoPreco())
          .isEqualByComparingTo("25.00"); // (12.50 - 10.00) / 10.00 = +25%
    }
  }

  @Nested
  @DisplayName("atualizar")
  class Atualizar {

    @Test
    @DisplayName("Atualiza os campos e não recalcula receitas quando nenhuma usa o ingrediente")
    void atualizar_semReceitasAfetadas() {
      IngredienteJpaEntity existente = ingrediente(ING_ID, "Chocolate 70%");
      when(ingredientRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(ING_ID, WORKSPACE_ID))
          .thenReturn(Optional.of(existente));
      when(ingredientRepository.existsByWorkspaceIdAndNameIgnoreCaseAndDeletedAtIsNull(
              WORKSPACE_ID, "Chocolate 70%"))
          .thenReturn(true);
      when(unitRepository.findByIdAndDeletedAtIsNull(UNIT_KG_ID)).thenReturn(Optional.of(unitKg));
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
      when(productIngredientRepository.findAllByIngredientIdWithDetails(ING_ID))
          .thenReturn(List.of());

      IngredienteResponse response = sut.atualizar(ING_ID, requestValido(), usuario);

      assertThat(response.getPrecoPorUnidade()).isEqualByComparingTo("55.90");
      assertThat(existente.getCostPerUnit()).isEqualByComparingTo("55.90");
      verify(auditLogRepository).save(any(LogAuditoriaJpaEntity.class));
    }
  }
}
