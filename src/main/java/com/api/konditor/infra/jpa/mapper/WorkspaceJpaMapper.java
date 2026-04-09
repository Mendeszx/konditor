package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Workspace;
import com.api.konditor.infra.jpa.entity.WorkspaceJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link WorkspaceJpaEntity} (infra) e {@link Workspace} (domínio). O campo
 * {@code owner} é mapeado recursivamente via {@link UserJpaMapper}.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {UserJpaMapper.class, PlanDetailsJpaMapper.class})
public interface WorkspaceJpaMapper {

  Workspace toDomain(WorkspaceJpaEntity entity);

  WorkspaceJpaEntity toJpa(Workspace domain);
}
