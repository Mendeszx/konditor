package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.OrderItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data JPA para {@link OrderItemJpaEntity}.
 */
public interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, UUID> {

    List<OrderItemJpaEntity> findAllByOrderIdAndDeletedAtIsNull(UUID orderId);

    List<OrderItemJpaEntity> findAllByProductIdAndDeletedAtIsNull(UUID productId);

    void deleteAllByOrderId(UUID orderId);
}
