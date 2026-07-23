package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Ingredient;
import com.api.konditor.infra.jpa.entity.IngredienteJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link IngredienteJpaEntity} (infra) e {@link Ingredient} (domínio). */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WorkspaceJpaMapper.class, UnitJpaMapper.class, UserJpaMapper.class})
public interface IngredientJpaMapper {

  Ingredient toDomain(IngredienteJpaEntity entity);

  IngredienteJpaEntity toJpa(Ingredient domain);
}
