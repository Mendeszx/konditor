package com.api.konditor.domain.entity;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.SubscriptionStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de domínio que representa uma assinatura de workspace.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    private UUID id;
    private Workspace workspace;
    private UUID planId;
    private Plan plan;
    private SubscriptionStatus status;
    private Instant startedAt;
    private Instant endsAt;
    private Instant trialEndsAt;
    private String externalSubscriptionId;
    private Instant lastPaymentAt;
    private Instant nextBillingAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private User createdBy;
    private User updatedBy;
}
