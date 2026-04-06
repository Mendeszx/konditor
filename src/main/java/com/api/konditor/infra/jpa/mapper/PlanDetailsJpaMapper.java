package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.enuns.Plan;
import com.api.konditor.infra.jpa.entity.PlanDetailsJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link PlanDetailsJpaEntity} (infra) e {@link Plan} (domínio).
 * Converte o campo {@code name} da entidade para o enum {@link Plan} e vice-versa.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlanDetailsJpaMapper {

    default Plan toDomain(PlanDetailsJpaEntity entity) {
        if (entity == null) return null;
        return Plan.valueOf(entity.getName());
    }

    default PlanDetailsJpaEntity toJpa(Plan plan) {
        if (plan == null) return null;
        return PlanDetailsJpaEntity.builder().name(plan.name()).build();
    }
}
