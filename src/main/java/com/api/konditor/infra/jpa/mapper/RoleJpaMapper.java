package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.jpa.entity.PapelJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link PapelJpaEntity} (infra) e {@link Role} (domínio). Converte o campo
 * {@code name} da entidade para o enum {@link Role} e vice-versa.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleJpaMapper {

  default Role toDomain(PapelJpaEntity entity) {
    if (entity == null) return null;
    return Role.valueOf(entity.getNome());
  }

  default PapelJpaEntity toJpa(Role role) {
    if (role == null) return null;
    return PapelJpaEntity.builder().nome(role.name()).build();
  }
}
