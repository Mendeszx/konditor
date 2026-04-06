package com.api.konditor.domain.entity;

import com.api.konditor.domain.enuns.Role;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de domínio que representa um membro de workspace.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {

    private UUID id;
    private Workspace workspace;
    private User user;
    private Role role;
    private User invitedBy;
    private Instant joinedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private User createdBy;
    private User updatedBy;
}
