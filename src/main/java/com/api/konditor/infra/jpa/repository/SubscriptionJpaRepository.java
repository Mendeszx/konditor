package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.SubscriptionStatus;
import com.api.konditor.infra.jpa.entity.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link SubscriptionJpaEntity}.
 */
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    Optional<SubscriptionJpaEntity> findTopByWorkspaceIdAndStatusOrderByStartedAtDesc(UUID workspaceId, SubscriptionStatus status);

    List<SubscriptionJpaEntity> findAllByWorkspaceId(UUID workspaceId);

    Optional<SubscriptionJpaEntity> findByWorkspaceIdAndStatus(UUID workspaceId, SubscriptionStatus status);

    boolean existsByWorkspaceIdAndPlanAndStatus(UUID workspaceId, Plan plan, SubscriptionStatus status);
}
