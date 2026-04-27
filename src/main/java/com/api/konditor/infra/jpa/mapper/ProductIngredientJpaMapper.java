package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.ProductIngredient;
import com.api.konditor.domain.entity.User;
import com.api.konditor.infra.jpa.entity.IngredienteProdutoJpaEntity;
import com.api.konditor.infra.jpa.entity.UsuarioJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct entre {@link IngredienteProdutoJpaEntity} (infra) e {@link ProductIngredient}
 * (domínio).
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {ProductJpaMapper.class, IngredientJpaMapper.class, UnitJpaMapper.class})
public interface ProductIngredientJpaMapper {

  @Mapping(target = "createdBy", expression = "java(idToUser(entity.getCreatedBy()))")
  @Mapping(target = "updatedBy", expression = "java(idToUser(entity.getUpdatedBy()))")
  ProductIngredient toDomain(IngredienteProdutoJpaEntity entity);

  @Mapping(target = "createdBy", expression = "java(userToUserJpaEntity(domain.getCreatedBy()))")
  @Mapping(target = "updatedBy", expression = "java(userToUserJpaEntity(domain.getUpdatedBy()))")
  IngredienteProdutoJpaEntity toJpa(ProductIngredient domain);

  default UsuarioJpaEntity userToUserJpaEntity(User user) {
    if (user == null) return null;
    return UsuarioJpaEntity.builder().id(user.getId()).build();
  }

  default User idToUser(UsuarioJpaEntity entity) {
    if (entity == null) return null;
    return User.builder().id(entity.getId()).build();
  }
}
