package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.domain.enuns.SubscriptionStatus;
import com.api.konditor.infra.jpa.entity.AssinaturaJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link AssinaturaJpaEntity}. */
public interface SubscriptionJpaRepository extends JpaRepository<AssinaturaJpaEntity, UUID> {

  Optional<AssinaturaJpaEntity> findTopByWorkspaceIdAndStatusOrderByStartedAtDesc(
      UUID workspaceId, SubscriptionStatus status);

  List<AssinaturaJpaEntity> findAllByWorkspaceId(UUID workspaceId);

  Optional<AssinaturaJpaEntity> findByWorkspaceIdAndStatus(
      UUID workspaceId, SubscriptionStatus status);

  boolean existsByWorkspaceIdAndPlanAndStatus(
      UUID workspaceId, Plan plan, SubscriptionStatus status);
}
