package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Order;
import com.api.konditor.infra.jpa.entity.OrderJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link OrderJpaEntity} (infra) e {@link Order} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {WorkspaceJpaMapper.class, UserJpaMapper.class})
public interface OrderJpaMapper {

    Order toDomain(OrderJpaEntity entity);

    OrderJpaEntity toJpa(Order domain);
}
