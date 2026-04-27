package com.api.konditor.domain.usecase.impl;

import com.api.konditor.app.config.security.UsuarioAutenticado;
import com.api.konditor.app.controller.request.OnboardingRequest;
import com.api.konditor.app.controller.response.DadosUsuarioResponse;
import com.api.konditor.app.controller.response.OnboardingResponse;
import com.api.konditor.app.exception.OnboardingException;
import com.api.konditor.domain.entity.Subscription;
import com.api.konditor.domain.entity.User;
import com.api.konditor.domain.entity.Workspace;
import com.api.konditor.domain.entity.WorkspaceMember;
import com.api.konditor.domain.enuns.AuditOperation;
import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;
import com.api.konditor.domain.enuns.SubscriptionStatus;
import com.api.konditor.domain.usecase.OnboardingUseCase;
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
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação do caso de uso de onboarding.
 *
 * <p>Fluxo executado:
 *
 * <ol>
 *   <li>Valida que o usuário ainda não possui um workspace ativo (idempotência)
 *   <li>Busca a entidade de domínio {@link User} pelo ID do token
 *   <li>Constrói e persiste o {@link Workspace}
 *   <li>Vincula o usuário como {@code owner} do workspace ({@link WorkspaceMember})
 *   <li>Cria a {@link Subscription} gratuita para o workspace
 *   <li>Registra a operação no {@code audit_log}
 * </ol>
 *
 * <p>O use case manipula entidades de domínio e usa os mappers para converter para entidades JPA
 * antes de persistir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingUseCaseImpl implements OnboardingUseCase {

  private final UserJpaRepository userRepository;
  private final WorkspaceJpaRepository workspaceRepository;
  private final WorkspaceMemberJpaRepository workspaceMemberRepository;
  private final SubscriptionJpaRepository subscriptionRepository;
  private final PlanDetailsJpaRepository planDetailsRepository;
  private final RoleJpaRepository roleRepository;
  private final AuditLogJpaRepository auditLogRepository;

  private final UserJpaMapper userMapper;
  private final WorkspaceJpaMapper workspaceMapper;
  private final WorkspaceMemberJpaMapper workspaceMemberMapper;

  // =========================================================================
  // Caso de uso
  // =========================================================================

  @Override
  @Transactional
  public OnboardingResponse executar(
      UsuarioAutenticado usuarioAutenticado, OnboardingRequest request) {
    UUID userId = UUID.fromString(usuarioAutenticado.id());
    log.info(
        "[ONBOARDING] Iniciando onboarding para usuário id={} email={}",
        userId,
        usuarioAutenticado.email());

    // 1. Valida idempotência — usuário não pode ter workspace ativo
    validarSemWorkspaceAtivo(userId);

    // 2. Busca entidade de domínio do usuário
    User usuario = buscarUsuario(userId);
    UsuarioJpaEntity usuarioJpa = userMapper.toJpa(usuario);

    // 3. Cria e persiste o Workspace
    Workspace workspace = criarWorkspace(usuario, request);
    EspacoTrabalhoJpaEntity workspaceJpa = persistirWorkspace(workspace, usuarioJpa);
    log.info(
        "[ONBOARDING] Workspace criado id={} nome='{}' moeda={}",
        workspaceJpa.getId(),
        workspaceJpa.getNome(),
        workspaceJpa.getMoeda());

    // 4. Vincula usuário como owner
    WorkspaceMember member = criarMembro(workspace, usuario);
    MembroEspacoTrabalhoJpaEntity membroJpa = persistirMembro(member, workspaceJpa, usuarioJpa);
    log.info(
        "[ONBOARDING] Usuário id={} vinculado como owner no workspace id={}",
        userId,
        workspaceJpa.getId());

    // 5. Cria assinatura free
    Subscription subscription = criarSubscription(workspace);
    AssinaturaJpaEntity subscriptionJpa =
        persistirSubscription(subscription, workspaceJpa, usuarioJpa);
    log.info("[ONBOARDING] Assinatura free criada para workspace id={}", workspaceJpa.getId());

    // 6. Registra no audit log
    registrarAuditLog(
        workspaceJpa,
        "Workspace",
        workspaceJpa.getId(),
        AuditOperation.CREATE,
        "{\"event\":\"onboarding\",\"workspace\":\"" + workspaceJpa.getNome() + "\"}",
        usuarioJpa);
    registrarAuditLog(
        workspaceJpa,
        "WorkspaceMember",
        membroJpa.getId(),
        AuditOperation.CREATE,
        "{\"role\":\"owner\",\"userId\":\"" + userId + "\"}",
        usuarioJpa);
    registrarAuditLog(
        workspaceJpa,
        "Subscription",
        subscriptionJpa.getId(),
        AuditOperation.CREATE,
        "{\"plano\":\"free\",\"status\":\"active\"}",
        usuarioJpa);

    log.info(
        "[ONBOARDING] Onboarding concluído com sucesso para usuário id={} workspace id={}",
        userId,
        workspaceJpa.getId());

    return montarResposta(workspaceJpa, usuarioAutenticado);
  }

  // =========================================================================
  // Validação
  // =========================================================================

  /**
   * Garante idempotência: um usuário não pode iniciar onboarding se já possui um workspace ativo
   * (sem soft-delete).
   */
  private void validarSemWorkspaceAtivo(UUID userId) {
    boolean possuiWorkspace =
        !workspaceRepository.findAllByOwnerIdAndDeletedAtIsNull(userId).isEmpty();
    if (possuiWorkspace) {
      log.warn(
          "[ONBOARDING] Onboarding rejeitado — usuário id={} já possui workspace ativo", userId);
      throw new OnboardingException(
          "Usuário já possui um workspace ativo. Onboarding não pode ser refeito.");
    }
  }

  // =========================================================================
  // Construção das entidades de domínio
  // =========================================================================

  private User buscarUsuario(UUID userId) {
    UsuarioJpaEntity jpa =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new OnboardingException("Usuário não encontrado com id=" + userId));
    return userMapper.toDomain(jpa);
  }

  /** Constrói a entidade de domínio {@link Workspace} com os dados do request. */
  private Workspace criarWorkspace(User owner, OnboardingRequest request) {
    return Workspace.builder()
        .name(request.getNomeWorkspace().trim())
        .owner(owner)
        .currency(request.getMoeda().toUpperCase())
        .createdBy(owner.getId())
        .build();
  }

  /** Constrói a entidade de domínio {@link WorkspaceMember} — owner do workspace. */
  private WorkspaceMember criarMembro(Workspace workspace, User usuario) {
    return WorkspaceMember.builder()
        .workspace(workspace)
        .user(usuario)
        .role(Role.owner)
        .joinedAt(Instant.now())
        .createdBy(usuario)
        .build();
  }

  /** Constrói a entidade de domínio {@link Subscription} para o plano gratuito. */
  private Subscription criarSubscription(Workspace workspace) {
    return Subscription.builder()
        .workspace(workspace)
        .plan(Plan.free)
        .status(SubscriptionStatus.active)
        .startedAt(Instant.now())
        .build();
  }

  // =========================================================================
  // Persistência — converte domínio → JPA e salva
  // =========================================================================

  private EspacoTrabalhoJpaEntity persistirWorkspace(
      Workspace workspace, UsuarioJpaEntity ownerJpa) {
    DetalhesPlanoJpaEntity planFree = buscarPlanDetails(Plan.free);

    EspacoTrabalhoJpaEntity jpa = workspaceMapper.toJpa(workspace);
    jpa.setProprietario(ownerJpa);
    jpa.setPlano(planFree);

    return workspaceRepository.save(jpa);
  }

  private MembroEspacoTrabalhoJpaEntity persistirMembro(
      WorkspaceMember member, EspacoTrabalhoJpaEntity workspaceJpa, UsuarioJpaEntity usuarioJpa) {
    PapelJpaEntity roleOwner = buscarRole(Role.owner);

    MembroEspacoTrabalhoJpaEntity jpa = workspaceMemberMapper.toJpa(member);
    jpa.setEspacoTrabalho(workspaceJpa);
    jpa.setUsuario(usuarioJpa);
    jpa.setPapel(roleOwner);
    jpa.setConvidadoPor(null);
    jpa.setCriadoPor(usuarioJpa);

    return workspaceMemberRepository.save(jpa);
  }

  private AssinaturaJpaEntity persistirSubscription(
      Subscription subscription,
      EspacoTrabalhoJpaEntity workspaceJpa,
      UsuarioJpaEntity usuarioJpa) {
    DetalhesPlanoJpaEntity planFree = buscarPlanDetails(Plan.free);

    AssinaturaJpaEntity jpa =
        AssinaturaJpaEntity.builder()
            .workspace(workspaceJpa)
            .plan(planFree)
            .status(SubscriptionStatus.active)
            .startedAt(subscription.getStartedAt())
            .createdBy(usuarioJpa)
            .build();

    return subscriptionRepository.save(jpa);
  }

  // =========================================================================
  // Audit Log
  // =========================================================================

  private void registrarAuditLog(
      EspacoTrabalhoJpaEntity workspace,
      String entityName,
      UUID entityId,
      AuditOperation operation,
      String dataAfter,
      UsuarioJpaEntity performedBy) {
    LogAuditoriaJpaEntity entry =
        LogAuditoriaJpaEntity.builder()
            .workspace(workspace)
            .entityName(entityName)
            .entityId(entityId)
            .operation(operation)
            .dataAfter(dataAfter)
            .performedBy(performedBy)
            .performedAt(Instant.now())
            .build();

    auditLogRepository.save(entry);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DetalhesPlanoJpaEntity buscarPlanDetails(Plan plan) {
    return planDetailsRepository
        .findByName(plan.name())
        .orElseThrow(
            () ->
                new OnboardingException(
                    "Configuração de plano não encontrada: "
                        + plan.name()
                        + ". Verifique os dados de seed."));
  }

  private PapelJpaEntity buscarRole(Role role) {
    return roleRepository
        .findByName(role.name())
        .orElseThrow(
            () ->
                new OnboardingException(
                    "Configuração de role não encontrada: "
                        + role.name()
                        + ". Verifique os dados de seed."));
  }

  private OnboardingResponse montarResposta(
      EspacoTrabalhoJpaEntity workspaceJpa, UsuarioAutenticado usuarioAutenticado) {
    return new OnboardingResponse(
        workspaceJpa.getId().toString(),
        workspaceJpa.getNome(),
        workspaceJpa.getMoeda(),
        Role.owner,
        Plan.free,
        new DadosUsuarioResponse(
            usuarioAutenticado.id(), usuarioAutenticado.name(), usuarioAutenticado.email()));
  }
}
