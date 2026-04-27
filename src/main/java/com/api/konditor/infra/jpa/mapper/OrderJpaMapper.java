package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Order;
import com.api.konditor.infra.jpa.entity.PedidoJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link PedidoJpaEntity} (infra) e {@link Order} (domínio). */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WorkspaceJpaMapper.class, UserJpaMapper.class})
public interface OrderJpaMapper {

  Order toDomain(PedidoJpaEntity entity);

  PedidoJpaEntity toJpa(Order domain);
}
