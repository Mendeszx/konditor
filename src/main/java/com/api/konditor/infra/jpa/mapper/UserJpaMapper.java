package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.User;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link UsuarioJpaEntity} (infra) e {@link User} (domínio). */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserJpaMapper {

  User toDomain(UsuarioJpaEntity entity);

  UsuarioJpaEntity toJpa(User domain);
}
