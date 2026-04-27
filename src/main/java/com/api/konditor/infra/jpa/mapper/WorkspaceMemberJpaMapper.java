package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.WorkspaceMember;
import com.api.konditor.infra.jpa.entity.MembroEspacoTrabalhoJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link MembroEspacoTrabalhoJpaEntity} (infra) e {@link WorkspaceMember}
 * (domínio).
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WorkspaceJpaMapper.class, UserJpaMapper.class, RoleJpaMapper.class})
public interface WorkspaceMemberJpaMapper {

  WorkspaceMember toDomain(MembroEspacoTrabalhoJpaEntity entity);

  MembroEspacoTrabalhoJpaEntity toJpa(WorkspaceMember domain);
}
