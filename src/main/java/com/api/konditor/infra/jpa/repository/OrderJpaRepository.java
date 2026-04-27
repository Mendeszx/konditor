package com.api.konditor.infra.jpa.repository;

import com.api.konditor.domain.enuns.OrderStatus;
import com.api.konditor.infra.jpa.entity.PedidoJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link PedidoJpaEntity}. */
public interface OrderJpaRepository extends JpaRepository<PedidoJpaEntity, UUID> {

  List<PedidoJpaEntity> findAllByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

  List<PedidoJpaEntity> findAllByWorkspaceIdAndStatusAndDeletedAtIsNull(
      UUID workspaceId, OrderStatus status);

  Optional<PedidoJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
