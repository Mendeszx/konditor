package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Unit;
import com.api.konditor.infra.jpa.entity.UnitJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link UnitJpaEntity} (infra) e {@link Unit} (domínio). */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = UserJpaMapper.class)
public interface UnitJpaMapper {

  Unit toDomain(UnitJpaEntity entity);

  UnitJpaEntity toJpa(Unit domain);
}
