package com.api.konditor.domain.useCase.impl;

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
import com.api.konditor.domain.useCase.OnboardingUseCase;
import com.api.konditor.infra.jpa.entity.AuditLogJpaEntity;
import com.api.konditor.infra.jpa.entity.PlanDetailsJpaEntity;
import com.api.konditor.infra.jpa.entity.RoleJpaEntity;
import com.api.konditor.infra.jpa.entity.SubscriptionJpaEntity;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import com.api.konditor.infra.jpa.entity.WorkspaceJpaEntity;
import com.api.konditor.infra.jpa.entity.WorkspaceMemberJpaEntity;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementação do caso de uso de onboarding.
 *
 * <p>Fluxo executado:
 * <ol>
 *   <li>Valida que o usuário ainda não possui um workspace ativo (idempotência)</li>
 *   <li>Busca a entidade de domínio {@link User} pelo ID do token</li>
 *   <li>Constrói e persiste o {@link Workspace}</li>
 *   <li>Vincula o usuário como {@code owner} do workspace ({@link WorkspaceMember})</li>
 *   <li>Cria a {@link Subscription} gratuita para o workspace</li>
 *   <li>Registra a operação no {@code audit_log}</li>
 * </ol>
 *
 * <p>O use case manipula entidades de domínio e usa os mappers para
 * converter para entidades JPA antes de persistir.
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
    public OnboardingResponse executar(UsuarioAutenticado usuarioAutenticado, OnboardingRequest request) {
        UUID userId = UUID.fromString(usuarioAutenticado.id());
        log.info("[ONBOARDING] Iniciando onboarding para usuário id={} email={}", userId, usuarioAutenticado.email());

        // 1. Valida idempotência — usuário não pode ter workspace ativo
        validarSemWorkspaceAtivo(userId);

        // 2. Busca entidade de domínio do usuário
        User usuario = buscarUsuario(userId);
        UserJpaEntity usuarioJpa = userMapper.toJpa(usuario);

        // 3. Cria e persiste o Workspace
        Workspace workspace = criarWorkspace(usuario, request);
        WorkspaceJpaEntity workspaceJpa = persistirWorkspace(workspace, usuarioJpa);
        log.info("[ONBOARDING] Workspace criado id={} nome='{}' moeda={}",
                workspaceJpa.getId(), workspaceJpa.getName(), workspaceJpa.getCurrency());

        // 4. Vincula usuário como owner
        WorkspaceMember member = criarMembro(workspace, usuario);
        persistirMembro(member, workspaceJpa, usuarioJpa);
        log.info("[ONBOARDING] Usuário id={} vinculado como owner no workspace id={}", userId, workspaceJpa.getId());

        // 5. Cria assinatura free
        Subscription subscription = criarSubscription(workspace);
        persistirSubscription(subscription, workspaceJpa, usuarioJpa);
        log.info("[ONBOARDING] Assinatura free criada para workspace id={}", workspaceJpa.getId());

        // 6. Registra no audit log
        registrarAuditLog(workspaceJpa, usuarioJpa);

        log.info("[ONBOARDING] Onboarding concluído com sucesso para usuário id={} workspace id={}",
                userId, workspaceJpa.getId());

        return montarResposta(workspaceJpa, usuarioAutenticado);
    }

    // =========================================================================
    // Validação
    // =========================================================================

    /**
     * Garante idempotência: um usuário não pode iniciar onboarding se já possui
     * um workspace ativo (sem soft-delete).
     */
    private void validarSemWorkspaceAtivo(UUID userId) {
        boolean possuiWorkspace = !workspaceRepository.findAllByOwnerIdAndDeletedAtIsNull(userId).isEmpty();
        if (possuiWorkspace) {
            log.warn("[ONBOARDING] Onboarding rejeitado — usuário id={} já possui workspace ativo", userId);
            throw new OnboardingException("Usuário já possui um workspace ativo. Onboarding não pode ser refeito.");
        }
    }

    // =========================================================================
    // Construção das entidades de domínio
    // =========================================================================

    private User buscarUsuario(UUID userId) {
        UserJpaEntity jpa = userRepository.findById(userId)
                .orElseThrow(() -> new OnboardingException("Usuário não encontrado com id=" + userId));
        return userMapper.toDomain(jpa);
    }

    /**
     * Constrói a entidade de domínio {@link Workspace} com os dados do request.
     */
    private Workspace criarWorkspace(User owner, OnboardingRequest request) {
        return Workspace.builder()
                .name(request.getNomeWorkspace().trim())
                .owner(owner)
                .currency(request.getMoeda().toUpperCase())
                .createdBy(owner.getId())
                .build();
    }

    /**
     * Constrói a entidade de domínio {@link WorkspaceMember} — owner do workspace.
     */
    private WorkspaceMember criarMembro(Workspace workspace, User usuario) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(usuario)
                .role(Role.owner)
                .joinedAt(Instant.now())
                .createdBy(usuario)
                .build();
    }

    /**
     * Constrói a entidade de domínio {@link Subscription} para o plano gratuito.
     */
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

    private WorkspaceJpaEntity persistirWorkspace(Workspace workspace, UserJpaEntity ownerJpa) {
        PlanDetailsJpaEntity planFree = buscarPlanDetails(Plan.free);

        WorkspaceJpaEntity jpa = workspaceMapper.toJpa(workspace);
        jpa.setOwner(ownerJpa);
        jpa.setPlan(planFree);

        return workspaceRepository.save(jpa);
    }

    private void persistirMembro(WorkspaceMember member, WorkspaceJpaEntity workspaceJpa, UserJpaEntity usuarioJpa) {
        RoleJpaEntity roleOwner = buscarRole(Role.owner);

        WorkspaceMemberJpaEntity jpa = workspaceMemberMapper.toJpa(member);
        jpa.setWorkspace(workspaceJpa);
        jpa.setUser(usuarioJpa);
        jpa.setRole(roleOwner);
        jpa.setInvitedBy(null);
        jpa.setCreatedBy(usuarioJpa);

        workspaceMemberRepository.save(jpa);
    }

    private void persistirSubscription(Subscription subscription, WorkspaceJpaEntity workspaceJpa, UserJpaEntity usuarioJpa) {
        PlanDetailsJpaEntity planFree = buscarPlanDetails(Plan.free);

        SubscriptionJpaEntity jpa = SubscriptionJpaEntity.builder()
                .workspace(workspaceJpa)
                .plan(planFree)
                .status(SubscriptionStatus.active)
                .startedAt(subscription.getStartedAt())
                .createdBy(usuarioJpa)
                .build();

        subscriptionRepository.save(jpa);
    }

    // =========================================================================
    // Audit Log
    // =========================================================================

    private void registrarAuditLog(WorkspaceJpaEntity workspaceJpa, UserJpaEntity performedBy) {
        AuditLogJpaEntity log = AuditLogJpaEntity.builder()
                .workspace(workspaceJpa)
                .entityName("Workspace")
                .entityId(workspaceJpa.getId())
                .operation(AuditOperation.CREATE)
                .dataAfter("{\"event\":\"onboarding\",\"workspace\":\"" + workspaceJpa.getName() + "\"}")
                .performedBy(performedBy)
                .performedAt(Instant.now())
                .build();

        auditLogRepository.save(log);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PlanDetailsJpaEntity buscarPlanDetails(Plan plan) {
        return planDetailsRepository.findByName(plan.name())
                .orElseThrow(() -> new OnboardingException(
                        "Configuração de plano não encontrada: " + plan.name() + ". Verifique os dados de seed."));
    }

    private RoleJpaEntity buscarRole(Role role) {
        return roleRepository.findByName(role.name())
                .orElseThrow(() -> new OnboardingException(
                        "Configuração de role não encontrada: " + role.name() + ". Verifique os dados de seed."));
    }

    private OnboardingResponse montarResposta(WorkspaceJpaEntity workspaceJpa, UsuarioAutenticado usuarioAutenticado) {
        return new OnboardingResponse(
                workspaceJpa.getId().toString(),
                workspaceJpa.getName(),
                workspaceJpa.getCurrency(),
                Role.owner,
                Plan.free,
                new DadosUsuarioResponse(
                        usuarioAutenticado.id(),
                        usuarioAutenticado.name(),
                        usuarioAutenticado.email()
                )
        );
    }
}
