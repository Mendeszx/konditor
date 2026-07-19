package com.api.konditor.domain.usecase.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.OnboardingRequest;
import com.api.konditor.app.controller.response.OnboardingResponse;
import com.api.konditor.app.exception.OnboardingException;
import com.api.konditor.domain.entity.User;
import com.api.konditor.domain.entity.Workspace;
import com.api.konditor.domain.entity.WorkspaceMember;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.jpa.entity.AssinaturaJpaEntity;
import com.api.konditor.infra.jpa.entity.DetalhesPlanoJpaEntity;
import com.api.konditor.infra.jpa.entity.EspacoTrabalhoJpaEntity;
import com.api.konditor.infra.jpa.entity.LogAuditoriaJpaEntity;
import com.api.konditor.infra.jpa.entity.MembroEspacoTrabalhoJpaEntity;
import com.api.konditor.infra.jpa.entity.PapelJpaEntity;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import com.api.konditor.infra.jpa.mapper.UserJpaMapper;
import com.api.konditor.infra.jpa.mapper.WorkspaceJpaMapper;
import com.api.konditor.infra.jpa.mapper.WorkspaceMemberJpaMapper;
import com.api.konditor.infra.jpa.repository.AuditLogJpaRepository;
import com.api.konditor.infra.jpa.repository.PlanDetailsJpaRepository;
import com.api.konditor.infra.jpa.repository.RoleJpaRepository;
import com.api.konditor.infra.jpa.repository.SubscriptionJpaRepository;
import com.api.konditor.infra.jpa.repository.UserJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceJpaRepository;
import com.api.konditor.infra.jpa.repository.WorkspaceMemberJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários de {@link OnboardingUseCaseImpl}: criação de workspace + vínculo owner +
 * assinatura free + auditoria, e a idempotência (usuário com workspace ativo não repete
 * onboarding).
 */
@ExtendWith(MockitoExtension.class)
class OnboardingUseCaseImplTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID WORKSPACE_ID = UUID.randomUUID();

  @Mock private UserJpaRepository userRepository;
  @Mock private WorkspaceJpaRepository workspaceRepository;
  @Mock private WorkspaceMemberJpaRepository workspaceMemberRepository;
  @Mock private SubscriptionJpaRepository subscriptionRepository;
  @Mock private PlanDetailsJpaRepository planDetailsRepository;
  @Mock private RoleJpaRepository roleRepository;
  @Mock private AuditLogJpaRepository auditLogRepository;
  @Mock private UserJpaMapper userMapper;
  @Mock private WorkspaceJpaMapper workspaceMapper;
  @Mock private WorkspaceMemberJpaMapper workspaceMemberMapper;

  @InjectMocks private OnboardingUseCaseImpl sut;

  private UsuarioAutenticado usuarioAutenticado;
  private OnboardingRequest request;
  private UsuarioJpaEntity usuarioJpa;
  private User usuarioDomain;

  @BeforeEach
  void setUp() {
    usuarioAutenticado =
        new UsuarioAutenticado(
            USER_ID.toString(), "chef@konditor.io", "Chef Teste", null, null, null);
    request = new OnboardingRequest("Doceria da Chef", "brl");
    usuarioJpa =
        UsuarioJpaEntity.builder().id(USER_ID).email("chef@konditor.io").nome("Chef Teste").build();
    usuarioDomain = User.builder().id(USER_ID).build();
  }

  @Test
  @DisplayName("Onboarding cria workspace, vincula owner, assina plano free e audita")
  void executar_comSucesso() {
    when(workspaceRepository.findAllByProprietario_IdAndExcluidoEmIsNull(USER_ID))
        .thenReturn(List.of());
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioJpa));
    when(userMapper.toDomain(usuarioJpa)).thenReturn(usuarioDomain);
    when(userMapper.toJpa(usuarioDomain)).thenReturn(usuarioJpa);

    DetalhesPlanoJpaEntity planoFree = DetalhesPlanoJpaEntity.builder().name("free").build();
    when(planDetailsRepository.findByName("free")).thenReturn(Optional.of(planoFree));
    when(roleRepository.findByNome("owner"))
        .thenReturn(Optional.of(PapelJpaEntity.builder().nome("owner").build()));

    EspacoTrabalhoJpaEntity workspaceJpa =
        EspacoTrabalhoJpaEntity.builder().nome("Doceria da Chef").moeda("BRL").build();
    when(workspaceMapper.toJpa(any(Workspace.class))).thenReturn(workspaceJpa);
    when(workspaceRepository.save(workspaceJpa))
        .thenAnswer(
            inv -> {
              EspacoTrabalhoJpaEntity ws = inv.getArgument(0);
              ws.setId(WORKSPACE_ID);
              return ws;
            });

    when(workspaceMemberMapper.toJpa(any(WorkspaceMember.class)))
        .thenReturn(new MembroEspacoTrabalhoJpaEntity());
    when(workspaceMemberRepository.save(any(MembroEspacoTrabalhoJpaEntity.class)))
        .thenAnswer(
            inv -> {
              MembroEspacoTrabalhoJpaEntity m = inv.getArgument(0);
              m.setId(UUID.randomUUID());
              return m;
            });
    when(subscriptionRepository.save(any(AssinaturaJpaEntity.class)))
        .thenAnswer(
            inv -> {
              AssinaturaJpaEntity a = inv.getArgument(0);
              a.setId(UUID.randomUUID());
              return a;
            });

    OnboardingResponse response = sut.executar(usuarioAutenticado, request);

    assertThat(response.getWorkspaceId()).isEqualTo(WORKSPACE_ID.toString());
    assertThat(response.getWorkspaceNome()).isEqualTo("Doceria da Chef");
    assertThat(response.getMoeda()).isEqualTo("BRL");
    assertThat(response.getRole()).isEqualTo(Role.owner);
    assertThat(response.getPlano()).isEqualTo(Plan.free);
    assertThat(response.getUsuario().getEmail()).isEqualTo("chef@konditor.io");

    // Workspace persistido com o proprietário e o plano free
    assertThat(workspaceJpa.getProprietario()).isSameAs(usuarioJpa);
    assertThat(workspaceJpa.getPlano()).isSameAs(planoFree);

    // Assinatura free ativa criada para o workspace
    verify(subscriptionRepository)
        .save(
            argThat(
                assinatura ->
                    assinatura.getWorkspace() == workspaceJpa
                        && assinatura.getPlan() == planoFree));

    // 3 registros de auditoria: Workspace, WorkspaceMember e Subscription
    verify(auditLogRepository, times(3)).save(any(LogAuditoriaJpaEntity.class));
  }

  @Test
  @DisplayName("Usuário com workspace ativo não pode refazer o onboarding (idempotência)")
  void executar_usuarioJaTemWorkspace_lancaExcecao() {
    when(workspaceRepository.findAllByProprietario_IdAndExcluidoEmIsNull(USER_ID))
        .thenReturn(List.of(EspacoTrabalhoJpaEntity.builder().id(WORKSPACE_ID).build()));

    assertThatThrownBy(() -> sut.executar(usuarioAutenticado, request))
        .isInstanceOf(OnboardingException.class)
        .hasMessageContaining("já possui um workspace ativo");

    verify(workspaceRepository, never()).save(any());
    verify(workspaceMemberRepository, never()).save(any());
    verify(subscriptionRepository, never()).save(any());
    verify(auditLogRepository, never()).save(any());
  }

  @Test
  @DisplayName("Seed do plano free ausente aborta o onboarding com erro claro")
  void executar_planoFreeAusente_lancaExcecao() {
    when(workspaceRepository.findAllByProprietario_IdAndExcluidoEmIsNull(USER_ID))
        .thenReturn(List.of());
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioJpa));
    when(userMapper.toDomain(usuarioJpa)).thenReturn(usuarioDomain);
    when(userMapper.toJpa(usuarioDomain)).thenReturn(usuarioJpa);
    when(planDetailsRepository.findByName("free")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.executar(usuarioAutenticado, request))
        .isInstanceOf(OnboardingException.class)
        .hasMessageContaining("plano");
  }
}
