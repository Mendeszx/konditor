package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.Ingredient;
import com.api.konditor.infra.jpa.entity.IngredientJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link IngredientJpaEntity} (infra) e {@link Ingredient} (domínio). */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {WorkspaceJpaMapper.class, UnitJpaMapper.class, UserJpaMapper.class})
public interface IngredientJpaMapper {

  Ingredient toDomain(IngredientJpaEntity entity);

  IngredientJpaEntity toJpa(Ingredient domain);
}
