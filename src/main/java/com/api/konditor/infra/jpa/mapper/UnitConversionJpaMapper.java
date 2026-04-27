package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.UnitConversion;
import com.api.konditor.infra.jpa.entity.ConversaoUnidadeJpaEntity;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link ConversaoUnidadeJpaEntity} (infra) e {@link UnitConversion}
 * (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = UnitJpaMapper.class)
public interface UnitConversionJpaMapper {

  @Mapping(target = "createdBy", expression = "java(userJpaEntityToUuid(entity.getCreatedBy()))")
  @Mapping(target = "updatedBy", expression = "java(userJpaEntityToUuid(entity.getUpdatedBy()))")
  UnitConversion toDomain(ConversaoUnidadeJpaEntity entity);

  @Mapping(target = "createdBy", expression = "java(uuidToUserJpaEntity(domain.getCreatedBy()))")
  @Mapping(target = "updatedBy", expression = "java(uuidToUserJpaEntity(domain.getUpdatedBy()))")
  ConversaoUnidadeJpaEntity toJpa(UnitConversion domain);

  default UUID userJpaEntityToUuid(UsuarioJpaEntity entity) {
    if (entity == null) return null;
    return entity.getId();
  }

  default UsuarioJpaEntity uuidToUserJpaEntity(UUID id) {
    if (id == null) return null;
    return UsuarioJpaEntity.builder().id(id).build();
  }
}
