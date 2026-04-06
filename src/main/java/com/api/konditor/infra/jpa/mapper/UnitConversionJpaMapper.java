package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.UnitConversion;
import com.api.konditor.infra.jpa.entity.UnitConversionJpaEntity;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

/**
 * Mapper MapStruct entre {@link UnitConversionJpaEntity} (infra) e {@link UnitConversion} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = UnitJpaMapper.class)
public interface UnitConversionJpaMapper {

    @Mapping(target = "createdBy", expression = "java(userJpaEntityToUuid(entity.getCreatedBy()))")
    @Mapping(target = "updatedBy", expression = "java(userJpaEntityToUuid(entity.getUpdatedBy()))")
    UnitConversion toDomain(UnitConversionJpaEntity entity);

    @Mapping(target = "createdBy", expression = "java(uuidToUserJpaEntity(domain.getCreatedBy()))")
    @Mapping(target = "updatedBy", expression = "java(uuidToUserJpaEntity(domain.getUpdatedBy()))")
    UnitConversionJpaEntity toJpa(UnitConversion domain);

    default UUID userJpaEntityToUuid(UserJpaEntity entity) {
        if (entity == null) return null;
        return entity.getId();
    }

    default UserJpaEntity uuidToUserJpaEntity(UUID id) {
        if (id == null) return null;
        return UserJpaEntity.builder().id(id).build();
    }
}
