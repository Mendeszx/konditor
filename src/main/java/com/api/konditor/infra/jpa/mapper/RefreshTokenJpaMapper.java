package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.RefreshToken;
import com.api.konditor.infra.jpa.entity.RefreshTokenJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link RefreshTokenJpaEntity} (infra) e {@link RefreshToken} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RefreshTokenJpaMapper {

    RefreshToken toDomain(RefreshTokenJpaEntity entity);

    RefreshTokenJpaEntity toJpa(RefreshToken domain);
}
