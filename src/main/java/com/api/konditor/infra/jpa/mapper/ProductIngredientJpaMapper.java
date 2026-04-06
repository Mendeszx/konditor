package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.ProductIngredient;
import com.api.konditor.domain.entity.User;
import com.api.konditor.infra.jpa.entity.ProductIngredientJpaEntity;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;


/**
 * Mapper MapStruct entre {@link ProductIngredientJpaEntity} (infra) e {@link ProductIngredient} (domínio).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ProductJpaMapper.class, IngredientJpaMapper.class, UnitJpaMapper.class})
public interface ProductIngredientJpaMapper {

    @Mapping(target = "createdBy", expression = "java(idToUser(entity.getCreatedBy()))")
    @Mapping(target = "updatedBy", expression = "java(idToUser(entity.getUpdatedBy()))")
    ProductIngredient toDomain(ProductIngredientJpaEntity entity);

    @Mapping(target = "createdBy", expression = "java(userToUserJpaEntity(domain.getCreatedBy()))")
    @Mapping(target = "updatedBy", expression = "java(userToUserJpaEntity(domain.getUpdatedBy()))")
    ProductIngredientJpaEntity toJpa(ProductIngredient domain);

    default UserJpaEntity userToUserJpaEntity(User user) {
        if (user == null) return null;
        return UserJpaEntity.builder().id(user.getId()).build();
    }

    default User idToUser(UserJpaEntity entity) {
        if (entity == null) return null;
        return User.builder().id(entity.getId()).build();
    }
}
