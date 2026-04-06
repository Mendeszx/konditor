package com.api.konditor.app.config.security;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.Role;

/**
 * Representa o principal autenticado via JWT no contexto de segurança.
 *
 * <p>Carrega todas as claims do token para que qualquer endpoint protegido
 * tenha acesso imediato ao contexto do tenant, sem consultas adicionais ao banco.
 *
 * @param id            ID único do usuário (claim {@code sub})
 * @param email         Endereço de e-mail (claim {@code email})
 * @param name          Nome completo (claim {@code name})
 * @param workspaceId   ID do workspace ativo (claim {@code workspaceId})
 * @param role          Papel do usuário no workspace (claim {@code workspaceRole})
 * @param plan          Plano do workspace (claim {@code plan})
 */
public record UsuarioAutenticado(
        String id,
        String email,
        String name,
        String workspaceId,
        Role role,
        Plan plan
) {}
