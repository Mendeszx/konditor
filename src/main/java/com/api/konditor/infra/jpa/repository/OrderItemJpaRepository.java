package com.api.konditor.infra.jpa.repository;

import com.api.konditor.infra.jpa.entity.ItemPedidoJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link ItemPedidoJpaEntity}. */
public interface OrderItemJpaRepository extends JpaRepository<ItemPedidoJpaEntity, UUID> {

  List<ItemPedidoJpaEntity> findAllByOrderIdAndDeletedAtIsNull(UUID orderId);

  List<ItemPedidoJpaEntity> findAllByProductIdAndDeletedAtIsNull(UUID productId);

  void deleteAllByOrderId(UUID orderId);
}
