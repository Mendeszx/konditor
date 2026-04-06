package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.WorkspaceMember;
import com.api.konditor.infra.jpa.entity.WorkspaceMemberJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link WorkspaceMemberJpaEntity} (infra) e {@link WorkspaceMember} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {WorkspaceJpaMapper.class, UserJpaMapper.class, RoleJpaMapper.class})
public interface WorkspaceMemberJpaMapper {

    WorkspaceMember toDomain(WorkspaceMemberJpaEntity entity);

    WorkspaceMemberJpaEntity toJpa(WorkspaceMember domain);
}
