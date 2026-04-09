package com.api.konditor.infra.jpa.mapper;

import com.api.konditor.domain.entity.OrderItem;
import com.api.konditor.domain.entity.User;
import com.api.konditor.infra.jpa.entity.OrderItemJpaEntity;
import com.api.konditor.infra.jpa.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/** Mapper MapStruct entre {@link OrderItemJpaEntity} (infra) e {@link OrderItem} (domínio). */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {OrderJpaMapper.class, ProductJpaMapper.class})
public interface OrderItemJpaMapper {

  @Mapping(target = "createdBy", expression = "java(idToUser(entity.getCreatedBy()))")
  @Mapping(target = "updatedBy", expression = "java(idToUser(entity.getUpdatedBy()))")
  OrderItem toDomain(OrderItemJpaEntity entity);

  @Mapping(target = "createdBy", expression = "java(userToUserJpaEntity(domain.getCreatedBy()))")
  @Mapping(target = "updatedBy", expression = "java(userToUserJpaEntity(domain.getUpdatedBy()))")
  OrderItemJpaEntity toJpa(OrderItem domain);

  default UserJpaEntity userToUserJpaEntity(User user) {
    if (user == null) return null;
    return UserJpaEntity.builder().id(user.getId()).build();
  }

  default User idToUser(UserJpaEntity entity) {
    if (entity == null) return null;
    return User.builder().id(entity.getId()).build();
  }
}
