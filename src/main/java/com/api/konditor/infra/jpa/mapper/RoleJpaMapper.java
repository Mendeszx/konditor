package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.enuns.Role;
import com.api.konditor.infra.jpa.entity.RoleJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link RoleJpaEntity} (infra) e {@link Role} (domínio). Converte o campo
 * {@code name} da entidade para o enum {@link Role} e vice-versa.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleJpaMapper {

  default Role toDomain(RoleJpaEntity entity) {
    if (entity == null) return null;
    return Role.valueOf(entity.getName());
  }

  default RoleJpaEntity toJpa(Role role) {
    if (role == null) return null;
    return RoleJpaEntity.builder().name(role.name()).build();
  }
}
