package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Subscription;
import com.api.konditor.infra.jpa.entity.SubscriptionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link SubscriptionJpaEntity} (infra) e {@link Subscription} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {WorkspaceJpaMapper.class, UserJpaMapper.class, PlanDetailsJpaMapper.class})
public interface SubscriptionJpaMapper {

    Subscription toDomain(SubscriptionJpaEntity entity);

    SubscriptionJpaEntity toJpa(Subscription domain);
}
