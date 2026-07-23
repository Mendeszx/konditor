package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.WorkspaceMember;
import com.api.konditor.infra.jpa.entity.MembroEspacoTrabalhoJpaEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper MapStruct entre {@link MembroEspacoTrabalhoJpaEntity} (infra) e {@link WorkspaceMember}
 * (domínio).
 *
 * <p>Os campos da entidade JPA estão em português e os do domínio em inglês — todo mapeamento é
 * explícito e {@code unmappedTargetPolicy = ERROR} garante que um campo esquecido vire erro de
 * compilação (ver KON-56).
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WorkspaceJpaMapper.class, UserJpaMapper.class, RoleJpaMapper.class},
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkspaceMemberJpaMapper {

  @Mapping(target = "workspace", source = "espacoTrabalho")
  @Mapping(target = "user", source = "usuario")
  @Mapping(target = "role", source = "papel")
  @Mapping(target = "invitedBy", source = "convidadoPor")
  @Mapping(target = "joinedAt", source = "entrouEm")
  @Mapping(target = "createdAt", source = "criadoEm")
  @Mapping(target = "updatedAt", source = "atualizadoEm")
  @Mapping(target = "deletedAt", source = "excluidoEm")
  @Mapping(target = "createdBy", source = "criadoPor")
  @Mapping(target = "updatedBy", source = "atualizadoPor")
  WorkspaceMember toDomain(MembroEspacoTrabalhoJpaEntity entity);

  @InheritInverseConfiguration
  MembroEspacoTrabalhoJpaEntity toJpa(WorkspaceMember domain);
}
