package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Subscription;
import com.api.konditor.infra.jpa.entity.AssinaturaJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link AssinaturaJpaEntity} (infra) e {@link Subscription} (domínio). */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WorkspaceJpaMapper.class, UserJpaMapper.class, PlanDetailsJpaMapper.class})
public interface SubscriptionJpaMapper {

  Subscription toDomain(AssinaturaJpaEntity entity);

  AssinaturaJpaEntity toJpa(Subscription domain);
}
