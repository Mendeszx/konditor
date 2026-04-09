package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.OrderItemJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link OrderItemJpaEntity}. */
public interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, UUID> {

  List<OrderItemJpaEntity> findAllByOrderIdAndDeletedAtIsNull(UUID orderId);

  List<OrderItemJpaEntity> findAllByProductIdAndDeletedAtIsNull(UUID productId);

  void deleteAllByOrderId(UUID orderId);
}
