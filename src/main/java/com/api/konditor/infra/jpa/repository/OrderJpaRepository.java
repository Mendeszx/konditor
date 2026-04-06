package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.OrderStatus;
import com.api.konditor.infra.jpa.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link OrderJpaEntity}.
 */
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    List<OrderJpaEntity> findAllByWorkspaceIdAndStatusAndDeletedAtIsNull(UUID workspaceId, OrderStatus status);

    Optional<OrderJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
