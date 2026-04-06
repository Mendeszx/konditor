package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.User;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link UserJpaEntity} (infra) e {@link User} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserJpaMapper {

    User toDomain(UserJpaEntity entity);

    UserJpaEntity toJpa(User domain);
}
